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
package org.apache.rya.adapter.rest.reasoner.types.pellet;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerResult;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerRunner;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.domain.RyaStatement.RyaStatementBuilder;
import org.apache.rya.api.domain.RyaType;
import org.apache.rya.api.domain.RyaURI;
import org.apache.rya.api.resolver.RyaToRdfConversions;
import org.apache.rya.jena.jenasesame.JenaSesame;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;

/**
 * Handles running the Pellet reasoner.
 */
public class PelletReasonerRunner implements ReasonerRunner {
    private static final Logger log = Logger.getLogger(PelletReasonerRunner.class);

    @Override
    public ReasonerResult runReasoner(final SailRepository repo, final String filename) throws Exception {
        final List<RyaStatement> ryaStatements = new ArrayList<>();
        RepositoryConnection conn = null;
        try {
            conn = repo.getConnection();

            final Dataset dataset = JenaSesame.createDataset(conn);

            final Model adapterModel = dataset.getDefaultModel();

            // create an empty ontology model using Pellet spec
            final OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, null);
            adapterModel.add(model);

            final File file = new File(filename);
            // read the file
            model.read("file:///" + file.getAbsolutePath());
            model.prepare();

            final Reasoner reasoner = PelletReasonerFactory.theInstance().create();

            final InfModel infModel = ModelFactory.createInfModel(reasoner, model);

            final StmtIterator iterator = infModel.listStatements();

            int count = 0;
            while (iterator.hasNext()) {
                final com.hp.hpl.jena.rdf.model.Statement stmt = iterator.nextStatement();

                final Resource subject = stmt.getSubject();
                final Property predicate = stmt.getPredicate();
                final RDFNode object = stmt.getObject();

                final RyaStatement ryaStatement = convertJenaStatementToRyaStatement(stmt);

                log.info(subject.toString() + " " + predicate.toString() + " " + object.toString());
                model.add(stmt);

                // TODO: figure out why the infModel doesn't make its way back to the Sail repo connection automatically
                conn.add(RyaToRdfConversions.convertStatement(ryaStatement));
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
            String uri = null;
            if (subject.isURIResource()) {
                uri = subject.asResource().getURI();
            } else if (subject.isAnon()) {
                uri = subject.asNode().getBlankNodeId().toString();
            } else if (object.isResource()) {
                uri = subject.asResource().getId().toString();
            }
            builder.setSubject(new RyaURI(uri));
        }
        if (predicate != null && predicate.getURI() != null) {
            builder.setPredicate(new RyaURI(predicate.getURI()));
        }
        if (object != null) {
            RyaType ryaType = null;
            if (object.isLiteral()) {
                final Literal literal = object.asLiteral();
                final Class<?> clazz = literal.getValue().getClass();
                final URI uriType = determineClassTypeUri(clazz);
                ryaType = new RyaType(uriType, literal.toString());
            } else if (object.isURIResource()) {
                final String uri = object.asResource().getURI();
                ryaType = new RyaType(XMLSchema.ANYURI, uri);
            } else if (object.isAnon()) {
                final String data = object.asNode().getBlankNodeId().toString();
                ryaType = new RyaType(XMLSchema.STRING, data);
            } else if (object.isResource()) {
                final String data = object.asResource().getId().toString();
                ryaType = new RyaType(XMLSchema.ANYURI, data);
            }
            builder.setObject(ryaType);
        }
        final RyaStatement ryaStatement = builder.build();

        return ryaStatement;
    }

    private static URI determineClassTypeUri(final Class<?> classType) {
        if (classType.equals(Integer.class)) {
            return XMLSchema.INTEGER;
        } else if (classType.equals(Double.class)) {
            return XMLSchema.DOUBLE;
        } else if (classType.equals(Float.class)) {
            return XMLSchema.FLOAT;
        } else if (classType.equals(Short.class)) {
            return XMLSchema.SHORT;
        } else if (classType.equals(Long.class)) {
            return XMLSchema.LONG;
        } if (classType.equals(Boolean.class)) {
            return XMLSchema.BOOLEAN;
        } else if (classType.equals(Byte.class)) {
            return XMLSchema.BYTE;
        } else if (classType.equals(Date.class)) {
            return XMLSchema.DATETIME;
        } else if (classType.equals(URI.class)) {
            return XMLSchema.ANYURI;
        }

        return XMLSchema.STRING;
    }
}