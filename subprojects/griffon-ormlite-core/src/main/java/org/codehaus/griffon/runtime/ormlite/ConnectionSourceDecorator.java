/*
 * Copyright 2014-2016 the original author or authors.
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

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import javax.annotation.Nonnull;
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
    public DatabaseConnection getReadOnlyConnection() throws SQLException {
        return delegate.getReadOnlyConnection();
    }

    @Override
    public DatabaseConnection getReadWriteConnection() throws SQLException {
        return delegate.getReadWriteConnection();
    }

    @Override
    public void releaseConnection(DatabaseConnection connection) throws SQLException {
        delegate.releaseConnection(connection);
    }

    @Override
    public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
        return delegate.saveSpecialConnection(connection);
    }

    @Override
    public void clearSpecialConnection(DatabaseConnection connection) {
        delegate.clearSpecialConnection(connection);
    }

    @Override
    public DatabaseConnection getSpecialConnection() {
        return delegate.getSpecialConnection();
    }

    @Override
    public void close() throws SQLException {
        delegate.close();
    }

    @Override
    public void closeQuietly() {
        delegate.closeQuietly();
    }

    @Override
    public DatabaseType getDatabaseType() {
        return delegate.getDatabaseType();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }
}
