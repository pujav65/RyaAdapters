/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rya.adapter.rest.reasoner.helper;

import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

/**
 * Utility methods for statements.
 */
public class StatementUtils {
    /**
     * Private constructor to prevent instantiation.
     */
    private StatementUtils() {
    }

    /**
     * Creates a {@link Statement} out of the subject, predicate, and object.
     * @param subject the subject of the triple.
     * @param predicate the predicate of the triple.
     * @param object the object of the triple.
     * @return the {@link StatementImpl}.
     */
    public static StatementImpl createStatement(final String subject, final String predicate, final String object) {
        return new StatementImpl(new URIImpl(subject), new URIImpl(predicate), new URIImpl(object));
    }

    /**
     * Creates a {@link Statement} out of the subject, predicate, object, and
     * namespace.
     * @param subject the subject of the triple.
     * @param predicate the predicate of the triple.
     * @param object the object of the triple.
     * @param namespace the namespace to use for the subject, predicate, and
     * object.
     * @return the {@link StatementImpl}.
     */
    public static StatementImpl createStatement(final String subject, final String predicate, final String object, final String namespace) {
        return createStatement(namespace + subject, namespace + predicate, namespace + object);
    }
}