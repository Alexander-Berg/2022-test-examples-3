package ru.yandex.market.mbo.statistic;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;

/**
 * @author yuramalinov
 * @created 29.04.19
 */
public class YangLogStoreRequestHelper {
    private static final long DEFAULT_HID = 278342L; // ru/yandex/market/mbo/statistics/dao/market_content.sql
    private long nextId = 1;

    public YangLogStorage.YangLogStoreRequest.Builder newRequest() {
        long id = nextId++;

        return YangLogStorage.YangLogStoreRequest.newBuilder()
            .setId("request-id-" + id)
            .setCategoryId(DEFAULT_HID)
            .setHitmanId(id)
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS);
    }

    public YangLogStorage.OperatorInfo.Builder newOperatorInfo(long uid, double billingTotal) {
        long id = nextId++;

        return YangLogStorage.OperatorInfo.newBuilder()
            .setTaskId("task-id-" + id)
            .setPoolId("pool-id-" + id)
            .setAssignmentId("assignment-id-" + id)
            .setUid(uid)
            .setBillingTotal(billingTotal);
    }

    public YangLogStorage.ModelStatistic.Builder newModelStatistic() {
        long id = nextId++;
        return YangLogStorage.ModelStatistic.newBuilder()
            .setModelId(id)
            .setType(ModelStorage.ModelType.SKU)
            .setCreatedInTask(true);
    }

    public YangLogStorage.ActionCount.Builder newActionCount() {
        return YangLogStorage.ActionCount.newBuilder()
            .setAliases(0)
            .setBarCode(0)
            .setCutOffWord(0)
            .setIsSku(0)
            .setParam(0)
            .clearParamIds()
            .setPickerAdded(0)
            .setVendorCode(0)
            .setPictureUploaded(0)
            .setPictureCopied(0);
    }

    public YangLogStorage.MappingStatistic.Builder newMappingStatistic() {
        return YangLogStorage.MappingStatistic.newBuilder()
            .setMarketSkuId(nextId++)
            .setOfferId(nextId++)
            .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED);
    }
}
