package ru.yandex.market.mbo.mdm.common.service;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ValidationContext;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class AllOkMasterDataValidator extends MasterDataValidator {

    public AllOkMasterDataValidator() {
        super(null, null, null, null, null, null);
    }

    @Override
    public ArrayList<ErrorInfo> validateServicePart(MasterData masterData,
                                                    ValidationContext validationContext) {
        return new ArrayList<>();
    }

    @Override
    public List<ErrorInfo> validateBusinessPart(MasterData masterData, ValidationContext validationContext) {
        return List.of();
    }

    @Override
    public MasterData filterServicePart(MasterData masterData) {
        return masterData;
    }

    @Override
    public List<ErrorInfo> validateMasterData(MasterData masterData, ValidationContext validationContext) {
        return List.of();
    }

    @Override
    public MasterData filterMasterData(MasterData masterData, ValidationContext validationContext) {
        return masterData;
    }

    @Override
    public List<ErrorInfo> validateMasterData(MasterData masterData) {
        return List.of();
    }
}
