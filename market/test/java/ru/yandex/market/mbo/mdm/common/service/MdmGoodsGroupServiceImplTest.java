package ru.yandex.market.mbo.mdm.common.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.MdmGoodsGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.junit.Assert.assertEquals;

public class MdmGoodsGroupServiceImplTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    private MdmGoodsGroupServiceImpl mdmGoodsGroupService;

    @Before
    public void setUpMdmGoodsGroupService() {
        mdmGoodsGroupService = new MdmGoodsGroupServiceImpl(mdmGoodGroupRepository);
    }

    @Test
    public void getAllGoodsGroups() {
        MdmGoodsGroup.GetAllGoodsGroupsResponse protoResponse = mdmGoodsGroupService.getAllGoodsGroups(
            MdmGoodsGroup.GetAllGoodsGroupsRequest.newBuilder().build());

        assertEquals(protoResponse.getGoodsGroupCount(), 16);
    }
}
