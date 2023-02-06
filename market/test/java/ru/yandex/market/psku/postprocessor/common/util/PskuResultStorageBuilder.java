package ru.yandex.market.psku.postprocessor.common.util;

import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;

import java.sql.Timestamp;
import java.time.Instant;

public class PskuResultStorageBuilder {
    private long             pskuId;
    private long             categoryId = 0;
    private ProcessingResult clusterizerProcessingResult;
    private ProcessingResult mappingCheckerProcessingResult;
    private Timestamp        createTime = Timestamp.from(Instant.now());
    private Long             mskuMappedId;
    private PskuStorageState state;
    private String           errorKinds;
    private Long             pmodelIdForDelete;


    public PskuResultStorage build() {
        PskuResultStorage pskuResultStorage = new PskuResultStorage();

        pskuResultStorage.setPskuId(pskuId);
        pskuResultStorage.setCategoryId(categoryId);
        pskuResultStorage.setClusterizerProcessingResult(clusterizerProcessingResult);
        pskuResultStorage.setMappingCheckerProcessingResult(mappingCheckerProcessingResult);
        pskuResultStorage.setCreateTime(createTime);
        pskuResultStorage.setMskuMappedId(mskuMappedId);
        pskuResultStorage.setState(state);
        pskuResultStorage.setErrorKinds(errorKinds);
        pskuResultStorage.setPmodelIdForDelete(pmodelIdForDelete);

        return pskuResultStorage;
    }

    public PskuResultStorageBuilder setPskuId(long pskuId) {
        this.pskuId = pskuId;
        return this;
    }

    public PskuResultStorageBuilder setCategoryId(long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public PskuResultStorageBuilder setClusterizerProcessingResult(ProcessingResult clusterizerProcessingResult) {
        this.clusterizerProcessingResult = clusterizerProcessingResult;
        return this;
    }

    public PskuResultStorageBuilder setMappingCheckerProcessingResult(ProcessingResult mappingCheckerProcessingResult) {
        this.mappingCheckerProcessingResult = mappingCheckerProcessingResult;
        return this;
    }

    public PskuResultStorageBuilder setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
        return this;
    }

    public PskuResultStorageBuilder setMskuMappedId(long mskuMappedId) {
        this.mskuMappedId = mskuMappedId;
        return this;
    }

    public PskuResultStorageBuilder setState(PskuStorageState state) {
        this.state = state;
        return this;
    }

    public PskuResultStorageBuilder setErrorKinds(String errorKinds) {
        this.errorKinds = errorKinds;
        return this;
    }

    public PskuResultStorageBuilder setPmodelIdForDelete(Long pmodelIdForDelete) {
        this.pmodelIdForDelete = pmodelIdForDelete;
        return this;
    }
}
