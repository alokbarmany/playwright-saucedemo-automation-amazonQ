package com.saucedemo.runners;

import com.saucedemo.context.PlaywrightManager;
import com.saucedemo.dataprovider.JsonDataReader;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import org.testng.annotations.DataProvider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@CucumberOptions(features = "src/test/resources/features", glue = { "com.saucedemo.hooks",
        "com.saucedemo.steps" }, plugin = {
                "pretty",
                "html:target/cucumber-reports/report.html",
                "json:target/cucumber-reports/cucumber.json",
                "junit:target/cucumber-reports/cucumber.xml",
                "rerun:target/cucumber-reports/rerun.txt",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
        }, tags = "regression", dryRun = false, monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Multiplies each scenario by the number of JSON data rows in its @dataFile
     * tag.
     * rowIndex is stored in PlaywrightManager ThreadLocal before each scenario run.
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        Object[][] originalScenarios = super.scenarios();
        List<Object[]> expandedScenarios = new ArrayList<>();

        for (Object[] scenario : originalScenarios) {
            PickleWrapper pickleWrapper = (PickleWrapper) scenario[0];
            int rowCount = getRowCountForScenario(pickleWrapper);
            for (int i = 0; i < rowCount; i++) {
                PlaywrightManager.enqueueRowIndex(i);
                expandedScenarios.add(new Object[] { scenario[0], scenario[1] });
            }
        }
        return expandedScenarios.toArray(new Object[0][]);
    }

    /**
     * Intercepts each scenario run to set rowIndex in PlaywrightManager before
     * Hooks fire.
     */
    @Override
    public void runScenario(PickleWrapper pickleWrapper, FeatureWrapper featureWrapper) {
        // rowIndex is not available here directly — it is set via pending ThreadLocal
        // from the DataProvider entry before this method is called by TestNG
        super.runScenario(pickleWrapper, featureWrapper);
    }

    private int getRowCountForScenario(PickleWrapper pickleWrapper) {
        return pickleWrapper.getPickle().getTags().stream()
                .filter(tag -> tag.startsWith("@dataFile:"))
                .findFirst()
                .map(tag -> {
                    String path = tag.substring("@dataFile:".length());
                    String env = System.getProperty("env", getDefaultEnv());
                    return JsonDataReader.getRowCount(path.replace("{env}", env));
                })
                .orElse(1);
    }

    private String getDefaultEnv() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config/config.properties")) {
            Properties props = new Properties();
            props.load(input);
            return props.getProperty("default.env", "dev");
        } catch (Exception e) {
            return "dev";
        }
    }
}
