package com.jaca;

import com.google.inject.Inject;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.*;
import org.testng.internal.IResultListener;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by nibu.baby on 5/24/2016.
 */
public class ReportAdapterListener implements IResultListener, ISuiteListener, IInvokedMethodListener {

    private static final String EXECUTION_ID;
    private static final Logger LOGGER = Logger.getLogger(ReportAdapterListener.class.getName());
    private static String baseUrl;
    private static boolean isConfigured = false;
    private static boolean isExtraInfoUpdated=true;
    private static FileHandler fileHandler = null;

    static {
        final UUID uuid = UUID.randomUUID();
        EXECUTION_ID = uuid.toString();
        try {
            fileHandler = new FileHandler("JacaListnerLog.log");
            baseUrl = System.getProperty("JURL");
//            baseUrl="http://localhost:8080/";
            if (baseUrl != null && !baseUrl.isEmpty()) {
                LOGGER.info("**Jaca Adapter v2.1**");
                isConfigured = true;
            } else {
                LOGGER.info("Jaca is not configured, Set reporting dashboard base url command line parameter -- 'JURL' ");
            }
        } catch (NullPointerException e) {
            System.out.println("Jaca is not configured, Set reporting dashboard base url command line parameter  -- 'JURL' ");
            LOGGER.info("Jaca is not configured, Set reporting dashboard base url command line parameter -- 'JURL' ");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

    }

    @Inject
    WebDriver driver;
    private Formatter simpleFormatter = null;
    private int suiteID;
    private int testsBuildID;
    private long suiteStartingTime;
    private Map<String, HashMap<String, Integer>> groupsInfoLists = new HashMap<String, HashMap<String, Integer>>();
    private boolean isBuildInfoUpdated;
    private int buildNo;
    private String jobName;
    private Map<String, Integer> suiteMap = new HashMap<String, Integer>();
    private String basePackName = null;


    public void onStart(ISuite iSuite) {
        simpleFormatter = new SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);
        LOGGER.addHandler(fileHandler);
        if (isConfigured) {
            LOGGER.info("Adding the suite info");
            suiteUpdate(iSuite);
        }

    }

    private void suiteUpdate(ISuite iSuite) {
        //Add method mapping to config methods (before methods)
        String name = iSuite.getName();
        iSuite.getAttributeNames().toArray();
        int suiteSize = iSuite.getAllMethods().size();

        suiteStartingTime = Instant.now().toEpochMilli();
        Calendar calendar = Calendar.getInstance();
        JSONObject json = new JSONObject();
        json.put("suiteName", iSuite.getName());
        json.put("executionID", EXECUTION_ID);
        json.put("executionDate", Instant.now().toEpochMilli());
        json.put("status", "InProgress");
        json.put("intSize", suiteSize);

        buildNo = Integer.parseInt(System.getenv("BUILD_NUMBER"));
        int buildID = Integer.parseInt(System.getenv("BUILD_ID"));
        String buildUrl = System.getenv("BUILD_URL");
        jobName = System.getenv("JOB_NAME");
        basePackName = System.getProperty("JBASE");
        String envName = System.getProperty("env");
        String project = System.getProperty("project");
        System.out.println("Environment " + envName);

       /*Testing information*/
        /*jobName = "SampleJob_V2_One";
        buildNo = 1;
        String buildUrl = "www.url.com";
        basePackName ="com.jac";
        String envName = "QA Env";
        String project =" V2 App";*/
        /**********/


        if (!isBuildInfoUpdated) {

            JSONObject buildInfo = new JSONObject();
            buildInfo.put("executionID", EXECUTION_ID);
            buildInfo.put("timeStamp", Instant.now().toEpochMilli());
            buildInfo.put("jobName", jobName);
            buildInfo.put("buildUrl", buildUrl);
            buildInfo.put("project", project);
            buildInfo.put("buildEnv", envName);
            buildInfo.put("buildNo", buildNo);
            buildInfo.put("buildStatus", "InProgress");
            buildInfo.put("runnerType", 1);


            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(baseUrl + "/build");
            StringEntity params = null;

            try {
                params = new StringEntity(buildInfo.toString());

                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);
                int code = response.getStatusLine().getStatusCode();
                StringBuffer result = new StringBuffer();
                if (code == 200) {
                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                        testsBuildID = Integer.parseInt(result.toString());
                    }
                }

            } catch (IOException e) {
                LOGGER.info("Issues while updating build info, Check the endpoint '" + baseUrl + "' , " + e.getMessage());
                e.printStackTrace();
            }

            isBuildInfoUpdated = true;
        }


        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(baseUrl + "/suite");
        StringEntity params = null;

        try {
            params = new StringEntity(json.toString());

            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            StringBuffer result = new StringBuffer();
            if (code == 200) {
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                    suiteID = Integer.parseInt(result.toString());
                    suiteMap.put(name, suiteID);
                }
            } else {
                LOGGER.info("Suite info post failed with status code :" + code);
            }


        } catch (IOException e) {
            LOGGER.info("Issues while adding suite info" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onFinish(ISuite iSuite) {
        if (isConfigured) {
            System.out.println("reporter suite finish");
            int numBerOfMethods = iSuite.getAllMethods().toArray().length;
            LOGGER.info("Updating suite info");
            updateSuiteResults(iSuite);
            /*AdditionalInfo suiteInfo = new AdditionalInfo();
            if (!suiteInfo.getInfoMap().isEmpty()) {
                ///need to add
                suiteInfo.getInfoMap().forEach((k, v) -> {
                    addAdditionalExecInfo(k, v);
                });
            }*/

        }
    }

    public void onConfigurationSuccess(ITestResult iTestResult) {

    }

    public void onConfigurationFailure(ITestResult iTestResult) {

    }

    public void onConfigurationSkip(ITestResult iTestResult) {

    }

    public void onTestStart(ITestResult iTestResult) {
        iTestResult.getName();
        iTestResult.getTestName();
//        iTestResult.setAttribute("WebDriver", this.driver);


    }

    public void onTestSuccess(ITestResult iTestResult) {
        if (isConfigured) {
            testResultUpdate(iTestResult);
        }
    }


    public void onTestFailure(ITestResult iTestResult) {

        if (isConfigured) {
            testResultUpdate(iTestResult);
        }
    }

    public void onTestSkipped(ITestResult iTestResult) {

        if (isConfigured) {
            testResultUpdate(iTestResult);
        }
    }

    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {

    }

    public void onStart(ITestContext iTestContext) {
        iTestContext.setAttribute("WebDriver", this.driver);
    }

    public void onFinish(ITestContext iTestContext) {

    }

    private void updateSuiteResults(ISuite suite) {
        int numberOfFailedTests = 0;
        int numberOfSkippedTest = 0;
        int numberOfPassedTest = 0;
        int totalTests = 0;

        Map<String, ISuiteResult> results = suite.getResults();
        for (ISuiteResult r2 : results.values()) {
            ITestContext testContext = r2.getTestContext();
            String suiteName = testContext.getName();
            System.out.println("*** Suite name form update Suite results ::" + suiteName);
            int totalNumberOfTests = testContext.getAllTestMethods().length;
            numberOfFailedTests += testContext.getFailedTests().size();
            numberOfSkippedTest += testContext.getSkippedTests().size();
            numberOfPassedTest += testContext.getPassedTests().size();
            int skippedConfig = testContext.getSkippedConfigurations().size();
            int failedConfig = testContext.getFailedConfigurations().size();
//             totalTests =numberOfFailedTests+ numberOfSkippedTest+numberOfPassedTest;
            String startDate = testContext.getStartDate().toString();
            testContext.getEndDate();
            Set<ITestResult> iTResults = testContext.getFailedTests().getAllResults();
            Collection<ITestNGMethod> methods = testContext.getFailedTests().getAllMethods();
            IResultMap resultMap = testContext.getFailedTests();
            addGroupsInfo(testContext.getFailedTests(), "Failed");
            addGroupsInfo(testContext.getPassedTests(), "Passed");
            addGroupsInfo(testContext.getSkippedTests(), "Skipped");
        }

        try {
            totalTests = numberOfFailedTests + numberOfSkippedTest + numberOfPassedTest;
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            //get current date time with Date()
            Date date = new Date();
            JSONObject json = new JSONObject();
            long finishingTime = Instant.now().toEpochMilli();
            long timeTaken = finishingTime - suiteStartingTime;
            String suiteStatus;
            if (suite.getSuiteState().isFailed()) {
                suiteStatus = "false";
            } else {
                suiteStatus = "true";
            }
            json.put("status", suiteStatus);
            json.put("timeTaken", timeTaken);
            json.put("numfailedTests", numberOfFailedTests);
            json.put("numSkippedTests", numberOfSkippedTest);
            json.put("numPassedTests", numberOfPassedTest);
            json.put("totalTests", totalTests);

            JSONArray ja = new JSONArray();
            for (String groupName : groupsInfoLists.keySet()) {

                HashMap<String, Integer> resultsStatuses = groupsInfoLists.get(groupName);
                JSONObject tagsInfo = new JSONObject();
                tagsInfo.put("name", groupName);
                tagsInfo.put("suiteID", suiteID);
                tagsInfo.put("executionID", EXECUTION_ID);
                tagsInfo.put("pass", resultsStatuses.get("Pass"));
                tagsInfo.put("fail", resultsStatuses.get("Fail"));
                tagsInfo.put("skip", resultsStatuses.get("Skip"));
                int total = resultsStatuses.get("Pass") + resultsStatuses.get("Fail") + resultsStatuses.get("Skip");
                tagsInfo.put("total", total);
                ja.put(tagsInfo);
            }

            json.put("tagsInfo", ja);
            HttpClient httpClient = HttpClientBuilder.create().build();
            String url = baseUrl + "/suite/" + String.valueOf(suiteID);
            HttpPut request = new HttpPut(url);
            StringEntity params = null;

            params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                LOGGER.info("Suite info update failed with status code :" + response.getStatusLine().getStatusCode());
            }
        } catch (UnsupportedEncodingException | ClientProtocolException e) {
            LOGGER.info("Issues while updating suite info " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            LOGGER.info("Issues while updating suite info " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testResultUpdate(ITestResult iTestResult) {

        boolean isScreenCaptured = false;
        boolean errorMsg = false;
        String errorMessage = "";
        String currentUrl = null;
        String browserName = null;
        String osName = null;
        String osVersion = null;
        String screenShot = null;
        String browserVersion = null;
        boolean platformInfoCaptured = false;
        String[] parameterValues = null;
        String exType = null;
        String exClassName = null;
        String exMethodName = null;

        if (iTestResult.getMethod().isTest()) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                //get current date time with Date()
                Date date = new Date();
                String testName = iTestResult.getName();
                iTestResult.getAttributeNames();
                boolean isTestParametrized = false;

                String testID = "";
                int priority=0 ;
                if (iTestResult.getMethod().getConstructorOrMethod().getMethod().isAnnotationPresent(TestAnnotations.class)) {
                    testID = iTestResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestAnnotations.class).testID();
                    priority =iTestResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestAnnotations.class).priority();
                }

            /*if (iTestResult.getParameters().length > 0) {
                isTestParametrized = true;
                parameterValues = (String[])iTestResult.getParameters();

            }*/
                String suiteName = iTestResult.getTestContext().getSuite().getName();
                String description = iTestResult.getMethod().getDescription();

                String dependsOn = Arrays.toString(iTestResult.getMethod().getMethodsDependedUpon());

                int invocationCount = iTestResult.getMethod().getCurrentInvocationCount();
                String[] groups = iTestResult.getMethod().getGroups();
                String id = iTestResult.getMethod().getId();
                String methodName = iTestResult.getMethod().getMethodName();
                String className = iTestResult.getMethod().getTestClass().getName();
                boolean b = iTestResult.getMethod().isTest();
                long startMilliseconds = iTestResult.getEndMillis();
                long endMilliseconds = iTestResult.getStartMillis();

                long totalTimeMilliseconds = startMilliseconds - endMilliseconds;
                if (iTestResult.getThrowable() != null) {
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw, true);
                    iTestResult.getThrowable().printStackTrace(pw);
                    iTestResult.getThrowable().getStackTrace();
                    exType = iTestResult.getThrowable().getClass().getSimpleName();
                    errorMessage = sw.getBuffer().toString();
                    if (basePackName != null) {
//                    Stream<StackTraceElement > str  = Arrays.stream(iTestResult.getThrowable().getStackTrace()).filter(item->item.getClassName().startsWith(""));
                        Object[] str = Arrays.stream(iTestResult.getThrowable().getStackTrace()).filter(item -> item.getClassName().startsWith(basePackName)).toArray();
                        if (str.length > 0) {
                            StackTraceElement element = (StackTraceElement) str[0];
                            exClassName = element.getClassName();
                            exMethodName = element.getMethodName();
                            element.getLineNumber();
                        }
                    }
                    errorMsg = true;
                }
                iTestResult.getMethod().getSuccessPercentage();

                List<String> reporterLogs = Reporter.getOutput(iTestResult);
                int status = iTestResult.getStatus();
                long timeTaken = startMilliseconds - endMilliseconds;

                Object currentClass = iTestResult.getInstance();
                if (currentClass instanceof JacaBase) {
                    driver = ((JacaBase) currentClass).getDriver();
                    if (driver instanceof RemoteWebDriver) {

                        Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
                        System.out.println("Browser name:" + cap.getBrowserName());
                        browserName = cap.getBrowserName().toLowerCase();
                        osName = cap.getPlatform().family().toString();
                        browserVersion = cap.getVersion().toString();
                        try {
                            currentUrl = driver.getCurrentUrl();
                        } catch (Exception e) {
                            LOGGER.info("Unable to capture the Driver url, Check the execution conditions,Browser might have closed/not launched");
                        }
                        platformInfoCaptured = true;
                    } else {
                        LOGGER.info("Unable to find Remote Webdriver instance, Verify the driver implementation");
                    }
                } else {
                    LOGGER.info("Unable to capture driver info, driver object info should be available (implement JacaBase Interface)");
                }



                    if (iTestResult.getStatus() == ITestResult.FAILURE) {
                        if (currentClass instanceof JacaBase) {
                            try {
                            screenShot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                            }
                            catch (Exception e) {
                                LOGGER.info("Unable to capture screen shot" + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            LOGGER.info("Unable to capture failure screen shot, driver object info should be available (implement JacaBase Interface)");
                        }
                    }


                long timeStamp = Instant.now().toEpochMilli();

                JSONObject json = new JSONObject();
                json.put("testName", testName);
                json.put("suiteID", suiteMap.get(suiteName));
                json.put("description", description);
                json.put("status", status);
                json.put("className", className);
                json.put("passDate", dateFormat.format(date));
                json.put("executionTime", timeStamp);
                json.put("executionID", EXECUTION_ID);
                json.put("timeTaken", timeTaken);
                json.put("suiteName", suiteName);
                json.put("groupTags", groups);
//                json.put("parametrized", isTestParametrized);
                json.put("reporterLogs", reporterLogs);
                json.put("testID", testID);
                json.put("tcPriority", priority);
                json.put("JobName", jobName);
                json.put("buildJobName", jobName);
                json.put("buildNo", buildNo);
                json.put("runnerType", 1);
                if (platformInfoCaptured) {
                    json.put("platFormInfo", osName);
                    json.put("browserType", browserName);
                    json.put("browserVersion", browserVersion);
                }

                /*if (isTestParametrized) {
                    json.put("tcParameters", parameterValues);
                }*/


                if (screenShot != null) {
                    isScreenCaptured = true;
                    json.put("screenShot", screenShot);
                    LOGGER.info("Screen captured");
                }

                if (errorMsg) {
                    json.put("errorLog", errorMessage);
                    if (platformInfoCaptured) {
                        json.put("currentUrl", currentUrl);
                    }
                    json.put("exType", exType);
                    json.put("exMethodName", exMethodName);
                    json.put("exClassName", exClassName);
                }
                json.put("screenCaptured", isScreenCaptured);
                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost request = new HttpPost(baseUrl + "/scenario");
                StringEntity params = null;

                params = new StringEntity(json.toString());
                request.addHeader("content-type", "application/json");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);
                int code = response.getStatusLine().getStatusCode();
                StringBuffer result = new StringBuffer();
                if (code == 200) {
                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                } else {
                    LOGGER.info("Scenario update calls failed with status code :" + code);
                }

            } catch (UnsupportedEncodingException | ClientProtocolException e) {
                LOGGER.info("Issues while updating test case details " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                LOGGER.info("Issues while updating test case details " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                LOGGER.info("Unable to proceed Update " + e.getMessage());
                e.printStackTrace();
            }

        }
    }

    public void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
    }

    public void afterInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
        if (iTestResult.getMethod().isTest()) {
            iInvokedMethod.getTestResult().getParameters();
        }

        Method[] methods = iInvokedMethod.getClass().getDeclaredMethods();
        for (Method method : methods) {
            Parameter[] parameters = method.getParameters();
            for (Parameter p : parameters) {
                if (p.isNamePresent()) {
                    System.out.println(p.toString() + " : " + p.getName());
                }
            }
        }

    }


    private void addGroupsInfo(IResultMap resultMap, String status) {
        Collection<ITestNGMethod> methods = resultMap.getAllMethods();
        for (ITestNGMethod m : methods) {
            String[] groups = m.getGroups();
            for (String group : groups) {
                if (!groupsInfoLists.containsKey(group)) {
                    HashMap<String, Integer> statuses = new HashMap<String, Integer>();
                    statuses.put("Pass", 0);
                    statuses.put("Fail", 0);
                    statuses.put("Skip", 0);
                    groupsInfoLists.put(group, statuses);
                }
                if (groupsInfoLists.containsKey(group)) {
                    HashMap<String, Integer> sts = groupsInfoLists.get(group);

                    if (status == "Passed") {
                        String key = "Pass";
                        sts.put(key, sts.get(key) + 1);
                    }

                    if (status == "Failed") {
                        String key = "Fail";
                        sts.put(key, sts.get(key) + 1);
                    }
                    if (status == "Skipped") {
                        String key = "Skip";
                        sts.put(key, sts.get(key) + 1);
                    }
                }

            }
        }

    }

    protected static void addAdditionalExecInfo(String key, String value) {

        JSONObject suiteInfo = new JSONObject();
        suiteInfo.put("executionID", EXECUTION_ID);
        suiteInfo.put("executionDate", Instant.now().toEpochMilli());
        suiteInfo.put("infoKey", key);
        suiteInfo.put("infoValue", value);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(baseUrl + "/suiteinfo");
        StringEntity params = null;

        try {
            params = new StringEntity(suiteInfo.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            int code = response.getStatusLine().getStatusCode();
            StringBuffer result = new StringBuffer();
            if (code != 200) {
                LOGGER.info("Issues while updating Suite info, Check the endpoint ....");
            }

        } catch (IOException e) {
            LOGGER.info("Issues while updating Suite info, Check the endpoint '" + baseUrl + "' , " + e.getMessage());
            e.printStackTrace();
        }

    }

}
