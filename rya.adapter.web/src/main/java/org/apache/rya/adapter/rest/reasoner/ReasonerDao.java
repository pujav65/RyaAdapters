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
package org.apache.rya.adapter.rest.reasoner;

import org.apache.rya.adapter.rest.reasoner.types.ReasonerResult;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerRunner;
import org.openrdf.repository.sail.SailRepositoryConnection;

/**
 * The reasoner DAO.
 */
public class ReasonerDao {
    /**
     * Starts the reasoner using the rule file specified and the reasoner
     * specified.
     * @param conn the {@link SailRepositoryConnection}.
     * @param filename the name of the rule file.
     * @param reasonerType the {@link ReasonerType}.
     * @return the {@link ReasonerResult}.
     * @throws Exception
     */
    public ReasonerResult startReasoner(final SailRepositoryConnection conn, final String filename, final ReasonerType reasonerType) throws Exception {
        switch (reasonerType) {
            case JENA:
            case PELLET:
                final ReasonerRunner reasonerRunner = reasonerType.getReasonerRunner();
                final ReasonerResult reasonerResult = reasonerRunner.runReasoner(conn, filename);
                return reasonerResult;
            default:
                throw new Exception("Unknown reasoner type specified: " + reasonerType.toString());
        }
    }
}