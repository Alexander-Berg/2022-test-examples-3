package ru.yandex.market.core.testing;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import ru.yandex.market.core.annotations.ConverterClass;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.framework.converter.SmartStandartBeanElementConverter;


/**
 * @author mihanizm
 */
@ConverterClass(SmartStandartBeanElementConverter.class)
public class TestingDetails {

    private TestingInfo testingInfo;

    private Collection<CutoffInfo> cutoffs;

    public TestingDetails(TestingInfo testingInfo, Collection<CutoffInfo> cutoffs) {
        super();
        this.testingInfo = testingInfo;
        this.cutoffs = cutoffs;
    }

    public static Date getTestingStartDate(TestingInfo testingInfo) {
        if (testingInfo == null || testingInfo.getStartDate() == null || testingInfo.isFatalCancelled()) {
            return null;
        }
        Date now = new Date();
        if (testingInfo.getStartDate().before(now)) {
            if (testingInfo.isReady() || testingInfo.isInProgress()) {
                return testingInfo.getUpdatedAt() == null ? now : testingInfo.getUpdatedAt();
            } else {
                return now;
            }
        } else {
            return testingInfo.getStartDate();
        }
    }

    private static void skipWeekend(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
            case Calendar.MONDAY:
                calendar.add(Calendar.DATE, 2);
                break;
            default:
        }
    }

    public Collection<CutoffInfo> getCutoffs() {
        return cutoffs;
    }

    public Date getTestingStartDate() {
        return getTestingStartDate(testingInfo);
    }

    public TestingState getTestingInfo() {
        return testingInfo;
    }

}
