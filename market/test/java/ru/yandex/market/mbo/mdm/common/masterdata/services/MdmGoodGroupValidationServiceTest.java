package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.MdmGoodGroupValidationError.Type.EMPTY_GROUP_NAME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.MdmGoodGroupValidationError.Type.NO_SUCH_GOOD_GROUP;

public class MdmGoodGroupValidationServiceTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmGoodGroupRepository repository;
    private MdmGoodGroupValidationService mdmGoodGroupValidationService;

    @Before
    public void setUp() throws Exception {
        mdmGoodGroupValidationService = new MdmGoodGroupValidationService(repository);
    }

    @Test
    public void whenGroupNameIsInvalid() {
        MdmGoodGroup mdmGoodGroup = new MdmGoodGroup();
        List<MdmGoodGroupValidationError> validationErrors = mdmGoodGroupValidationService.validateCreate(mdmGoodGroup);
        assertEquals(validationErrors.size(), 1);
        assertEquals(EMPTY_GROUP_NAME, validationErrors.get(0).getType());

        mdmGoodGroup.setGroupName("");
        validationErrors = mdmGoodGroupValidationService.validateCreate(mdmGoodGroup);
        assertEquals(1, validationErrors.size());
        assertEquals(EMPTY_GROUP_NAME, validationErrors.get(0).getType());
    }

    @Test
    public void whenThereIsNoGoodGroup() {
        long mdmGoodGroupId = 123456L;
        List<MdmGoodGroupValidationError> validationErrors = mdmGoodGroupValidationService
            .validateExistence(mdmGoodGroupId);
        assertEquals(1, validationErrors.size());
        assertEquals(NO_SUCH_GOOD_GROUP, validationErrors.get(0).getType());
    }

    @Test
    public void whenCreateAndCheckValidGoodGroup() {
        MdmGoodGroup mdmGoodGroup = new MdmGoodGroup();
        mdmGoodGroup.setGroupName("some test group name");
        List<MdmGoodGroupValidationError> validationErrors = mdmGoodGroupValidationService.validateCreate(mdmGoodGroup);
        assertEquals(0, validationErrors.size());
        validationErrors = mdmGoodGroupValidationService.validateCreate(mdmGoodGroup);
        assertEquals(0, validationErrors.size());
    }
}
