/*
 * Copyright 2014-2015 the original author or authors.
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
package org.codehaus.griffon.compile.ormlite.ast.transform

import griffon.plugins.ormlite.ConnectionSourceHandler
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * @author Andres Almiray
 */
class ConnectionSourceAwareASTTransformationSpec extends Specification {
    def 'ConnectionSourceAwareASTTransformation is applied to a bean via @ConnectionSourceAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''import griffon.transform.ConnectionSourceAware
        @ConnectionSourceAware
        class Bean { }
        new Bean()
        ''')

        then:
        bean instanceof ConnectionSourceHandler
        ConnectionSourceHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }

    def 'ConnectionSourceAwareASTTransformation is not applied to a ConnectionSourceHandler subclass via @ConnectionSourceAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''
        import griffon.plugins.ormlite.ConnectionSourceCallback
        import griffon.plugins.ormlite.ConnectionSourceCallback
        import griffon.plugins.ormlite.exceptions.RuntimeSQLException
        import griffon.plugins.ormlite.ConnectionSourceHandler
        import griffon.transform.ConnectionSourceAware

        import javax.annotation.Nonnull
        @ConnectionSourceAware
        class ConnectionSourceHandlerBean implements ConnectionSourceHandler {
            @Override
            public <R> R withConnectionSource(@Nonnull ConnectionSourceCallback<R> callback) throws RuntimeSQLException {
                return null
            }
            @Override
            public <R> R withConnectionSource(@Nonnull String databaseName, @Nonnull ConnectionSourceCallback<R> callback) throws RuntimeSQLException {
                 return null
            }
            @Override
            void closeConnectionSource(){}
            @Override
            void closeConnectionSource(@Nonnull String databaseName){}
        }
        new ConnectionSourceHandlerBean()
        ''')

        then:
        bean instanceof ConnectionSourceHandler
        ConnectionSourceHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }
}
