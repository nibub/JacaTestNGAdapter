import com.jaca.TestAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by nibu.baby on 12/1/2016.
 */
public class ScreenShotsTests extends TestBase {

    WebDriver driver;

    @BeforeMethod
    public void setup() {
        driver = new FirefoxDriver();
        super.driver = driver;
        System.out.println("In setup method");
    }

    @AfterMethod
    public void tearDown() {
        driver.close();
        System.out.println("In teardown method");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test one")
    @TestAnnotations(testID = "SRNT001")
    public void testMethodOne() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test one");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test two")
    @TestAnnotations(testID = "SRNT002")
    public void testMethodTwo() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test two");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }
/*
    @Test(groups = {"Smoke", "Web"}, description = "Screen test Three")
    @TestAnnotations(testID = "SRNT003")
    public void testMethodThree() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test Three");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test four")
    @TestAnnotations(testID = "SRNT004")
    public void testMethodFour() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test four");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test five")
    @TestAnnotations(testID = "SRNT005")
    public void testMethodFive() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test five");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test six")
    @TestAnnotations(testID = "SRNT006")
    public void testMethodSix() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test SIX");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test five")
    @TestAnnotations(testID = "SRNT007")
    public void testMethodSeven() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test SEVEN");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }

    @Test(groups = {"Smoke", "Web"}, description = "Screen test Eight")
    @TestAnnotations(testID = "SRNT008")
    public void testMethodEight() throws InterruptedException {
        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Screen test EIGHT");
        Thread.sleep(500);
        Assert.fail("Failed the test");
    }*/




}
