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
package griffon.plugins.ormlite

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import griffon.annotations.inject.BindTo
import griffon.core.GriffonApplication
import griffon.plugins.datasource.events.DataSourceConnectEndEvent
import griffon.plugins.datasource.events.DataSourceConnectStartEvent
import griffon.plugins.datasource.events.DataSourceDisconnectEndEvent
import griffon.plugins.datasource.events.DataSourceDisconnectStartEvent
import griffon.plugins.ormlite.events.OrmliteConnectEndEvent
import griffon.plugins.ormlite.events.OrmliteConnectStartEvent
import griffon.plugins.ormlite.events.OrmliteDisconnectEndEvent
import griffon.plugins.ormlite.events.OrmliteDisconnectStartEvent
import griffon.plugins.ormlite.exceptions.RuntimeSQLException
import griffon.test.core.GriffonUnitRule
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.application.event.EventHandler
import javax.inject.Inject

@Unroll
class OrmliteSpec extends Specification {
    static {
        System.setProperty('org.slf4j.simpleLogger.defaultLogLevel', 'trace')
    }

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Inject
    private ConnectionSourceHandler connectionSourceHandler

    @Inject
    private GriffonApplication application

    void 'Open and close default connectionSource'() {
        given:
        List eventNames = [
            'OrmliteConnectStartEvent', 'DataSourceConnectStartEvent',
            'DataSourceConnectEndEvent', 'OrmliteConnectEndEvent',
            'OrmliteDisconnectStartEvent', 'DataSourceDisconnectStartEvent',
            'DataSourceDisconnectEndEvent', 'OrmliteDisconnectEndEvent'
        ]
        TestEventHandler testEventHandler = new TestEventHandler()
        application.eventRouter.subscribe(testEventHandler)

        when:
        connectionSourceHandler.withConnectionSource { String databaseName, ConnectionSource connectionSource ->
            true
        }
        connectionSourceHandler.closeConnectionSource()
        // second call should be a NOOP
        connectionSourceHandler.closeConnectionSource()

        then:
        testEventHandler.events.size() == 8
        testEventHandler.events == eventNames
    }

    void 'Connect to default connectionSource'() {
        expect:
        connectionSourceHandler.withConnectionSource { String databaseName, ConnectionSource connectionSource ->
            databaseName == 'default' && connectionSource
        }
    }

    void 'Bootstrap init is called'() {
        given:
        assert !bootstrap.initWitness

        when:
        connectionSourceHandler.withConnectionSource { String sessionFactoryName, ConnectionSource connectionSource -> }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        connectionSourceHandler.withConnectionSource { String sessionFactoryName, ConnectionSource connectionSource -> }
        connectionSourceHandler.closeConnectionSource()

        then:
        bootstrap.initWitness
        bootstrap.destroyWitness
    }

    void 'Can connect to #name connectionSource'() {
        expect:
        connectionSourceHandler.withConnectionSource(name) { String databaseName, ConnectionSource connectionSource ->
            databaseName == name && connectionSource
        }

        where:
        name       | _
        'default'  | _
        'internal' | _
        'people'   | _
    }

    void 'Bogus connectionSource name (#name) results in error'() {
        when:
        connectionSourceHandler.withConnectionSource(name) { String databaseName, ConnectionSource connectionSource ->
            true
        }

        then:
        thrown(IllegalArgumentException)

        where:
        name    | _
        null    | _
        ''      | _
        'bogus' | _
    }

    void 'Execute statements on people connectionSource'() {
        when:
        List peopleIn = connectionSourceHandler.withConnectionSource('people') { String databaseName, ConnectionSource connectionSource ->
            TableUtils.createTableIfNotExists(connectionSource, Person)
            Dao<Person, Integer> peopleDao = DaoManager.createDao(connectionSource, Person)
            [[id: 1, name: 'Danno', lastname: 'Ferrin'],
             [id: 2, name: 'Andres', lastname: 'Almiray'],
             [id: 3, name: 'James', lastname: 'Williams'],
             [id: 4, name: 'Guillaume', lastname: 'Laforge'],
             [id: 5, name: 'Jim', lastname: 'Shingler'],
             [id: 6, name: 'Alexander', lastname: 'Klein'],
             [id: 7, name: 'Rene', lastname: 'Groeschke']].each { data ->
                Person person = new Person(data)
                peopleDao.create(person)
            }
        }

        List peopleOut = connectionSourceHandler.withConnectionSource('people') { String databaseName, ConnectionSource connectionSource ->
            Dao<Person, Integer> peopleDao = DaoManager.createDao(connectionSource, Person)
            peopleDao.queryForAll().collect([]) { it.asMap() }
        }

        then:
        peopleIn == peopleOut
    }

    void 'A runtime SQLException is thrown within connectionSource handling'() {
        when:
        connectionSourceHandler.withConnectionSource('people') { String databaseName, ConnectionSource connectionSource ->
            Dao<Person, Integer> peopleDao = DaoManager.createDao(connectionSource, Person)
            peopleDao.create(new Person())
        }

        then:
        thrown(RuntimeSQLException)
    }

    @BindTo(OrmliteBootstrap)
    private TestOrmliteBootstrap bootstrap = new TestOrmliteBootstrap()


    private class TestEventHandler {
        List<String> events = []

        @EventHandler
        void handleDataSourceConnectStartEvent(DataSourceConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceConnectEndEvent(DataSourceConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceDisconnectStartEvent(DataSourceDisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceDisconnectEndEvent(DataSourceDisconnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleOrmliteConnectStartEvent(OrmliteConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleOrmliteConnectEndEvent(OrmliteConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleOrmliteDisconnectStartEvent(OrmliteDisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleOrmliteDisconnectEndEvent(OrmliteDisconnectEndEvent event) {
            events << event.class.simpleName
        }
    }
}
