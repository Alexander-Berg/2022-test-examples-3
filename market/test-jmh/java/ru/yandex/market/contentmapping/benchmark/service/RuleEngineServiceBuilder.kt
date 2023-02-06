package ru.yandex.market.contentmapping.benchmark.service

import ru.yandex.market.contentmapping.benchmark.mock.CategoryParameterInfoServiceMock
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterInfoService
import ru.yandex.market.contentmapping.services.rules.v2.MappingApplyer
import ru.yandex.market.contentmapping.services.rules.v2.RuleApplyResultMerger
import ru.yandex.market.contentmapping.services.rules.v2.RuleEngineService
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingForceMappingStrategy
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingDirectStrategy
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingMappingStrategy
import ru.yandex.market.contentmapping.services.rules.v2.stratategy.MappingPictureStrategy

class RuleEngineServiceBuilder {
    companion object {
        fun buildRuleEngineService(data: Map<Long, Map<Long, CategoryParameterInfo>>): RuleEngineService {
            return RuleEngineService(
                    buildMappingApplyer(data),
                    buildRuleApplyMerger()
            )
        }

        fun buildMappingApplyer(data: Map<Long, Map<Long, CategoryParameterInfo>>): MappingApplyer {
            return MappingApplyer(
                    buildCategoryParameterInfoService(data),
                    mapOf(
                            ParamMappingType.MAPPING to MappingMappingStrategy(),
                            ParamMappingType.DIRECT to MappingDirectStrategy(),
                            ParamMappingType.FORCE_MAPPING to MappingForceMappingStrategy(),
                            ParamMappingType.FIRST_PICTURE to MappingPictureStrategy(true),
                            ParamMappingType.PICTURE to MappingPictureStrategy(false),
                    )
            )
        }

        fun buildRuleApplyMerger(): RuleApplyResultMerger {
            return RuleApplyResultMerger()
        }

        fun buildCategoryParameterInfoService(data: Map<Long, Map<Long, CategoryParameterInfo>>): CategoryParameterInfoService {
            return CategoryParameterInfoServiceMock(data)
        }
    }
}
