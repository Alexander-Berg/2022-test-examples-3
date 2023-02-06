package ru.yandex.autotests.search.meta.data;

import org.apache.commons.lang.exception.ExceptionUtils;
import ru.yandex.autotests.search.meta.checkers.Inspection;
import ru.yandex.autotests.search.meta.report.Status;
import ru.yandex.autotests.search.meta.report.TestCase;
import ru.yandex.autotests.search.meta.utils.Browser;
import ru.yandex.autotests.search.meta.utils.RetryException;

import static ru.yandex.autotests.search.meta.checkers.Inspection.BETA;
import static ru.yandex.autotests.search.meta.checkers.Inspection.PROD;

/**
 * @author Ksenia Mamich fenice@yandex-team.ru
 *         Date: 29.05.13
 */
public class TestScenario {

    private final Request request;
    private final Inspection inspection;

    public TestScenario(Request request, Inspection inspection) {
        this.request = request;
        this.inspection = inspection;
    }

    private void openPages(Browser browser) {
        browser.beta().get(request.getBetaUrl());
        browser.prod().get(request.getProdUrl());
    }

    public void perform(Browser browser, ReportHandler report) {
        TestCase tc;
        try {
            openPages(browser);

            boolean needToCheckAgain = inspection.isRefreshable();
            do {
                tc = ReportHandler.createTestCase(request.toString(), request.getProdUrl(), request.getBetaUrl());

                    inspection.process(browser.prod(), PROD);
                    inspection.process(browser.beta(), BETA);
                    String result = inspection.check(browser);
                    tc.setReason(result);

                    if (result != null) {
                        if (inspection.isRefreshable() && inspection.getChecksCount() < 3) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {
                            }
                            openPages(browser);
                            continue;
                        }

                        tc.setStatus(Status.FAIL);
                        tc.setBetaImg(ReportCreator.saveImage(browser.takeBetaScreenshot()));
                        tc.setProdImg(ReportCreator.saveImage(browser.takeProdScreenshot()));
                    } else {
                        tc.setStatus(Status.OK);
                    }

                needToCheckAgain = false;
            } while (needToCheckAgain);

            report.addTestCase(inspection.toString(), tc, request.getService());
        } catch (Throwable t) {
            tc = ReportHandler.createTestCase(request.toString(), request.getProdUrl(), request.getBetaUrl());
            tc.setStatus(Status.ERROR);
            tc.setReason(ExceptionUtils.getStackTrace(t));
            throw new RetryException(inspection.toString(), tc, request.getService(), t);
        }
    }

    public Request getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return String.format("Request [%s] with inspection %s", request, inspection);
    }

}
