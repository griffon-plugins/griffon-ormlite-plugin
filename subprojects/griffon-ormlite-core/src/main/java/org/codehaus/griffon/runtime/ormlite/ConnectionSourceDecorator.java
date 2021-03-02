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

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import griffon.annotations.core.Nonnull;

import java.io.IOException;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class ConnectionSourceDecorator implements ConnectionSource {
    private final ConnectionSource delegate;

    public ConnectionSourceDecorator(@Nonnull ConnectionSource delegate) {
        this.delegate = requireNonNull(delegate, "Argument 'delegate' must not be null");
    }

    @Nonnull
    protected ConnectionSource getDelegate() {
        return delegate;
    }

    @Override
    public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
        return getDelegate().getReadOnlyConnection(tableName);
    }

    @Override
    public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
        return getDelegate().getReadWriteConnection(tableName);
    }

    @Override
    public void releaseConnection(DatabaseConnection connection) throws SQLException {
        getDelegate().releaseConnection(connection);
    }

    @Override
    public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
        return getDelegate().saveSpecialConnection(connection);
    }

    @Override
    public void clearSpecialConnection(DatabaseConnection connection) {
        getDelegate().clearSpecialConnection(connection);
    }

    @Override
    public DatabaseConnection getSpecialConnection(String tableName) {
        return getDelegate().getSpecialConnection(tableName);
    }

    @Override
    public void closeQuietly() {
        getDelegate().closeQuietly();
    }

    @Override
    public DatabaseType getDatabaseType() {
        return getDelegate().getDatabaseType();
    }

    @Override
    public boolean isOpen(String tableName) {
        return getDelegate().isOpen(tableName);
    }

    @Override
    public boolean isSingleConnection(String tableName) {
        return getDelegate().isSingleConnection(tableName);
    }

    @Override
    public void close() throws IOException {
        getDelegate().close();
    }
}
