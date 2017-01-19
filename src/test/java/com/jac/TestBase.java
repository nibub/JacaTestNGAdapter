package com.jac;

import com.jaca.JacaBase;
import org.openqa.selenium.WebDriver;

/**
 * Created by nibu.baby on 5/26/2016.
 */
public class TestBase implements JacaBase {
    public WebDriver driver;

    public WebDriver getDriver() {
        return this.driver;
    }
}
