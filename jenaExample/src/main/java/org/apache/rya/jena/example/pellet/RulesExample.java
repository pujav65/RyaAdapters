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
// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package org.apache.rya.jena.example.pellet;

import static org.apache.rya.jena.example.pellet.util.ExampleUtils.createRyaStatement;

import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.repository.sail.SailRepositoryConnection;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * An example program that tests the DL-safe rules example from Table 3 in the
 * paper: B. Motik, U. Sattler, R. Studer. Query Answering for OWL-DL with
 * Rules. Proc. of the 3rd International Semantic Web Conference (ISWC 2004),
 * Hiroshima, Japan, November, 2004, pp. 549-563
 */
public class RulesExample implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/dl-safe.owl";
    private static final String NS = "http://owldl.com/ontologies/dl-safe.owl";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link RulesExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public RulesExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC, null );
        model.read( file );

        final ObjectProperty sibling = model.getObjectProperty( NS + "#sibling" );

        final OntClass BadChild = model.getOntClass( NS + "#BadChild" );
        final OntClass Child = model.getOntClass( NS + "#Child" );

        final Individual Abel = model.getIndividual( NS + "#Abel" );
        final Individual Cain = model.getIndividual( NS + "#Cain" );
        final Individual Remus = model.getIndividual( NS + "#Remus" );
        final Individual Romulus = model.getIndividual( NS + "#Romulus" );

        model.prepare();

        // Cain has sibling Abel due to SiblingRule
        printPropertyValues( Cain, sibling );
        // Abel has sibling Cain due to SiblingRule and rule works symmetric
        printPropertyValues( Abel, sibling );
        // Remus is not inferred to have a sibling because his father is not
        // known
        printPropertyValues( Remus, sibling );
        // No siblings for Romulus for same reasons
        printPropertyValues( Romulus, sibling );

        // Cain is a BadChild due to BadChildRule
        printInstances( BadChild );
        // Cain is a Child due to BadChildRule and ChildRule2
        // Oedipus is a Child due to ChildRule1 and ChildRule2 combined with the
        // unionOf type
        printInstances( Child );

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(NS + "#Cain", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public static void printPropertyValues(final Individual ind, final Property prop) {
        System.out.print( ind.getLocalName() + " has " + prop.getLocalName() + "(s): " );
        printIterator( ind.listPropertyValues( prop ) );
    }

    public static void printInstances(final OntClass cls) {
        System.out.print( cls.getLocalName() + " instances: " );
        printIterator( cls.listInstances() );
    }

    public static void printIterator(final ExtendedIterator<?> i) {
        if( !i.hasNext() ) {
            System.out.print( "none" );
        }
        else {
            while( i.hasNext() ) {
                final Resource val = (Resource) i.next();
                System.out.print( val.getLocalName() );
                if( i.hasNext() ) {
                    System.out.print( ", " );
                }
            }
        }
        System.out.println();
    }

    public static void main(final String[] args) throws Exception {
        final RulesExample app = new RulesExample(null);
        app.run();
    }
}
