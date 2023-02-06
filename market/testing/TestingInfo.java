package ru.yandex.market.core.testing;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import ru.yandex.market.core.annotations.ConverterClass;
import ru.yandex.market.core.framework.converter.SmartStandartBeanElementConverter;

/**
 * @author ashevenkov
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
@ConverterClass(SmartStandartBeanElementConverter.class)
public class TestingInfo extends TestingState {
    private List<TestingParamStatus> testingParamStatus;
    private int attemptsLeft;

    public TestingInfo(TestingState testingState, List<TestingParamStatus> testingParamStatus, int attemptsLeft) {
        super(testingState);
        this.testingParamStatus = testingParamStatus;
        this.attemptsLeft = attemptsLeft;
    }

    public List<TestingParamStatus> getTestingParamStatus() {
        return testingParamStatus;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    @Override
    public String toString() {
        return "TestingInfo{" +
                "testingParamStatus=" + testingParamStatus +
                ", attemptsLeft=" + attemptsLeft +
                "} " + super.toString();
    }
}
