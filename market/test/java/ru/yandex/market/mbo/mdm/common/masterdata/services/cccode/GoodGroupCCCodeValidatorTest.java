package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MdmGoodGroupValidationService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class GoodGroupCCCodeValidatorTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmGoodGroupRepository repository;
    private MdmGoodGroupValidationService mdmGoodGroupValidationService;
    private GoodGroupCCCodeValidator goodGroupCCCodeValidator;

    @Before
    public void setUp() throws Exception {
        mdmGoodGroupValidationService = new MdmGoodGroupValidationService(repository);
        goodGroupCCCodeValidator = new GoodGroupCCCodeValidator(mdmGoodGroupValidationService);
    }

    @Test
    public void whenOperationDoNotSupport() {
        assertTrue(goodGroupCCCodeValidator.isOperationSupported(CCCodeValidator.Operation.CREATE));
        assertTrue(goodGroupCCCodeValidator.isOperationSupported(CCCodeValidator.Operation.UPDATE));
        assertFalse(goodGroupCCCodeValidator.isOperationSupported(CCCodeValidator.Operation.DELETE));
    }

    @Test
    public void whenGoodGroupDoNotExist() {
        CustomsCommCode customsCommCode = new CustomsCommCode();
        MdmGoodGroup group = repository.findAll().get(0);
        customsCommCode.setGoodGroupId(group.getId());
        assertTrue(goodGroupCCCodeValidator.validate(customsCommCode, customsCommCode).isEmpty());
        customsCommCode.setGoodGroupId(12345L);
        assertFalse(goodGroupCCCodeValidator.validate(customsCommCode, customsCommCode).isEmpty());
    }
}
