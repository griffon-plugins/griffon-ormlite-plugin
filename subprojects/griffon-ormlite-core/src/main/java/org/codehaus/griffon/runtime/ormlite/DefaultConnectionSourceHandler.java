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
import griffon.annotations.core.Nullable;
import griffon.plugins.ormlite.ConnectionSourceCallback;
import griffon.plugins.ormlite.ConnectionSourceFactory;
import griffon.plugins.ormlite.ConnectionSourceHandler;
import griffon.plugins.ormlite.ConnectionSourceStorage;
import griffon.plugins.ormlite.exceptions.RuntimeSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Arrays;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultConnectionSourceHandler implements ConnectionSourceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultConnectionSourceHandler.class);
    private static final String ERROR_DATASBASE_BLANK = "Argument 'databaseName' must not be blank";
    private static final String ERROR_CONNECTION_SOURCE_NULL = "Argument 'connectionSource' must not be null";
    private static final String ERROR_CALLBACK_NULL = "Argument 'callback' must not be null";

    private final ConnectionSourceFactory connectionSourceFactory;
    private final ConnectionSourceStorage connectionSourceStorage;

    @Inject
    public DefaultConnectionSourceHandler(@Nonnull ConnectionSourceFactory connectionSourceFactory, @Nonnull ConnectionSourceStorage connectionSourceStorage) {
        this.connectionSourceFactory = requireNonNull(connectionSourceFactory, "Argument 'connectionSourceFactory' must not be null");
        this.connectionSourceStorage = requireNonNull(connectionSourceStorage, "Argument 'connectionSourceStorage' must not be null");
    }

    @Nullable
    @Override
    public <R> R withConnectionSource(@Nonnull ConnectionSourceCallback<R> callback) {
        return withConnectionSource(DefaultConnectionSourceFactory.KEY_DEFAULT, callback);
    }

    @Nullable
    @Override
    public <R> R withConnectionSource(@Nonnull String databaseName, @Nonnull ConnectionSourceCallback<R> callback) throws RuntimeSQLException {
        requireNonBlank(databaseName, ERROR_DATASBASE_BLANK);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        ConnectionSource connectionSource = getConnectionSource(databaseName);
        return doWithConnection(databaseName, connectionSource, callback);
    }

    @Nullable
    @SuppressWarnings("ThrowFromFinallyBlock")
    static <R> R doWithConnection(@Nonnull String databaseName, @Nonnull ConnectionSource connectionSource, @Nonnull ConnectionSourceCallback<R> callback) throws RuntimeSQLException {
        requireNonBlank(databaseName, ERROR_DATASBASE_BLANK);
        requireNonNull(connectionSource, ERROR_CONNECTION_SOURCE_NULL);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        try {
            LOG.debug("Executing statements on database '{}'", databaseName);
            return callback.handle(databaseName, connectionSource);
        } catch (SQLException e) {
            throw new RuntimeSQLException(databaseName, e);
        }
    }

    @Override
    public void closeConnectionSource() {
        closeConnectionSource(DefaultConnectionSourceFactory.KEY_DEFAULT);
    }

    @Override
    public void closeConnectionSource(@Nonnull String databaseName) {
        ConnectionSource connectionSource = connectionSourceStorage.get(databaseName);
        if (connectionSource != null) {
            connectionSourceFactory.destroy(databaseName, connectionSource);
            connectionSourceStorage.remove(databaseName);
        }
    }

    @Nonnull
    private ConnectionSource getConnectionSource(@Nonnull String databaseName) {
        ConnectionSource connectionSource = connectionSourceStorage.get(databaseName);
        if (connectionSource == null) {
            connectionSource = connectionSourceFactory.create(databaseName);
            connectionSourceStorage.set(databaseName, connectionSource);
        }
        return connectionSource;
    }
}
