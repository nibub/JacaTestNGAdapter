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
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.*;
import org.testng.internal.IResultListener;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * Created by nibu.baby on 5/24/2016.
 */
public class ReportAdapterListener implements IResultListener, ISuiteListener, IInvokedMethodListener {

    private static final String EXECUTION_ID;
    private static String baseUrl;
    private static boolean isConfigured = false;

    static {
        UUID uuid = UUID.randomUUID();
        EXECUTION_ID = uuid.toString();
        try {
            baseUrl = System.getProperty("JURL");
            if (baseUrl != null && !baseUrl.isEmpty()) {
                isConfigured = true;
            }
        } catch (NullPointerException e) {
            System.out.println("Jaca is not configured, Set reporting dashboard base url property-- 'jurl' ");
        }

    }

    @Inject
    WebDriver driver;
    private int suiteID;
    private int testsBuildID;
    private long suiteStartingTime;
    private Map<String, HashMap<String, Integer>> groupsInfoLists = new HashMap<String, HashMap<String, Integer>>();
    private boolean isBuildInfoUpdated;


    public void onStart(ISuite iSuite) {

        if (isConfigured) {
            suiteUpdate(iSuite);
        }


    }

    private void suiteUpdate(ISuite iSuite) {
        //Add method mapping to config methods (before methods)
        String name = iSuite.getName();
        iSuite.getAttributeNames().toArray();
        suiteStartingTime = Instant.now().toEpochMilli();
        Calendar calendar = Calendar.getInstance();
        JSONObject json = new JSONObject();
        json.put("suiteName", iSuite.getName());
        json.put("executionID", EXECUTION_ID);
        json.put("executionDate", Instant.now().toEpochMilli());
        json.put("status", "InProgress");

        int buildNo = Integer.parseInt(System.getenv("BUILD_NUMBER"));
        int buildID = Integer.parseInt(System.getenv("BUILD_ID"));
        String buildUrl = System.getenv("BUILD_URL");
        String jobName = System.getenv("JOB_NAME");
        String envName = System.getProperty("env");
        String project = System.getProperty("project");
        System.out.println("Environment " + envName);


        if (!isBuildInfoUpdated) {

            JSONObject buildInfo = new JSONObject();
            buildInfo.put("executionID", EXECUTION_ID);
            buildInfo.put("timeStamp", Instant.now().toEpochMilli());
            buildInfo.put("jobName", jobName);
            buildInfo.put("buildUrl", buildUrl);
            buildInfo.put("project", project);
            buildInfo.put("buildEnv", envName);
            buildInfo.put("buildNo", buildNo);


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
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onFinish(ISuite iSuite) {
        if (isConfigured) {
            System.out.println("reporter suite finish");
            int numBerOfMethods = iSuite.getAllMethods().toArray().length;
            updateSuiteResults(iSuite);
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
            httpClient.execute(request);
        } catch (UnsupportedEncodingException | ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testResultUpdate(ITestResult iTestResult) {

        boolean isScreenCaptured = false;
        boolean errorMsg = false;
        String errorMessage = "";

        if (iTestResult.getMethod().isTest()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            //get current date time with Date()
            Date date = new Date();
            String testName = iTestResult.getName();
            iTestResult.getAttributeNames();
            boolean isTestParametrized = false;
            String parameterValues = "";
            String testID = "";
            if (iTestResult.getMethod().getConstructorOrMethod().getMethod().isAnnotationPresent(TestAnnotations.class)) {
                testID = iTestResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestAnnotations.class).testID();
            }

            if (iTestResult.getParameters().length > 0) {
                isTestParametrized = true;
                parameterValues = Arrays.toString(iTestResult.getParameters());
            }
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
                errorMessage = sw.getBuffer().toString();
                errorMsg = true;
            }
            iTestResult.getMethod().getSuccessPercentage();

            List<String> reporterLogs = Reporter.getOutput(iTestResult);
            int status = iTestResult.getStatus();
            long timeTaken = startMilliseconds - endMilliseconds;
            String screenShot = null;
            if (iTestResult.getStatus() == 2) {
                Object currentClass = iTestResult.getInstance();
                driver = ((Base) currentClass).getDriver();
                screenShot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            }


            long timeStamp = Instant.now().toEpochMilli();
            try {
                JSONObject json = new JSONObject();

                json.put("testName", testName);
                json.put("suiteID", suiteID);
                json.put("description", description);
                json.put("status", status);
                json.put("className", className);
                json.put("passDate", dateFormat.format(date));
                json.put("executionTime", timeStamp);
                json.put("executionID", EXECUTION_ID);
                json.put("timeTaken", timeTaken);
                json.put("suiteName", suiteName);
                json.put("groupTags", groups);
                json.put("parametrized", isTestParametrized);
                json.put("reporterLogs", reporterLogs);
                json.put("testID", testID);
                if (isTestParametrized) {
                    json.put("tcParameters", parameterValues);
                }


                if (screenShot != null) {
                    isScreenCaptured = true;
                    json.put("screenShot", screenShot);
                    System.out.println("Screen captured");
                }

                if (errorMsg) {
                    json.put("errorLog", errorMessage);
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
                }


            } catch (UnsupportedEncodingException | ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
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

}
