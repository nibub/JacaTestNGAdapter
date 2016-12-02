import com.jaca.Base;
import org.openqa.selenium.WebDriver;

/**
 * Created by nibu.baby on 5/26/2016.
 */
public class TestBase implements Base {
    WebDriver driver;

    public WebDriver getDriver() {
        return this.driver;
    }
}
