package ru.voskhod.signature.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by a.chebotareva on 14.03.2017.
 */
public class Main {
    public static final String url = "https://ep.minsvyaz.ru/Esep-ExternalSystem/UIToSing/New.aspx";
    public static void main(String[] args) throws InterruptedException {
        DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR,"dismiss");
        capabilities.setCapability(InternetExplorerDriver.INITIAL_BROWSER_URL,url);
        capabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
        System.setProperty("webdriver.ie.driver",(new File("data/IEDriverServer.exe")).getAbsolutePath());
        InternetExplorerDriver driver = new InternetExplorerDriver(capabilities);
        WebDriverWait wait = new WebDriverWait(driver,30);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cph2_listFileAccessCodes")));
        }catch (org.openqa.selenium.TimeoutException e){
            System.err.println("Драйвер запустился некорректно.");
            driver.close();
            driver.quit();
            return;
        }
        String oldHandle = driver.getWindowHandle();
        driver.findElement(By.name("ctl00$cph2$fileUpload")).sendKeys(new File("data/cat2.JPG").getAbsolutePath());
        driver.findElement(By.name("ctl00$cph2$buttonFileUpload")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"cph2_listFileAccessCodes\"]/option[1]")));
        driver.findElement(By.id("cph2_signDocuments")).click();
        driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
        Set<String> windows= driver.getWindowHandles();
        windows.remove(oldHandle);
        driver.switchTo().window(windows.toArray()[0].toString());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("image-container")));
        driver.findElement(By.xpath("//*[@id=\"content\"]/div/div[1]/div[1]/div[1]/a[2]")).click();
        driver.manage().timeouts().implicitlyWait(2,TimeUnit.SECONDS);
        driver.findElement(By.id("certificates-list")).findElements(By.xpath(".//*")).get(2).click();
        driver.findElement(By.xpath("//*[@id=\"sign-document-dialog\"]/div/div/div/a[2]")).click();
        driver.manage().timeouts().implicitlyWait(2,TimeUnit.SECONDS);
        driver.findElement(By.id("pinCode")).sendKeys("123456");
        List<WebElement> buttons = driver.findElements(By.className("ui-button-text"));
        for(WebElement button:buttons){
            if(button.getText().equalsIgnoreCase("Готово")){
                button.findElement(By.xpath(".//..")).click();
            }
        }
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cph2_ButtonDownload")));
        driver.findElement(By.id("cph2_ButtonDownload"));
    }
}
