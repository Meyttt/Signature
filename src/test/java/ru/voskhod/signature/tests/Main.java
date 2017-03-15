package ru.voskhod.signature.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by a.chebotareva on 14.03.2017.
 */
public class Main {
    public static final String url = "https://ep.minsvyaz.ru/Esep-ExternalSystem/UIToSing/New.aspx";
    public static void main(String[] args) throws InterruptedException {
        DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR,"dismiss");
        capabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
        System.setProperty("webdriver.ie.driver",(new File("data/IEDriverServer.exe")).getAbsolutePath());
        InternetExplorerDriver driver = new InternetExplorerDriver(capabilities);
        WebDriverWait wait = new WebDriverWait(driver,5);
        driver.navigate().to(url);
//        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
//        while(!driver.getCurrentUrl().equalsIgnoreCase(url)){
//            driver.close();
//            driver = new InternetExplorerDriver(capabilities);
//            driver.navigate().to(url);
//            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
//        }
//        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"colTwo\"]/h2")));
        driver.manage().timeouts().implicitlyWait(5,TimeUnit.SECONDS);
        driver.findElement(By.name("ctl00$cph2$fileUpload")).sendKeys(new File("data/cat2.JPG").getAbsolutePath());
        driver.findElement(By.name("ctl00$cph2$buttonFileUpload")).click();
        Thread.sleep(5000);
//        driver.manage().timeouts().implicitlyWait(5,TimeUnit.SECONDS);
        driver.findElement(By.id("cph2_signDocuments")).click();

    }
}
