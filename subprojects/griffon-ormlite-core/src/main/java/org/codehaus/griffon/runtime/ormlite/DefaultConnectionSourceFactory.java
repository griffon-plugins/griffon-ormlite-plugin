/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2020 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.ormlite;

import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import griffon.annotations.core.Nonnull;
import griffon.core.Configuration;
import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.core.injection.Injector;
import griffon.plugins.datasource.DataSourceFactory;
import griffon.plugins.datasource.DataSourceStorage;
import griffon.plugins.monitor.MBeanManager;
import griffon.plugins.ormlite.ConnectionSourceFactory;
import griffon.plugins.ormlite.OrmliteBootstrap;
import griffon.plugins.ormlite.events.OrmliteConnectEndEvent;
import griffon.plugins.ormlite.events.OrmliteConnectStartEvent;
import griffon.plugins.ormlite.events.OrmliteDisconnectEndEvent;
import griffon.plugins.ormlite.events.OrmliteDisconnectStartEvent;
import griffon.plugins.ormlite.exceptions.RuntimeSQLException;
import griffon.util.GriffonClassUtils;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;
import org.codehaus.griffon.runtime.ormlite.monitor.ConnectionSourceMonitor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static griffon.util.ConfigUtils.getConfigValue;
import static griffon.util.ConfigUtils.getConfigValueAsBoolean;
import static griffon.util.ConfigUtils.getConfigValueAsString;
import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultConnectionSourceFactory extends AbstractObjectFactory<ConnectionSource> implements ConnectionSourceFactory {
    private static final String ERROR_DATASOURCE_BLANK = "Argument 'databaseName' must not be blank";

    private static final String[] CUSTOM_PROPERTIES = {
        "connect_on_startup",
        "jmx"
    };

    private final Set<String> databaseNames = new LinkedHashSet<>();
    private final Configuration dataSourceConfiguration;
    @Inject
    private DataSourceFactory dataSourceFactory;
    @Inject
    private DataSourceStorage dataSourceStorage;
    @Inject
    private MBeanManager mBeanManager;
    @Inject
    private Metadata metadata;
    @Inject
    private Injector injector;

    @Inject
    public DefaultConnectionSourceFactory(@Nonnull @Named("ormlite") Configuration configuration,
                                          @Nonnull @Named("datasource") Configuration dataSourceConfiguration,
                                          @Nonnull GriffonApplication application) {
        super(configuration, application);
        this.dataSourceConfiguration = dataSourceConfiguration;
        databaseNames.add(KEY_DEFAULT);

        if (configuration.containsKey(getPluralKey())) {
            Map<String, Object> ormlites = configuration.get(getPluralKey());
            databaseNames.addAll(ormlites.keySet());
        }
    }

    @Nonnull
    @Override
    public Set<String> getConnectionSourceNames() {
        return databaseNames;
    }

    @Nonnull
    @Override
    public Map<String, Object> getConfigurationFor(@Nonnull String databaseName) {
        requireNonBlank(databaseName, ERROR_DATASOURCE_BLANK);
        return narrowConfig(databaseName);
    }

    @Nonnull
    @Override
    protected String getSingleKey() {
        return "database";
    }

    @Nonnull
    @Override
    protected String getPluralKey() {
        return "databases";
    }

    @Nonnull
    @Override
    public ConnectionSource create(@Nonnull String name) {
        Map<String, Object> config = narrowConfig(name);
        event(OrmliteConnectStartEvent.of(name, config));
        ConnectionSource connectionSource = createConnectionSource(config, name);

        if (getConfigValueAsBoolean(config, "jmx", true)) {
            connectionSource = new JMXAwareConnectionSource(connectionSource);
            registerMBeans(name, (JMXAwareConnectionSource) connectionSource);
        }

        for (Object o : injector.getInstances(OrmliteBootstrap.class)) {
            ((OrmliteBootstrap) o).init(name, connectionSource);
        }

        event(OrmliteConnectEndEvent.of(name, config, connectionSource));
        return connectionSource;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull ConnectionSource instance) {
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);
        event(OrmliteDisconnectStartEvent.of(name, config, instance));

        for (Object o : injector.getInstances(OrmliteBootstrap.class)) {
            ((OrmliteBootstrap) o).destroy(name, instance);
        }

        closeDataSource(name);

        if (getConfigValueAsBoolean(config, "jmx", true)) {
            unregisterMBeans((JMXAwareConnectionSource) instance);
        }

        event(OrmliteDisconnectEndEvent.of(name, config));
    }

    private void registerMBeans(@Nonnull String name, @Nonnull JMXAwareConnectionSource connectionSource) {
        ConnectionSourceMonitor monitor = new ConnectionSourceMonitor(metadata, (RecordingConnectionSource) connectionSource.getDelegate(), name);
        connectionSource.addObjectName(mBeanManager.registerMBean(monitor, false).getCanonicalName());
    }

    private void unregisterMBeans(@Nonnull JMXAwareConnectionSource connectionSource) {
        for (String objectName : connectionSource.getObjectNames()) {
            mBeanManager.unregisterMBean(objectName);
        }
        connectionSource.clearObjectNames();
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    private ConnectionSource createConnectionSource(@Nonnull Map<String, Object> config, @Nonnull String name) {
        DataSource dataSource = getDataSource(name);
        Map<String, Object> dsConfig = narrowDataSourceConfig(name);
        String url = getConfigValueAsString(dsConfig, "url", "");
        requireNonBlank(url, "Configuration for " + name + ".url must not be blank");

        try {
            DataSourceConnectionSource connectionSource = new DataSourceConnectionSource();
            connectionSource.setDataSource(dataSource);
            connectionSource.setDatabaseUrl(url);

            for (Map.Entry<String, Object> e : config.entrySet()) {
                if (Arrays.binarySearch(CUSTOM_PROPERTIES, e.getKey()) != -1) {
                    continue;
                }
                GriffonClassUtils.setPropertyValue(connectionSource, e.getKey(), e.getValue());
            }

            connectionSource.initialize();
            return new RecordingConnectionSource(connectionSource);
        } catch (SQLException e) {
            throw new RuntimeSQLException(name, e);
        }
    }

    private void closeDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource != null) {
            dataSourceFactory.destroy(dataSourceName, dataSource);
            dataSourceStorage.remove(dataSourceName);
        }
    }

    @Nonnull
    private DataSource getDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource == null) {
            dataSource = dataSourceFactory.create(dataSourceName);
            dataSourceStorage.set(dataSourceName, dataSource);
        }
        return dataSource;
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private Map<String, Object> narrowDataSourceConfig(@Nonnull String name) {
        requireNonBlank(name, "Argument 'name' must not be blank");

        String singleKey = "dataSource";
        String pluralKey = "dataSources";

        if (KEY_DEFAULT.equals(name) && dataSourceConfiguration.containsKey(singleKey)) {
            return (Map<String, Object>) dataSourceConfiguration.get(singleKey);
        } else {
            if (dataSourceConfiguration.containsKey(pluralKey)) {
                Map<String, Object> elements = dataSourceConfiguration.get(pluralKey);
                return getConfigValue(elements, name, Collections.<String, Object>emptyMap());
            }
        }
        return Collections.emptyMap();
    }
}
