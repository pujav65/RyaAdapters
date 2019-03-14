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

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerRunner;
import org.apache.rya.adapter.rest.reasoner.types.jena.JenaReasonerRunner;
import org.apache.rya.adapter.rest.reasoner.types.pellet.PelletReasonerRunner;

/**
 * A list of supported reasoners.
 */
public enum ReasonerType {
    /**
     * Use Jena for reasoning.
     */
    JENA(new JenaReasonerRunner()),
    /**
     * Use Pellet for reasoning.
     */
    PELLET(new PelletReasonerRunner());

    private ReasonerRunner reasonerRunner;

    /**
     * Creates a new {@link ReasonerType}.
     * @param reasonerRunner the {@link ReasonerRunner} associated with the
     * type. (not {@code null})
     */
    private ReasonerType(final ReasonerRunner reasonerRunner) {
        this.reasonerRunner = checkNotNull(reasonerRunner);
    }

    /**
     * @return the {@link ReasonerRunner} associated with the type.
     */
    public ReasonerRunner getReasonerRunner() {
        return reasonerRunner;
    }

    /**
     * Finds the reasoner type by name.
     * @param name the name to find.
     * @return the {@link ReasonerType} or {@code null} if none could be found.
     */
    public static ReasonerType fromName(final String name) {
        if (StringUtils.isNotBlank(name)) {
            for (final ReasonerType reasonerType : ReasonerType.values()) {
                if (reasonerType.toString().equals(name)) {
                    return reasonerType;
                }
            }
        }
        return null;
    }
}