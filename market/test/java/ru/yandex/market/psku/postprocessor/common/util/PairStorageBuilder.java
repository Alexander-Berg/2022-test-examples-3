package ru.yandex.market.psku.postprocessor.common.util;

import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PairStorage;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class PairStorageBuilder {
    private long pskuId;
    private long mskuId;
    private PairState state;
    private PairType type;
    private Integer reportPosition;
    private Double reportMatchRate;
    private Boolean isValid;
    private Long sessionId;
    private Markup.PartnerMappingValidationTaskResult validationTaskResult;
    private Integer countPskuOnMsku;
    private String userLogin;

    public PairStorage build() {
        final PairStorage pairStorage = new PairStorage();

        pairStorage.setPskuId(pskuId);
        pairStorage.setMskuId(mskuId);
        pairStorage.setState(state);
        pairStorage.setIsValid(isValid);
        pairStorage.setType(type);
        pairStorage.setReportPosition(reportPosition);
        pairStorage.setReportMatchRate(reportMatchRate);
        pairStorage.setPskuCategoryId(pskuId * 100);
        pairStorage.setPskuSupplierId(pskuId * 100 + 100);
        pairStorage.setSessionId(sessionId);
        pairStorage.setMskuCategoryId(pskuId * 42);
        pairStorage.setValidationResult(validationTaskResult);
        pairStorage.setCountPskuOnMsku(countPskuOnMsku);
        pairStorage.setUserLogin(userLogin);

        return pairStorage;
    }

    public PairStorageBuilder pskuId(long pskuId) {
        this.pskuId = pskuId;
        return this;
    }

    public PairStorageBuilder mskuId(long mskuId) {
        this.mskuId = mskuId;
        return this;
    }

    public PairStorageBuilder state(PairState state) {
        this.state = state;
        return this;
    }

    public PairStorageBuilder type(PairType type) {
        this.type = type;
        return this;
    }

    public PairStorageBuilder reportPosition(Integer reportPosition) {
        this.reportPosition = reportPosition;
        return this;
    }

    public PairStorageBuilder reportMatchRate(Double reportMatchRate) {
        this.reportMatchRate = reportMatchRate;
        return this;
    }

    public PairStorageBuilder isValid(Boolean isValid) {
        this.isValid = isValid;
        return this;
    }

    public PairStorageBuilder sessionId(Long sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public PairStorageBuilder setValidationTaskResult(Markup.PartnerMappingValidationTaskResult validationTaskResult) {
        this.validationTaskResult = validationTaskResult;
        return this;
    }

    public PairStorageBuilder countPskuOnMsku(Integer countPskuOnMsku) {
        this.countPskuOnMsku = countPskuOnMsku;
        return this;
    }

    public PairStorageBuilder userLogin(String userLogin) {
        this.userLogin = userLogin;
        return this;
    }
}
