package org.aksw.databugger.tests;

import org.aksw.databugger.Utils.CacheUtils;
import org.aksw.databugger.Utils.TestUtils;
import org.aksw.databugger.enums.TestGenerationType;
import org.aksw.databugger.exceptions.TripleReaderException;
import org.aksw.databugger.sources.SchemaSource;
import org.aksw.databugger.sources.Source;
import org.aksw.databugger.io.TripleReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Dimitris Kontokostas
 * handles test generation form a schema or a cache
 * Created: 11/20/13 7:31 PM
 */
public class TestGeneratorExecutor {
    private static Logger log = LoggerFactory.getLogger(TestGeneratorExecutor.class);
    private boolean isCanceled = false;

    public interface TestGeneratorExecutorMonitor{
        /*
        * Called when testing starts
        * */
        void generationStarted(final Source source, final long numberOfSources);

        /*
        * Called when a test generation starts
        * */
        void sourceGenerationStarted(final Source source, final TestGenerationType generationType);

        /*
        * Called when a test generation starts
        * */
        void sourceGenerationExecuted(final Source source, final TestGenerationType generationType, final long testsCreated);

        /*
        * Called when test generation ends
        * */
        void generationFinished();
    }

    private final List<TestGeneratorExecutorMonitor> progressMonitors = new ArrayList<TestGeneratorExecutorMonitor>();


    public void cancel(){
        isCanceled = true;
    }


    public List<UnitTest> generateTests(String testFolder, Source dataset, List<TestAutoGenerator> autoGenerators) {

        List<SchemaSource> sources = dataset.getReferencesSchemata();


        /*notify start of testing */
        for (TestGeneratorExecutorMonitor monitor : progressMonitors){
            monitor.generationStarted(dataset, sources.size());
        }

        List<UnitTest> allTests = new ArrayList<UnitTest>();
        for (SchemaSource s : sources) {
            if (isCanceled == true) {
                break;
            }

            log.info("Generating tests for: " + s.getUri());

            //Generate auto tests from schema
            allTests.addAll(generateAutoTestsForSchemaSource(testFolder, s,autoGenerators));
            //Find manual tests for schema
            allTests.addAll(generateManualTestsForSource(testFolder, s,autoGenerators));
        }

        //Find manual tests for dataset (if not canceled
        if (isCanceled == false)
            allTests.addAll(generateManualTestsForSource(testFolder, dataset,autoGenerators));

        /*notify start of testing */
        for (TestGeneratorExecutorMonitor monitor : progressMonitors){
            monitor.generationFinished();
        }

        return  allTests;
    }

    private List<UnitTest> generateAutoTestsForSchemaSource(String testFolder, SchemaSource s, List<TestAutoGenerator> autoGenerators) {
        List<UnitTest> tests = new ArrayList<UnitTest>();

        for (TestGeneratorExecutorMonitor monitor : progressMonitors){
            monitor.sourceGenerationStarted(s, TestGenerationType.AutoGenerated);
        }

        try {
            List<UnitTest> testsAutoCached = TestUtils.instantiateTestsFromModel(
                    TripleReaderFactory.createTripleFileReader(CacheUtils.getSourceAutoTestFile(testFolder,s)).read());
            tests.addAll(testsAutoCached);
            log.info(s.getUri() + " contains " + testsAutoCached.size() + " automatically created tests (loaded from cache)");

        } catch (TripleReaderException e) {
            // cannot read from file  / generate
            List<UnitTest> testsAuto = TestUtils.instantiateTestsFromAG(autoGenerators, s);
            tests.addAll(testsAuto);
            TestUtils.writeTestsToFile(testsAuto, CacheUtils.getSourceAutoTestFile(testFolder,s));
            log.info(s.getUri() + " contains " + testsAuto.size() + " automatically created tests");
        }

        for (TestGeneratorExecutorMonitor monitor : progressMonitors){
            monitor.sourceGenerationExecuted(s, TestGenerationType.AutoGenerated, tests.size());
        }

        return tests;
    }

    private List<UnitTest> generateManualTestsForSource(String testFolder, Source s, List<TestAutoGenerator> autoGenerators) {
        List<UnitTest> tests = new ArrayList<UnitTest>();

        for (TestGeneratorExecutorMonitor monitor : progressMonitors){
            monitor.sourceGenerationStarted(s, TestGenerationType.ManuallyGenerated);
        }
        try {
            List<UnitTest> testsManuals = TestUtils.instantiateTestsFromModel(
                    TripleReaderFactory.createTripleFileReader(CacheUtils.getSourceManualTestFile(testFolder,s)).read());
            tests.addAll(testsManuals);
            log.info(s.getUri() + " contains " + testsManuals.size() + " manually created tests");
        } catch (TripleReaderException e) {
            // Do nothing, Manual tests do not exist
        }

        for (TestGeneratorExecutorMonitor monitor : progressMonitors){
            monitor.sourceGenerationExecuted(s, TestGenerationType.ManuallyGenerated, tests.size());
        }

        return tests;


    }




    public void addTestExecutorMonitor(TestGeneratorExecutorMonitor monitor) {
        progressMonitors.add(monitor);
    }

    public void removeTestExecutorMonitor(TestGeneratorExecutorMonitor monitor) {
        progressMonitors.remove(monitor);
    }
}
