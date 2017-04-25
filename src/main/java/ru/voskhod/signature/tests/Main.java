package ru.voskhod.signature.tests;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Created by a.chebotareva on 14.03.2017.
 */
public class Main {
    private Config config= new Config("config.properties");
    private static String login;
    private static String password;
    private static String url;
    private static String validUrl;
    private static String filename;
    private static String zipname;
    private static String fileToCompare;
    private static String downloadedFile;
    private Logger logger;

    private WebDriver chromeDriver;
    InternetExplorerDriver driver;
    WebDriverWait wait;

    public Main() throws IOException {

    }

    @BeforeClass
    private  void  init() throws IOException, TimeoutException {
        logger = Logger.getLogger(Main.class);
        logger.warn("Проверка ЕСЭП от "+new Date());
        url=config.get("url");
        validUrl=config.get("validUrl");
        filename=new File(config.get("fileToDownload")).getAbsolutePath();
        zipname=new File(config.get("zipname")).getAbsolutePath();
        fileToCompare = new File(config.get("fileToCompare")).getAbsolutePath();
        downloadedFile = new File(config.get("downloadedFile")).getAbsolutePath();
        login = new String(config.get("login").getBytes(),"windows-1252");
        password = new String(config.get("password").getBytes(), "windows-1252");
        clearDirectory();
        initIEDRiver();
        initChromeDriver();
    }

    private  void initIEDRiver() throws TimeoutException {
        DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, "dismiss");
        capabilities.setCapability(InternetExplorerDriver.INITIAL_BROWSER_URL, url);
        capabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
        capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        capabilities.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
        capabilities.setCapability("nativeEvents", false);
        capabilities.setCapability("unexpectedAlertBehaviour", "accept");
        capabilities.setCapability("ignoreProtectedModeSettings", true);
        capabilities.setCapability("disable-popup-blocking", true);
        capabilities.setCapability("enablePersistentHover", true);
        System.setProperty("webdriver.ie.driver", (new File("data/IEDriverServer.exe")).getAbsolutePath());
        driver = new InternetExplorerDriver(capabilities);
        wait = new WebDriverWait(driver, 20);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cph2_listFileAccessCodes")));
        } catch (org.openqa.selenium.TimeoutException e) {
            if(driver.findElements(By.id("shieldIcon")).size()>0){
                troublesWithCert();
            }else{
                throw new TimeoutException(e.getMessage());
            }
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
    public void mainDyn() throws InterruptedException, IOException, NoSuchAlgorithmException, TimeoutException {
        this.init();
        driver.manage().timeouts().implicitlyWait(5,TimeUnit.SECONDS);
        if(driver.findElement(By.id("cph2_lbESIAinfo")).getText().contains("Вы не авторизованы в ЕСИА")){
            authorization();
        }
        String oldHandle = driver.getWindowHandle();
//        driver.findElement(By.name("ctl00$cph2$fileUpload")).submit();
        driver.findElement(By.name("ctl00$cph2$fileUpload")).sendKeys(new File(filename).getAbsolutePath());

//        wait.until(ExpectedConditions.textToBePresentInElement(driver.findElement(By.name("ctl00$cph2$fileUpload")),new File(filename).getAbsolutePath()));
        driver.findElement(By.name("ctl00$cph2$buttonFileUpload")).click();
        driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"cph2_listFileAccessCodes\"]/option[1]")));
        driver.findElement(By.id("cph2_signDocuments")).click();
        wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        //переходим на новую страницу
        Set<String> windows = driver.getWindowHandles();
        windows.remove(oldHandle);
        driver.switchTo().window(windows.toArray()[0].toString());
        //ждем загрузки страницы с предпоказом
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("image-container")));
        } catch (org.openqa.selenium.TimeoutException e) {
            if(driver.findElements(By.id("shieldIcon")).size()>0){
                troublesWithCert();
            }else{
                throw new TimeoutException(e.getMessage());
            }
        }
        testPictureOnSite();
        driver.findElement(By.partialLinkText("Подписать")).click();
        driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
        try {
            driver.findElement(By.id("certificates-list")).findElements(By.xpath(".//*")).get(2).click();
        }catch (IndexOutOfBoundsException e){
            driver.navigate().back();
            driver.switchTo().window(oldHandle);

        }
        driver.findElement(By.className("wrapper")).findElement(By.partialLinkText("Подписать")).click();
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
        String entryName = entry.getName();
        File file = new File(filepath);
        String directory = file.getParent();
        FileOutputStream fileOutputStream = new FileOutputStream(directory+"\\"+entryName);
        int n;
        while((n=zipInputStream.read(buf,0,1024))>-1){
            fileOutputStream.write(buf,0,n);
        }
        fileOutputStream.close();
        zipInputStream.closeEntry();
        return directory+"\\"+entryName;
    }
    private void validTest(String filename){
        chromeDriver.get(validUrl);
        chromeDriver.manage().timeouts().implicitlyWait(5,TimeUnit.SECONDS);
        chromeDriver.findElement(By.linkText("Проверка подписи (CMS)")).click();
        chromeDriver.manage().timeouts().implicitlyWait(2,TimeUnit.SECONDS);
        chromeDriver.findElement(By.id("firstParam")).sendKeys(new File(filename).getAbsolutePath());
        chromeDriver.manage().timeouts().implicitlyWait(10,TimeUnit.SECONDS);
        chromeDriver.findElement(By.name("btnVerify")).click();
        chromeDriver.manage().timeouts().implicitlyWait(15,TimeUnit.SECONDS);
        Assert.assertEquals(chromeDriver.findElement(By.id("resultDescription")).getText(),"Электронная подпись действительна");

    }

    @AfterClass
    private void closeDrivers() throws InterruptedException {
        if (driver!= null) {
            Thread.sleep(3000);
            driver.quit();
        }
        if(chromeDriver!=null) {
            Thread.sleep(3000);
            chromeDriver.quit();
        }
        clearDirectory();
    }

    private static void clearDirectory(){
        File file = new File("data/docs");
        File[] files = file.listFiles();
        if(files!=null) {
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    /**
     * Скачивание изображения со страницы с превью
     * @throws IOException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     */
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

    /**
     * Сравнение изображения в текущем превью и загруженного ранее из эталонного превью изображения с помощью хэш суммыю
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
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

    /**
     * Авторизация в ЕСИА с помощью прописанных в config.properties логина и пароля
     * @throws TimeoutException драйвер не смог перейти на новую страницу
     */
    private void authorization() throws TimeoutException, UnsupportedEncodingException {
        driver.findElement(By.id("cph2_btnESIA")).sendKeys();
        driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
        driver.findElement(By.id("cph2_btnESIA")).submit();
        driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);

        if(driver.findElements(By.className("another-user")).size()!=0){
            driver.findElement(By.className("another-user")).click();
            driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mobileOrEmail")));
        driver.findElement(By.id("mobileOrEmail")).clear();
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("mobileOrEmail")).sendKeys(new String(login.getBytes(),"ascii"));
        driver.findElement(By.xpath("//*[@id=\"authnFrm\"]/div[1]/div[1]/div[3]/button")).click();
//
//        driver.findElement(By.id("mobileOrEmail")).clear();
//        driver.findElement(By.id("mobileOrEmail")).sendKeys(new String(login.getBytes(),"UTF-8"));
//        driver.findElement(By.xpath("//*[@id=\"authnFrm\"]/div[1]/div[1]/div[3]/button")).click();
//        driver.findElement(By.id("mobileOrEmail")).clear();
//
//        driver.findElement(By.id("mobileOrEmail")).sendKeys(new String(login.getBytes(),"windows-1252"));
//        driver.findElement(By.xpath("//*[@id=\"authnFrm\"]/div[1]/div[1]/div[3]/button")).click();
//        driver.findElement(By.id("mobileOrEmail")).clear();
//
//        driver.findElement(By.id("mobileOrEmail")).sendKeys(new String(login.getBytes(),"windows-1251"));
//        driver.findElement(By.xpath("//*[@id=\"authnFrm\"]/div[1]/div[1]/div[3]/button")).click();
//
//        driver.findElement(By.xpath("//*[@id=\"authnFrm\"]/div[1]/div[1]/div[3]/button")).click();
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cph2_listFileAccessCodes")));
        } catch (org.openqa.selenium.TimeoutException e) {
            if(driver.findElements(By.id("shieldIcon")).size()>0){
                troublesWithCert();
            }else{
                throw new TimeoutException(e.getMessage());
            }
        }


    }

    /**
     * Метод для работы с тестовым стендом, который на данный момент имеет проблемы с сертификатом
     */
    private void troublesWithCert(){
        String currentURL = driver.getCurrentUrl();
//        System.out.println(currentURL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("shieldIcon")));
        driver.findElement(By.id("overridelink")).click();
        driver.manage().timeouts().implicitlyWait(20,TimeUnit.SECONDS);
        if(driver.getCurrentUrl().equalsIgnoreCase(currentURL)){
            System.out.println("driver has problems with "+driver.getCurrentUrl());
        }
    }

    public static void main(String[] args) throws InterruptedException, TimeoutException, NoSuchAlgorithmException, IOException {
        Main main = new Main();
        try {
            main.mainDyn();
            main.logger.warn("Проверка прошла успешно");
        }catch(Exception e){
            main.logger.error("Проверка провалена. Причина:");
            main.logger.error(e.getMessage());
            main.logger.error("Ошибка произошла на странице "+main.driver.getCurrentUrl());
        }catch (AssertionError e1){
            main.logger.warn("Проверка провалена. Причина:");
            main.logger.error(e1.getMessage());
            main.logger.error("Ошибка произошла на странице "+main.driver.getCurrentUrl());
        }finally {
            main.closeDrivers();
            clearDirectory();
        }
    }


}



