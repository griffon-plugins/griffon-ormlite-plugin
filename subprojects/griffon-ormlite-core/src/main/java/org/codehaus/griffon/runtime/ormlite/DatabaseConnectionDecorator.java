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

import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.GenericRowMapper;
import com.j256.ormlite.stmt.StatementBuilder;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.GeneratedKeyHolder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Savepoint;

import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DatabaseConnectionDecorator implements DatabaseConnection {
    private final DatabaseConnection delegate;

    public DatabaseConnectionDecorator(@Nonnull DatabaseConnection delegate) {
        this.delegate = requireNonNull(delegate, "Argument 'delegate' must not be null");
    }

    @Nonnull
    protected DatabaseConnection getDelegate() {
        return delegate;
    }

    @Override
    public boolean isAutoCommitSupported() throws SQLException {
        return getDelegate().isAutoCommitSupported();
    }

    @Override
    public boolean isAutoCommit() throws SQLException {
        return getDelegate().isAutoCommit();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        getDelegate().setAutoCommit(autoCommit);
    }

    @Override
    public Savepoint setSavePoint(String savePointName) throws SQLException {
        return getDelegate().setSavePoint(savePointName);
    }

    @Override
    public void commit(Savepoint savePoint) throws SQLException {
        getDelegate().commit(savePoint);
    }

    @Override
    public void rollback(Savepoint savePoint) throws SQLException {
        getDelegate().rollback(savePoint);
    }

    @Override
    public int executeStatement(String statementStr, int resultFlags) throws SQLException {
        return getDelegate().executeStatement(statementStr, resultFlags);
    }

    @Override
    public CompiledStatement compileStatement(String statement, StatementBuilder.StatementType type, FieldType[] argFieldTypes, int resultFlags, boolean cacheStore) throws SQLException {
        return getDelegate().compileStatement(statement, type, argFieldTypes, resultFlags, cacheStore);
    }

    @Override
    public int insert(String statement, Object[] args, FieldType[] argfieldTypes, GeneratedKeyHolder keyHolder) throws SQLException {
        return getDelegate().insert(statement, args, argfieldTypes, keyHolder);
    }

    @Override
    public int update(String statement, Object[] args, FieldType[] argfieldTypes) throws SQLException {
        return getDelegate().update(statement, args, argfieldTypes);
    }

    @Override
    public int delete(String statement, Object[] args, FieldType[] argfieldTypes) throws SQLException {
        return getDelegate().delete(statement, args, argfieldTypes);
    }

    @Override
    public <T> Object queryForOne(String statement, Object[] args, FieldType[] argfieldTypes, GenericRowMapper<T> rowMapper, ObjectCache objectCache) throws SQLException {
        return getDelegate().queryForOne(statement, args, argfieldTypes, rowMapper, objectCache);
    }

    @Override
    public long queryForLong(String statement) throws SQLException {
        return getDelegate().queryForLong(statement);
    }

    @Override
    public long queryForLong(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
        return getDelegate().queryForLong(statement, args, argFieldTypes);
    }

    @Override
    public void closeQuietly() {
        getDelegate().closeQuietly();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return getDelegate().isClosed();
    }

    @Override
    public boolean isTableExists(String tableName) throws SQLException {
        return getDelegate().isTableExists(tableName);
    }

    @Override
    public void close() throws IOException {
        getDelegate().close();
    }
}
