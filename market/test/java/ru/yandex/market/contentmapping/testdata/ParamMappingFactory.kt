package ru.yandex.market.contentmapping.testdata

import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.dto.mapping.*
import java.util.ArrayList
import java.util.function.Function

class ParamMappingFactory private constructor(private val paramMapping: ParamMapping) {
    private val rules: MutableList<ParamMappingRule> = ArrayList()

    fun mapping(): ParamMapping {
        return paramMapping
    }

    @JvmOverloads
    fun rule(
            shopValue: String, marketValue: Set<MarketParamValue>,
            ruleCustomizer: Function<ParamMappingRule, ParamMappingRule>? = null,
    ): ParamMappingRule {
        var rule = ParamMappingRule(
                id = nextId++,
                paramMappingId = paramMapping.id,
                shopValues = mapOf(paramMapping.shopParams[0].name to shopValue),
                marketValues = mapOf(paramMapping.marketParams[0].parameterId to marketValue),
        )
        if (ruleCustomizer != null) {
            rule = ruleCustomizer.apply(rule)
        }
        rules.add(rule)
        return rule
    }

    fun mappingWithRules(): ParamMappingWithRules {
        return ParamMappingWithRules(paramMapping, rules)
    }

    companion object {
        private var nextId = 10000

        @JvmOverloads
        fun map(param: CategoryParameterInfo, shopParam: String, split: String? = null): ParamMappingFactory {
            return ParamMappingFactory(ParamMapping(
                    id = nextId++,
                    marketParams = listOf(MarketParam(param.parameterId)),
                    shopParams = listOf(ShopParam(shopParam, split)),
            ))
        }
    }
}
