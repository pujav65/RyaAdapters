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
// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package org.apache.rya.jena.example.pellet;

import static org.apache.rya.jena.example.pellet.util.ExampleUtils.createRyaStatement;

import java.util.List;
import java.util.Set;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/*
 * Created on Oct 10, 2004
 */

/**
 */
public class OWLAPIExample implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/discovery_short.owl";

    private static final String NS = "http://xmlns.com/foaf/0.1/";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link OWLAPIExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public OWLAPIExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception  {
        run(FILE);
    }

    public void run(final String file) throws Exception  {
        System.out.print("Reading file " + file + "...");
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology = manager.loadOntology(IRI.create(file));

        final PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner( ontology );
        System.out.println("done.");

        reasoner.getKB().realize();
        reasoner.getKB().printClassTree();

        // create property and resources to query the reasoner
        final OWLClass Person = manager.getOWLDataFactory().getOWLClass(IRI.create(NS + "Person"));
        final OWLObjectProperty workHomepage = manager.getOWLDataFactory().getOWLObjectProperty(IRI.create(NS + "workInfoHomepage"));
        final OWLDataProperty foafName = manager.getOWLDataFactory().getOWLDataProperty(IRI.create(NS + "name"));

        // get all instances of Person class
        final NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances( Person, false);
        for(final Node<OWLNamedIndividual> sameInd : individuals) {
            // sameInd contains information about the individual (and all other individuals that were inferred to be the same)
            final OWLNamedIndividual ind =  sameInd.getRepresentativeElement();
            System.out.println(ind);

            // get the info about this specific individual
            final Set<OWLLiteral> names = reasoner.getDataPropertyValues( ind, foafName );
            final NodeSet<OWLClass> types = reasoner.getTypes( ind, true );
            final NodeSet<OWLNamedIndividual> homepages = reasoner.getObjectPropertyValues( ind, workHomepage );

            // we know there is a single name for each person so we can get that value directly
            if (names.iterator().hasNext()) {
                final String name = names.iterator().next().getLiteral();
                System.out.println( "Name: " + name );
            } else {
                System.out.println( "Name: Unknown" );
            }

            // at least one direct type is guaranteed to exist for each individual
            if (types.iterator().hasNext()) {
                final OWLClass type = types.iterator().next().getRepresentativeElement();
                System.out.println( "Type: " + type );
            } else {
                System.out.println( "Type: Unknown");
            }

            // there may be zero or more homepages so check first if there are any found
            if( homepages.isEmpty() ) {
                System.out.print( "Homepage: Unknown" );
            }
            else {
                System.out.print( "Homepage:" );
                for( final Node<OWLNamedIndividual> homepage : homepages ) {
                    System.out.print( " " + homepage.getRepresentativeElement().getIRI() );
                }
            }
            System.out.println();
            System.out.println();
        }

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump ontology into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithOntology(conn, ontology);
            final RyaStatement ryaStatement = createRyaStatement(NS + "Person", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public static void main(final String[] args) throws Exception  {
        final OWLAPIExample app = new OWLAPIExample(null);
        app.run();
    }
}
