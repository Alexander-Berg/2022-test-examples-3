package ru.yandex.market.pvz.core.test.factory.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.yandex.market.pvz.core.domain.banner_information.BannerInformationParams;
import ru.yandex.market.pvz.core.test.factory.TestBannerInformationFactory;

@Mapper(componentModel = "spring")
public interface BannerInformationTestParamsMapper {

    @Mapping(target = "bannerId", ignore = true)
    BannerInformationParams map(TestBannerInformationFactory.BannerInformationTestParams banner);

}
