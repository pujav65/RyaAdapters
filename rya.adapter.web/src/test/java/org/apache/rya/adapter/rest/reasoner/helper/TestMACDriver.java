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

import static org.apache.rya.adapter.rest.reasoner.helper.StatementUtils.createStatement;

import org.apache.log4j.Logger;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.api.persist.RyaDAOException;
import org.apache.rya.api.resolver.RdfToRyaConversions;
import org.openrdf.model.Statement;

/**
 * Runs a MiniAccumuloCluster Rya instance for testing.
 */
public class TestMACDriver {
    private static final Logger log = Logger.getLogger(TestMACDriver.class);

    private static boolean keepRunning = true;

    private static final boolean ADD_STATEMENTS = true;

    public static void main(final String args[]) {
        log.info("Setting up MiniAccumulo Cluster");

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread thread, final Throwable throwable) {
                log.fatal("Uncaught exception in " + thread.getName(), throwable);
            }
        });

        final AccumuloInstanceDriver accumuloInstanceDriver = new AccumuloInstanceDriver("Driver", false, false, false, true, "root", "secret", "cloudbase", "triplestore_", "basic_auth");
        try {
            accumuloInstanceDriver.setUp();

            if (ADD_STATEMENTS) {
                addStatements(accumuloInstanceDriver.getDao());
            }

            log.info("MiniAccumuloCluster running with zookeeper at " + accumuloInstanceDriver.getZooKeepers());
        } catch (final Exception e) {
            log.error("Error setting up and writing statements", e);
            keepRunning = false;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Shutting down...");
                try {
                    accumuloInstanceDriver.tearDown();
                } catch (final Exception e) {
                    log.error("Error while shutting down", e);
                } finally {
                    keepRunning = false;
                    log.info("Done shutting down");
                }
            }
        });

        while(keepRunning) {
            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                log.error("Interrupted exception while running Test Driver", e);
                keepRunning = false;
            }
        }
    }

    private static void addStatements(final RyaDAO<?> dao) throws RyaDAOException {
        final String namespace = "http://rya.apache.org/jena/ns/sports#";
        final Statement stmt1 = createStatement("Bob", "plays", "Football", namespace);
        final Statement stmt2 = createStatement("Susan", "coaches", "Football", namespace);

        dao.add(RdfToRyaConversions.convertStatement(stmt1));
        dao.add(RdfToRyaConversions.convertStatement(stmt2));
        dao.flush();
    }
}