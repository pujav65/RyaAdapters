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
package org.apache.rya.jena.example.pellet.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.domain.RyaStatement.RyaStatementBuilder;
import org.apache.rya.api.domain.RyaURI;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.api.persist.RyaDAOException;
import org.apache.rya.api.persist.query.RyaQuery;
import org.apache.rya.jena.jenasesame.JenaSesame;
import org.apache.rya.rdftriplestore.RdfCloudTripleStore;
import org.calrissian.mango.collect.CloseableIterable;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.repository.util.RDFInserter;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.rio.RioRenderer;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Utility methods to help run pellet examples.
 */
public final class ExampleUtils {
    private static final Logger log = Logger.getLogger(ExampleUtils.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private ExampleUtils() {
    }

    /**
     *
     * @param conn
     * @param model
     * @return
     */
    public static RyaDAO<?> getRyaDaoAndPopulateWithModel(final SailRepositoryConnection conn, final Model model) {
        final Dataset dataset = JenaSesame.createDataset(conn);

        final Model adapterModel = dataset.getDefaultModel();
        adapterModel.add(model);

        final RyaDAO<?> ryaDao = getRyaDao(conn);
        return ryaDao;
    }

    public static RyaDAO<?> getRyaDaoAndPopulateWithOntology(final SailRepositoryConnection conn, final OWLOntology ontology) throws RepositoryException, IOException {
        dumpOntologyToRepositoryWithoutDuplication(null, ontology, conn.getRepository().getConnection());

        final RyaDAO<?> ryaDao = getRyaDao(conn);
        return ryaDao;
    }

    /**
     *
     * @param conn
     * @return
     */
    public static RyaDAO<?> getRyaDao(final SailRepositoryConnection conn) {
        final SailRepository sailRepository = ((SailRepository)conn.getRepository());
        final RdfCloudTripleStore rdfCloudTripleStore = ((RdfCloudTripleStore)sailRepository.getSail());
        final RyaDAO<?> ryaDao = rdfCloudTripleStore.getRyaDAO();
        return ryaDao;
    }

    /**
     *
     * @param ryaDao
     * @param ryaStatement
     * @throws RyaDAOException
     * @throws IOException
     */
    public static List<RyaStatement> queryRyaStatement(final RyaDAO<?> ryaDao, final RyaStatement ryaStatement) throws RyaDAOException, IOException {
        log.info("Querying Rya...");
        final List<RyaStatement> results = new ArrayList<>();
        final CloseableIterable<RyaStatement> iter = ryaDao.getQueryEngine().query(new RyaQuery(ryaStatement));
        try {
            final Iterator<RyaStatement> iterator = iter.iterator();
            while (iterator.hasNext()) {
                final RyaStatement resultRyaStatement = iterator.next();
                log.info(resultRyaStatement);
                results.add(resultRyaStatement);
            }
        } finally {
            iter.close();
        }
        log.info("Done Querying Rya");
        return results;
    }

    public static RyaStatement createRyaStatement(final String subject, final String predicate, final String object) {
        final RyaStatementBuilder builder = new RyaStatementBuilder();
        if (subject != null) {
            builder.setSubject(new RyaURI(subject));
        }
        if (predicate != null) {
            builder.setPredicate(new RyaURI(predicate));
        }
        if (object != null) {
            builder.setObject(new RyaURI(object));
        }
        final RyaStatement ryaStatement = builder.build();
        return ryaStatement;
    }

    public static void dumpOntologyToRepositoryWithoutDuplication(final URI contextToCompareWith, final OWLOntology nextOntology, final RepositoryConnection nextRepositoryConnection, final URI... contexts) throws IOException, RepositoryException {
        IRI contextIRI = nextOntology.getOntologyID().getVersionIRI();

        if (contextIRI == null) {
            contextIRI = nextOntology.getOntologyID().getOntologyIRI();
        }

        if (contextIRI == null) {
            throw new IllegalArgumentException("Cannot dump anonymous ontologies to repository");
        }

        final URI context = contextIRI.toOpenRDFURI();

        // Create an RDFHandler that will insert all triples after they are
        // emitted from OWLAPI
        // into a specific context in the Sesame Repository
        RDFInserter repositoryHandler = new RDFInserter(nextRepositoryConnection);
        if (contextToCompareWith != null) {
            repositoryHandler = new DeduplicatingRDFInserter(contextToCompareWith, nextRepositoryConnection);
        }

        RioRenderer renderer;

        if(contexts == null || contexts.length == 0) {
            repositoryHandler.enforceContext(context);
            // Render the triples out from OWLAPI into a Sesame Repository
            renderer = new RioRenderer(nextOntology, nextOntology.getOWLOntologyManager(), repositoryHandler, null, context);
        }
        else
        {
            repositoryHandler.enforceContext(contexts);
            // Render the triples out from OWLAPI into a Sesame Repository
            renderer = new RioRenderer(nextOntology, nextOntology.getOWLOntologyManager(), repositoryHandler, null, contexts);
        }
        renderer.render();
    }

    public static void printOntologyEntities(final OWLOntology ontology) {
        for (final OWLEntity entity : ontology.getSignature()) {
            log.info(entity);
        }
    }

    public static void printModelStatements(final OntModel model) {
        final List<Statement> statements = model.listStatements().toList();
        for (final Statement statement : statements) {
            log.info(statement);
        }
    }
}