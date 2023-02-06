package ru.yandex.market.mdm.app.controller.dtos;

import java.util.Random;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;

public class SilverItemDtoConverterTest {

    @Test
    public void test() throws Exception {
        Random random = new Random("me".hashCode());
        MdmParamCacheMock paramCacheMock = TestMdmParamUtils.createParamCacheMock();
        SilverCommonSsku silver = new SilverCommonSsku(new SilverSskuKey(123, "123", MasterDataSourceType.MDM_ADMIN,
            "albina-gima"));
        Stream.concat(
                KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream(),
                Stream.of(KnownMdmParams.SHELF_LIFE, KnownMdmParams.SHELF_LIFE_UNIT))
            .map(paramCacheMock::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(silver::addBaseValue);
        SilverItemDto dto = SilverItemDtoConverter.fromSilverCommonSsku(silver);
        System.out.println(new ObjectMapper().registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).writeValueAsString(dto));
    }
}
