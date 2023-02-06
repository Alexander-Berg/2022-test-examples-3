package ru.yandex.market.core.moderation.qc.result;

import ru.yandex.market.core.testing.TestingType;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class LiteCheckResultRequestImpl implements LiteCheckResult {

    private long shopId;
    private TestingType testingType;
    private Status status;
    private Message message;

    public LiteCheckResultRequestImpl(long shopId, TestingType testingType, Status status, Message message) {
        this.shopId = shopId;
        this.testingType = testingType;
        this.status = status;
        this.message = message;
    }

    @Override
    public long getShopId() {
        return shopId;
    }

    @Override
    public TestingType getTestingType() {
        return testingType;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Message getMessage() {
        return message;
    }
}
