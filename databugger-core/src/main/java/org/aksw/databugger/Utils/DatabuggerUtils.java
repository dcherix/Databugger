package org.aksw.databugger.Utils;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.aksw.databugger.services.PrefixService;
import org.aksw.databugger.services.SchemaService;
import org.aksw.databugger.sources.DatasetSource;
import org.aksw.databugger.sources.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Dimitris Kontokostas
 * Description
 * Created: 9/24/13 11:25 AM
 */
public class DatabuggerUtils {
    private static Logger log = LoggerFactory.getLogger(DatabuggerUtils.class);

    public static String getAllPrefixes() {
        return " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                " PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                " PREFIX dcterms: <http://purl.org/dc/terms/> \n" +
                " PREFIX dc: <http://purl.org/dc/elements/1.1/> \n" +
                " PREFIX tddo: <http://databugger.aksw.org/ontology/core#> \n" +
                " PREFIX tddp: <http://databugger.aksw.org/data/patterns#> \n" +
                " PREFIX tddt: <http://databugger.aksw.org/ontology/tests#> \n" +
                " PREFIX tddg: <http://databugger.aksw.org/data/generators#> \n"
                ;
    }

    public static void fillSchemaServiceFromFile(String additionalCSV) {

        int count = 0;

        if (additionalCSV != null && !additionalCSV.isEmpty()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(additionalCSV), "UTF-8"));

                String line = null;


                while ((line = in.readLine()) != null) {
                    // skip comments & empty lines
                    if (line.startsWith("#") || line.trim().isEmpty())
                        continue;

                    count++;

                    String[] parts = line.split(",");
                    switch (parts.length) {
                        case 2:
                            SchemaService.addSchemaDecl(parts[0], parts[1]);
                            break;
                        case 3:
                            SchemaService.addSchemaDecl(parts[0], parts[1], parts[2]);
                            break;
                        default:
                            log.error("Invalid schema declaration in " + additionalCSV + ". Line: " + line);
                            count--;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                System.exit(1);
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                System.exit(1);
            }

            log.info("Loaded " + count + " schema declarations from: " + additionalCSV);
        }
    }

    public static void fillSchemaServiceFromLOV() {
        List<Source> sources = new ArrayList<Source>();
        Source lov = new DatasetSource("lov", "http://lov.okfn.org", "http://lov.okfn.org/endpoint/lov", "", null);

        // Example from http://lov.okfn.org/endpoint/lov
        QueryExecution qe = lov.getExecutionFactory().createQueryExecution(
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
                        "PREFIX dcterms:<http://purl.org/dc/terms/>\n" +
                        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n" +
                        "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n" +
                        "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" +
                        "PREFIX void:<http://rdfs.org/ns/void#>\n" +
                        "PREFIX bibo:<http://purl.org/ontology/bibo/>\n" +
                        "PREFIX vann:<http://purl.org/vocab/vann/>\n" +
                        "PREFIX voaf:<http://purl.org/vocommons/voaf#>\n" +
                        "PREFIX frbr:<http://purl.org/vocab/frbr/core#>\n" +
                        "PREFIX lov:<http://lov.okfn.org/dataset/lov/lov#>\n" +
                        "\n" +
                        "SELECT ?vocabPrefix ?vocabURI \n" +
                        "WHERE{\n" +
                        "\t?vocabURI a voaf:Vocabulary.\n" +
                        "\t?vocabURI vann:preferredNamespacePrefix ?vocabPrefix.\n" +
                        "} \n" +
                        "ORDER BY ?vocabPrefix ");

        int count = 0;
        ResultSet rs = qe.execSelect();
        while (rs.hasNext()) {
            QuerySolution row = rs.next();

            String prefix = row.get("vocabPrefix").toString();
            String vocab = row.get("vocabURI").toString();
            SchemaService.addSchemaDecl(prefix, vocab);
            count++;
        }

        log.info("Loaded " + count + " schema declarations from LOV SPARQL Endpoint");
    }

    public static void fillPrefixService(String filename) {

        Model prefixModel = ModelFactory.createDefaultModel();
        try {
            prefixModel.read(new FileInputStream(filename), null, "TURTLE");
        } catch (Exception e) {
            // TODO handle exception
        }

        // Update Prefix Service
        Map<String, String> prf = prefixModel.getNsPrefixMap();
        for (String id : prf.keySet()) {
            PrefixService.addPrefix(id, prf.get(id));
        }
    }

    public static boolean fileExists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static void writeModelToFile(Model model, String filetype, File file, boolean createParentFolders) throws FileNotFoundException {
        if (createParentFolders) {
            File parentF = file.getParentFile();
            if (!parentF.exists())
                file.getParentFile().mkdirs();
        }
        model.write(new FileOutputStream(file), filetype);
    }

    public static void writeModelToFile(Model model, String filetype, String filename, boolean createParentFolders) throws FileNotFoundException {
        writeModelToFile(model, filetype, new File(filename), createParentFolders);
    }
}
