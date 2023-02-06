package ru.yandex.market.pvz.core.test.factory.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ru.yandex.market.pvz.core.domain.survey.SurveyParams;
import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SurveyTestParamsMapper {

    @Mapping(target = "id", ignore = true)
    SurveyParams map(TestSurveyFactory.SurveyTestParams surveyTestParams);

}
