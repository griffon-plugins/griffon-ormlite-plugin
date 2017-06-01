/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.ormlite;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import griffon.core.Configuration;
import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.core.injection.Injector;
import griffon.exceptions.GriffonException;
import griffon.plugins.monitor.MBeanManager;
import griffon.plugins.ormlite.ConnectionSourceFactory;
import griffon.plugins.ormlite.OrmliteBootstrap;
import griffon.plugins.ormlite.exceptions.RuntimeSQLException;
import griffon.util.GriffonClassUtils;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;
import org.codehaus.griffon.runtime.jmx.ConnectionSourceMonitor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static griffon.util.ConfigUtils.getConfigValueAsBoolean;
import static griffon.util.ConfigUtils.getConfigValueAsString;
import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultConnectionSourceFactory extends AbstractObjectFactory<ConnectionSource> implements ConnectionSourceFactory {
    private static final String ERROR_DATASOURCE_BLANK = "Argument 'databaseName' must not be blank";

    private static final String[] CUSTOM_PROPERTIES = {
        "connect_on_startup",
        "jmx",
        "password",
        "url",
        "username"
    };

    private final Set<String> databaseNames = new LinkedHashSet<>();

    @Inject
    private MBeanManager mBeanManager;

    @Inject
    private Metadata metadata;

    @Inject
    private Injector injector;

    @Inject
    public DefaultConnectionSourceFactory(@Nonnull @Named("ormlite") Configuration configuration, @Nonnull GriffonApplication application) {
        super(configuration, application);
        databaseNames.add(KEY_DEFAULT);

        if (configuration.containsKey(getPluralKey())) {
            Map<String, Object> ormlites = (Map<String, Object>) configuration.get(getPluralKey());
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
        requireNonBlank(name, ERROR_DATASOURCE_BLANK);
        Map<String, Object> config = narrowConfig(name);

        event("OrmliteConnectStart", asList(name, config));

        ConnectionSource connectionSource = createConnectionSource(config, name);

        if (getConfigValueAsBoolean(config, "jmx", true)) {
            connectionSource = new JMXAwareConnectionSource(connectionSource);
            registerMBeans(name, (JMXAwareConnectionSource) connectionSource);
        }

        for (Object o : injector.getInstances(OrmliteBootstrap.class)) {
            ((OrmliteBootstrap) o).init(name, connectionSource);
        }

        event("OrmliteConnectEnd", asList(name, config, connectionSource));

        return connectionSource;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull ConnectionSource instance) {
        requireNonBlank(name, ERROR_DATASOURCE_BLANK);
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);

        event("OrmliteDisconnectStart", asList(name, config, instance));

        if (getConfigValueAsBoolean(config, "jmx", true)) {
            ((JMXAwareConnectionSource) instance).disposeMBeans();
        }

        for (Object o : injector.getInstances(OrmliteBootstrap.class)) {
            ((OrmliteBootstrap) o).destroy(name, instance);
        }

        if (instance.isOpen(null)) {
            try {
                instance.close();
            } catch (IOException e) {
                throw new GriffonException(name, e);
            }
        }

        event("OrmliteDisconnectEnd", asList(name, config));
    }

    private void registerMBeans(@Nonnull String name, @Nonnull JMXAwareConnectionSource connectionSource) {
        ConnectionSourceMonitor monitor = new ConnectionSourceMonitor(metadata, (RecordingConnectionSource) connectionSource.getDelegate(), name);
        connectionSource.addObjectName(mBeanManager.registerMBean(monitor, false).getCanonicalName());
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    private ConnectionSource createConnectionSource(@Nonnull Map<String, Object> config, @Nonnull String name) {
        String url = getConfigValueAsString(config, "url", "");
        requireNonBlank(url, "Configuration for " + name + ".url must not be blank");
        String username = getConfigValueAsString(config, "username", "");
        String password = getConfigValueAsString(config, "password", "");

        try {
            JdbcPooledConnectionSource connectionSource = new JdbcPooledConnectionSource(url, username, password);

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
}
