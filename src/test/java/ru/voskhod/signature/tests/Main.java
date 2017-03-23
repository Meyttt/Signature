package ru.voskhod.signature.tests;

import org.apache.commons.codec.binary.Hex;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Created by a.chebotareva on 14.03.2017.
 */
public class Main {
    Config config= new Config("config.properties");
    private static String url;
    private static String validUrl;
    private static String filename;
    private static String zipname;
    private static String fileToCompare;
    private static String downloadedFile;

    private WebDriver chromeDriver;
    InternetExplorerDriver driver;
    WebDriverWait wait;

    public Main() throws IOException {
    }

    @BeforeClass
    private  void  init() throws IOException {
        url=config.get("url");
        validUrl=config.get("validUrl");
        filename=config.get("fileToDownload");
        zipname="data/docs/"+config.get("fileToDownload").split("/")[1]+".zip";
        fileToCompare = config.get("fileToCompare");
//        downloadedFile = config.get("downloadedFile");
        clearDirectory();
        initIEDRiver();
        initChromeDriver();
    }

    private  void initIEDRiver(){
        DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, "dismiss");
        capabilities.setCapability(InternetExplorerDriver.INITIAL_BROWSER_URL, url);
        capabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
        capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        capabilities.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
        System.setProperty("webdriver.ie.driver", (new File("data/IEDriverServer.exe")).getAbsolutePath());
        driver = new InternetExplorerDriver(capabilities);
        wait = new WebDriverWait(driver, 30);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cph2_listFileAccessCodes")));
        } catch (org.openqa.selenium.TimeoutException e) {
            System.err.println("Драйвер запустился некорректно.");
            driver.close();
            driver.quit();
            return;
        }
    }
    private void initChromeDriver(){
        System.setProperty("webdriver.chrome.driver", "data/chromedriver.exe");
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.default_directory", (new File("data/docs")).getAbsolutePath());
        chromePrefs.put("plugins.plugins_disabled", new String[] {
                "Adobe Flash Player",
                "Chrome PDF Viewer"
        });
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        DesiredCapabilities cap = DesiredCapabilities.chrome();
        cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        cap.setCapability(ChromeOptions.CAPABILITY, options);
        chromeDriver = new ChromeDriver(cap);
    }
    @Test
    public void main() throws InterruptedException, IOException, NoSuchAlgorithmException {
            String oldHandle = driver.getWindowHandle();
            driver.findElement(By.name("ctl00$cph2$fileUpload")).sendKeys(new File(filename).getAbsolutePath());
            driver.findElement(By.name("ctl00$cph2$buttonFileUpload")).click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"cph2_listFileAccessCodes\"]/option[1]")));
            driver.findElement(By.id("cph2_signDocuments")).click();
            wait.until(ExpectedConditions.numberOfWindowsToBe(2));
            //переходим на новую страницу
            Set<String> windows = driver.getWindowHandles();
            windows.remove(oldHandle);
            driver.switchTo().window(windows.toArray()[0].toString());
            //ждем загрузки страницы с предпоказом
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("image-container")));
            testPictureOnSite();
            driver.findElement(By.xpath("//*[@id=\"content\"]/div/div[1]/div[1]/div[1]/a[2]")).click();
            driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
            try {
                driver.findElement(By.id("certificates-list")).findElements(By.xpath(".//*")).get(2).click();
            }catch (IndexOutOfBoundsException e){
                driver.navigate().back();
                driver.switchTo().window(oldHandle);

            }
            driver.findElement(By.xpath("//*[@id=\"sign-document-dialog\"]/div/div/div/a[2]")).click();
            driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
            driver.findElement(By.id("pinCode")).sendKeys("123456");
            List<WebElement> buttons = driver.findElements(By.className("ui-button-text"));
            for (WebElement button : buttons) {
                if (button.getText().equalsIgnoreCase("Готово")) {
                    button.findElement(By.xpath(".//..")).click();
                }
            }
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cph2_ButtonDownload")));
            String downloadUrl;
            getDownloadUrl:while(true){
                downloadUrl= driver.getCurrentUrl();
                if(downloadUrl!=null){
                    if(downloadUrl.contains("http")){
                        break getDownloadUrl;
                    }
                }
            }
            downloadByChrome(downloadUrl);
            Thread.sleep(10000);
            try {
                downloadedFile = unpackZip(zipname);
            } catch (IOException e) {
                e.printStackTrace();
            }
            validTest(downloadedFile);


    }
    private void downloadByChrome(String url){
        chromeDriver.get(url);
        chromeDriver.findElement(By.xpath("//*[@id=\"cph2_ButtonDownload\"]")).click();
        chromeDriver.manage().timeouts().implicitlyWait(10,TimeUnit.SECONDS);
        return;
    }

    private String unpackZip(String filepath) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filepath));
        ZipEntry entry = zipInputStream.getNextEntry();
        byte[] buf = new byte[1024];
        String entryname = entry.getName();
        File file = new File(filepath);
        String directory = file.getParent();
        FileOutputStream fileOutputStream = new FileOutputStream(directory+"\\"+entryname);
        int n;
        while((n=zipInputStream.read(buf,0,1024))>-1){
            fileOutputStream.write(buf,0,n);
        }
        fileOutputStream.close();
        zipInputStream.closeEntry();
        return directory+"\\"+entryname;
    }
    private void validTest(String filename){
        chromeDriver.get(validUrl);
        chromeDriver.manage().timeouts().implicitlyWait(5,TimeUnit.SECONDS);
        chromeDriver.findElement(By.linkText("Проверка подписи (CMS)")).click();
        chromeDriver.manage().timeouts().implicitlyWait(2,TimeUnit.SECONDS);
        chromeDriver.findElement(By.id("firstParam")).sendKeys(new File(filename).getAbsolutePath());
        chromeDriver.manage().timeouts().implicitlyWait(10,TimeUnit.SECONDS);
//        chromeDriver.findElement(By.id("chkVerifySignatureOnly")).click();
        chromeDriver.findElement(By.name("btnVerify")).click();
        chromeDriver.manage().timeouts().implicitlyWait(15,TimeUnit.SECONDS);
        Assert.assertEquals(chromeDriver.findElement(By.id("resultDescription")).getText(),"Электронная подпись действительна");

    }

    @AfterClass
    private void closeDrivers() throws InterruptedException {
        driver.close();
        driver.quit();
        chromeDriver.close();
        chromeDriver.quit();
        Thread.sleep(2000);
        clearDirectory();
    }

    private static void clearDirectory(){
        File file = new File("data/docs");
        File[] files = file.listFiles();
        for(int i=0; i<files.length;i++){
            files[i].delete();
        }
    }

    private void testPictureOnSite() throws IOException, InterruptedException, NoSuchAlgorithmException {
        String s;
        geturl:while(true){
            s = driver.findElement(By.id("image-container")).getAttribute("src");
            if(s!=null){
                if (s.contains("http")){
                    break geturl;
                }
            }
        }
        URL url = new URL(s);
        BufferedImage bufImgOne = ImageIO.read(url);
        ImageIO.write(bufImgOne, "bmp", new File(downloadedFile));
        Thread.sleep(1000);
        compareImages();
    }
    private void compareImages() throws NoSuchAlgorithmException, IOException {
        final MessageDigest messageDigest1 = MessageDigest.getInstance("SHA-1");
        final FileInputStream fileInputStream1 = new FileInputStream(downloadedFile);
        byte[] data1 = new byte[1024];
        int n;
        while ((n=fileInputStream1.read(data1,0,1024))>0){
            messageDigest1.update(data1,0,n);
        }
        byte[] res1=messageDigest1.digest();
        MessageDigest messageDigest2 = MessageDigest.getInstance("SHA-1");
        FileInputStream fileInputStream2 = new FileInputStream(fileToCompare);
        byte[] data2 = new byte[1024];
        while((n=fileInputStream2.read(data2,0,1024))>0){
            messageDigest2.update(data2,0,n);
        }
        byte[] res2 = messageDigest2.digest();
        Assert.assertEquals(Hex.encodeHexString(res1),Hex.encodeHexString(res2));
    }

}



