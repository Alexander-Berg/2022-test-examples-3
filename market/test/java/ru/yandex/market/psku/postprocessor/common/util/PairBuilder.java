package ru.yandex.market.psku.postprocessor.common.util;

import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Pair;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class PairBuilder {
    private Long pskuId;
    private Long mskuId;
    private PairType type;
    private Integer reportPosition;
    private Double reportMatchRate;
    private Long sessionId;
    private Integer countPskuOnMsku;

    public Pair build() {
        return new Pair(pskuId,
                mskuId,
                type,
                reportPosition,
                reportMatchRate,
                pskuId * 100,
                pskuId * 100 + 100,
                sessionId,
                pskuId * 42,
                countPskuOnMsku);
    }

    public PairBuilder pskuId(long pskuId) {
        this.pskuId = pskuId;
        return this;
    }

    public PairBuilder mskuId(long mskuId) {
        this.mskuId = mskuId;
        return this;
    }

    public PairBuilder type(PairType type) {
        this.type = type;
        return this;
    }

    public PairBuilder reportPosition(int reportPosition) {
        this.reportPosition = reportPosition;
        return this;
    }

    public PairBuilder reportMatchRate(double reportMatchRate) {
        this.reportMatchRate = reportMatchRate;
        return this;
    }

    public PairBuilder sessionId(Long sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public PairBuilder countPskuOnMsku(Integer countPskuOnMsku) {
        this.countPskuOnMsku = countPskuOnMsku;
        return this;
    }
}
