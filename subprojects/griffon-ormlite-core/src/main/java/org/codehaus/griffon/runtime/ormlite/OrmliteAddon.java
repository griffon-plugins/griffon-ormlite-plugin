/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2021 The author and/or original authors.
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

import com.j256.ormlite.support.ConnectionSource;
import griffon.annotations.core.Nonnull;
import griffon.annotations.inject.DependsOn;
import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.core.events.StartupStartEvent;
import griffon.plugins.monitor.MBeanManager;
import griffon.plugins.ormlite.ConnectionSourceCallback;
import griffon.plugins.ormlite.ConnectionSourceFactory;
import griffon.plugins.ormlite.ConnectionSourceHandler;
import griffon.plugins.ormlite.ConnectionSourceStorage;
import org.codehaus.griffon.runtime.core.addon.AbstractGriffonAddon;
import org.codehaus.griffon.runtime.ormlite.monitor.ConnectionSourceStorageMonitor;

import javax.application.event.EventHandler;
import javax.inject.Inject;
import javax.inject.Named;
import java.sql.SQLException;
import java.util.Map;

import static griffon.util.ConfigUtils.getConfigValueAsBoolean;

/**
 * @author Andres Almiray
 */
@DependsOn("datasource")
@Named("ormlite")
public class OrmliteAddon extends AbstractGriffonAddon {
    @Inject
    private ConnectionSourceHandler connectionSourceHandler;

    @Inject
    private ConnectionSourceFactory connectionSourceFactory;

    @Inject
    private ConnectionSourceStorage connectionSourceStorage;

    @Inject
    private MBeanManager mbeanManager;

    @Inject
    private Metadata metadata;

    @Override
    public void init(@Nonnull GriffonApplication application) {
        mbeanManager.registerMBean(new ConnectionSourceStorageMonitor(metadata, connectionSourceStorage));
    }

    @EventHandler
    public void handleStartupStartEvent(@Nonnull StartupStartEvent event) {
        for (String databaseName : connectionSourceFactory.getConnectionSourceNames()) {
            Map<String, Object> config = connectionSourceFactory.getConfigurationFor(databaseName);
            if (getConfigValueAsBoolean(config, "connect_on_startup", false)) {
                connectionSourceHandler.withConnectionSource(new ConnectionSourceCallback<Object>() {
                    @Override
                    public Object handle(@Nonnull String databaseName, @Nonnull ConnectionSource connectionSource) throws SQLException {
                        return null;
                    }
                });
            }
        }
    }

    @Override
    public void onShutdown(@Nonnull GriffonApplication application) {
        for (String databaseName : connectionSourceFactory.getConnectionSourceNames()) {
            connectionSourceHandler.closeConnectionSource(databaseName);
        }
    }
}
