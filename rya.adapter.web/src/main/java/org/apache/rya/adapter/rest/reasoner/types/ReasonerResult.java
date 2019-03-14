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

import java.util.List;

import org.apache.rya.api.domain.RyaStatement;

/**
 * The result returned from running the reasoner.
 */
public class ReasonerResult {
    private final List<RyaStatement> ryaStatements;

    /**
     * Creates a new instance of {@link ReasonerResult}.
     * @param ryaStatements the {@link RyaStatement}s returned.
     */
    public ReasonerResult(final List<RyaStatement> ryaStatements) {
        this.ryaStatements = ryaStatements;
    }

    /**
     * @return the {@link RyaStatement}s returned.
     */
    public List<RyaStatement> getRyaStatements() {
        return ryaStatements;
    }
}