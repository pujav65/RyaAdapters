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
package org.apache.rya.adapter.rest.reasoner.types.jena;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerResult;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerRunner;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.domain.RyaStatement.RyaStatementBuilder;
import org.apache.rya.api.domain.RyaURI;
import org.apache.rya.jena.jenasesame.JenaSesame;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.Rule.Parser;

/**
 * Handles running the Jena reasoner.
 */
public class JenaReasonerRunner implements ReasonerRunner {
    private static final Logger log = Logger.getLogger(JenaReasonerRunner.class);

    @Override
    public ReasonerResult runReasoner(final SailRepository repo, final String filename) throws Exception {
        final List<RyaStatement> ryaStatements = new ArrayList<>();
        RepositoryConnection conn = null;
        try {
            conn = repo.getConnection();

            final Dataset dataset = JenaSesame.createDataset(conn);

            final Model model = dataset.getDefaultModel();
            log.info(model.getNsPrefixMap());

            final File file = new File(filename);
            Reasoner reasoner = null;
            try (
                final FileInputStream fin = new FileInputStream(file);
                final BufferedReader br = new BufferedReader(new InputStreamReader(fin, StandardCharsets.UTF_8));
            ) {
                final Parser parser = Rule.rulesParserFromReader(br);
                reasoner = new GenericRuleReasoner(Rule.parseRules(parser));
            }

            final InfModel infModel = ModelFactory.createInfModel(reasoner, model);

            final StmtIterator iterator = infModel.listStatements();

            int count = 0;
            while (iterator.hasNext()) {
                final com.hp.hpl.jena.rdf.model.Statement stmt = iterator.nextStatement();

                final Resource subject = stmt.getSubject();
                final Property predicate = stmt.getPredicate();
                final RDFNode object = stmt.getObject();

                final RyaStatement ryaStatement = convertJenaStatementToRyaStatement(stmt);

                log.info(ryaStatement);
                log.info(subject.toString() + " " + predicate.toString() + " " + object.toString());
                model.add(stmt);
                ryaStatements.add(ryaStatement);
                count++;
            }
            log.info("Result count : " + count);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        final ReasonerResult reasonerResult = new ReasonerResult(ryaStatements);
        return reasonerResult;
    }

    private static RyaStatement convertJenaStatementToRyaStatement(final com.hp.hpl.jena.rdf.model.Statement statement) {
        final Resource subject = statement.getSubject();
        final Property predicate = statement.getPredicate();
        final RDFNode object = statement.getObject();

        final RyaStatementBuilder builder = new RyaStatementBuilder();
        if (subject != null) {
            builder.setSubject(new RyaURI(subject.getURI()));
        }
        if (predicate != null) {
            builder.setPredicate(new RyaURI(predicate.getURI()));
        }
        if (object != null) {
            builder.setObject(new RyaURI(object.asNode().getURI()));
        }
        final RyaStatement ryaStatement = builder.build();

        return ryaStatement;
    }
}