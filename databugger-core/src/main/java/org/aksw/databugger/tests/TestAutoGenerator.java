package org.aksw.databugger.tests;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.Var;
import org.aksw.databugger.Utils.DatabuggerUtils;
import org.aksw.databugger.Utils.TestUtils;
import org.aksw.databugger.enums.TestGenerationType;
import org.aksw.databugger.patterns.Pattern;
import org.aksw.databugger.patterns.PatternParameter;
import org.aksw.databugger.services.PatternService;
import org.aksw.databugger.sources.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Dimitris Kontokostas
 * Takes a sparqlPattern and a SPARQL query and based on the data
 * returned from that query we generate test cases
 * Created: 9/20/13 2:48 PM
 */
public class TestAutoGenerator {
    private static final Logger log = LoggerFactory.getLogger(TestAutoGenerator.class);

    private final String URI;
    private final String description;
    private final String query;
    private final String patternID;

    //tmp  for UML Diagram // TODO remove
    private Pattern pattern = null;

    public TestAutoGenerator(String uri, String description, String query, String patternID) {
        this.URI = uri;
        this.description = description;
        this.query = query;
        this.patternID = patternID;
    }

    /**
     * Checks if the the generator is valid (provides correct parameters)
     */
    public boolean isValid() {
        Pattern pattern = PatternService.getPattern(getPatternID());
        Query q;
        if (pattern == null) {
            log.error(getURI() + " : Pattern " + getPatternID() + " does not exist");
            return false;
        }
        try {
            q = QueryFactory.create(DatabuggerUtils.getAllPrefixes() + getQuery());
        } catch (Exception e) {
            log.error(getURI() + " Cannot parse query");
            e.printStackTrace();
            return false;
        }

        List<Var> sv = q.getProjectVars();
        if (sv.size() != pattern.getParameters().size()) {
            log.error(getURI() + " Select variables are different than Pattern parameters");
            return false;
        }


        return true;
    }

    public List<UnitTest> generate(Source source) {
        List<UnitTest> tests = new ArrayList<UnitTest>();

        Query q = QueryFactory.create(DatabuggerUtils.getAllPrefixes() + getQuery());
        QueryExecution qe = source.getExecutionFactory().createQueryExecution(q);
        ResultSet rs = qe.execSelect();
        Pattern pattern = PatternService.getPattern(getPatternID());

        while (rs.hasNext()) {
            QuerySolution row = rs.next();

            List<String> bindings = new ArrayList<String>();
            List<String> references = new ArrayList<String>();
            for (PatternParameter p : pattern.getParameters()) {
                if (row.contains(p.getId())) {
                    RDFNode n = row.get(p.getId());
                    if (n.isResource()) {
                        bindings.add("<" + n.toString().trim().replace(" ", "") + ">");
                        references.add(n.toString().trim().replace(" ", ""));
                    } else
                        bindings.add(n.toString());
                } else {
                    break;
                }


            }

            try {
                String sparql = pattern.instantiateSparqlPattern(bindings);
                String sparqlPrev = pattern.instantiateSparqlPatternPrevalence(bindings);

                tests.add(new UnitTest(
                        TestUtils.generateTestURI(source.getPrefix(), getPatternID(), sparql + sparqlPrev + URI),
                        getPatternID(),
                        TestGenerationType.AutoGenerated,
                        this.getURI(),
                        source.getSourceType(),
                        source.getUri(),
                        new TestAnnotation(),
                        sparql,
                        sparqlPrev,
                        references));
            } catch (Exception e) {

            }

        }


        return tests;
    }

    public String getURI() {
        return URI;
    }

    public String getDescription() {
        return description;
    }

    public String getQuery() {
        return query;
    }

    public String getPatternID() {
        return patternID;
    }
}
