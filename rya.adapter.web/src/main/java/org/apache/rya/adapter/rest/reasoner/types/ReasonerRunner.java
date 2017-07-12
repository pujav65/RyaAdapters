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
package org.apache.rya.adapter.rest.reasoner.types;

import org.openrdf.repository.sail.SailRepositoryConnection;

/**
 * Interface for running a reasoner.
 */
public interface ReasonerRunner {
    /**
     * Runs the reasoner using the file specified.
     * @param conn the {@link SailRepositoryConnection}.
     * @param filename the filename path.
     * @return the {@link ReasonerResult}.
     * @throws Exception
     */
    public ReasonerResult runReasoner(final SailRepositoryConnection conn, final String filename) throws Exception;
}