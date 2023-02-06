package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.InvalidStateException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.qatools.properties.Property;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.reporter.ReportsTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;

public class ReportHistoryTabPage<T> extends AbstractPage {
    private static final Logger log = LoggerFactory.getLogger(ReportsTest.class);
    protected static final String HISTORY_BUTTON_XPATH = "//*[@data-e2e='history_tab']";

    @Property("webdriver.downloadurl")
    private String downloadUrl;

    @Property("webdriver.downloadlocalfolder")
    private String downloadLocalFolder;

    @FindBy(xpath = "//a[@data-e2e='link_PDF']")
    private SelenideElement topReportPdfBtn;

    @FindBy(xpath = "//a[@data-e2e='link_XLS']")
    private SelenideElement topReportXlsxBtn;

    @FindBy(xpath = "//button[@data-e2e='button_submit']")
    protected SelenideElement runReportBtn;

    @FindBy(xpath = HISTORY_BUTTON_XPATH)
    private SelenideElement historyTab;

    @FindBy(xpath = "//*[@data-e2e='createReport_tab']")
    private SelenideElement runReportTab;

    @FindBy(xpath = "//span[@data-e2e='data_field_ID отчёта']")
    private SelenideElement reportIdText;

    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement closePopUpButton;

    @FindBy(xpath = "//span[@data-e2e='report_status']")
    private SelenideElement reportGenerationStatus;

    public ReportHistoryTabPage(WebDriver driver) {
        super(driver);
    }

    public T openHistoryTab() {
        try {
            log.info("Opening History Tab");
            historyTab.click();
        } catch (InvalidStateException e) {
            log.info("Waiting for popup to disappear");
            closeReportFinishedPopUp();
            log.info("Opening History Tab");
            historyTab.click();
        }
        return (T)this;
    }

    public T openRunReportTab() {
        runReportTab.click();
        return (T)this;
    }

    public int getTopReportId() {
        // It's possible that there are no reports on the History tab page
        try {
            return Integer.parseInt(reportIdText.getText());
        } catch (ElementNotFound e) {
            return 0;
        }
    }

    public void closeReportFinishedPopUp() {
        closePopUpButton.click();
    }

    public T waitPrintNotification() {
        if (notificationDialog.IsPresentWithMessage("Запущена подготовка отчета")) {
            notificationDialog.waitUntilHidden();
        }
        return (T)this;
    }

    public T waitTopReportIsReady(int reportId) {
        return waitTopReportIsReady(20, reportId);
    }

    public T waitTopReportIsReady(int timeOutSeconds, int reportId) {
        WebDriverWait myWait = new WebDriverWait(driver, timeOutSeconds, 2000);
        log.info("Waiting for the new report Ready state");
        ExpectedCondition<Boolean> conditionToCheck = inputDriver -> {
            log.info("Reopening History tab and check top report state");
            inputDriver.navigate().refresh();
            historyTab.click();
            int lastReportId = getTopReportId();
            return (reportGenerationStatus.getText().equals("Готов") && lastReportId > reportId);
        };
        myWait.until(conditionToCheck);
        return (T)this;
    }


    private void waitForFilePresent(String filePath) {
        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                File f = new File(filePath);
                return f.exists();
            }

            @Override
            public String toString() {
                return String.format("file to be present within the time specified");
            }
        };
        WebDriverWait wait = new WebDriverWait(
                driver,
                15,
                WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS
        );
        wait.until(condition);
    }

    private void waitForOpenStream(URL fileUrl) {
        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    fileUrl.openStream();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public String toString() {
                return "file to be present within the time specified";
            }
        };
        WebDriverWait wait = new WebDriverWait(
                driver,
                WebDriverTimeout.MEDIUM_WAIT_TIMEOUT,
                WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS
        );
        wait.until(condition);
    }

    private InputStream getDownloadedFile(String fileName) throws IOException {
        // if a local run
        if (driver instanceof ChromeDriver) {
            String filePath = String.format("%s/%s", downloadLocalFolder, fileName);
            waitForFilePresent(filePath);
            File initialFile = new File(filePath);
            return new FileInputStream(initialFile);
        }
        // if a usergrid run
        String sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
        URL fileUrl = new URL(String.format("%s%s/%s", downloadUrl, sessionId, fileName));
        waitForOpenStream(fileUrl);
        return fileUrl.openStream();
    }

    public String openPdfReport(String fileName) {
        topReportPdfBtn.click();
        String pdfContent = "";
        try {
            InputStream pdfFileStream = getDownloadedFile(fileName);
            PDDocument document = PDDocument.load(pdfFileStream);
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                pdfContent = stripper.getText(document);
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pdfContent;
    }

    public String openXlsxReport(String fileName) {
        topReportXlsxBtn.click();
        StringBuilder xlsxContent = new StringBuilder();
        try {
            InputStream xlsxFileStream = getDownloadedFile(fileName);
            XSSFWorkbook wb = new XSSFWorkbook(xlsxFileStream);
            XSSFSheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (cell.getCellType() == CELL_TYPE_NUMERIC) {
                        xlsxContent.append(cell.getNumericCellValue());
                    } else if (cell.getCellType() == CELL_TYPE_STRING) {
                        xlsxContent.append(cell.getStringCellValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xlsxContent.toString();
    }
}
