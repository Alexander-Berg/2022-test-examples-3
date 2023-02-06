package ru.yandex.market.pricingmgmt.api.dictionary

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.mechanics.PromocodeType
import ru.yandex.mj.generated.server.model.CategoryDto
import ru.yandex.mj.generated.server.model.ChannelDto
import ru.yandex.mj.generated.server.model.ChannelPeriodDto
import ru.yandex.mj.generated.server.model.DictionaryItemDto
import ru.yandex.mj.generated.server.model.ErrorResponse
import ru.yandex.mj.generated.server.model.PromotionCatteamDto
import ru.yandex.mj.generated.server.model.PromotionDto
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory", value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class DictionaryApiServiceTest : ControllerTest() {
    @Test
    @DbUnitDataSet(
        after = ["DictionaryApiServiceTest.uploadPromotionDictionary.after.csv"]
    )
    fun uploadPromotionDictionary() {
        uploadDictionary("/xlsx-template/promotion-dictionary.xlsx").andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["DictionaryApiServiceTest.uploadOverExistingDict.before.csv"],
        after = ["DictionaryApiServiceTest.uploadOverExistingDict.after.csv"]
    )
    fun uploadPromotionDictionaryOverExistingDict() {
        uploadDictionary("/xlsx-template/promotion-dictionary-over-existing.xlsx").andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun uploadPromotionDictionaryWithWrongPiChannel() {
        uploadDictionary("/xlsx-template/promotion-dictionary-wrong-pi-channel.xlsx").andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["DictionaryApiServiceTest.getPromotionDictionary.before.csv"]
    )
    fun getPromotionDictionary() {
        val expectedResult = PromotionDto().catteams(
            listOf(
                PromotionCatteamDto().name("DiY").categories(
                    listOf(
                        CategoryDto().name("Внешнее продвижение").channels(
                            listOf(ChannelDto().name("Mobile Performance").period(null))
                        ), CategoryDto().name("Контентное присутствие").channels(
                            listOf(
                                ChannelDto().name("Landing page").budgetPlan(150000L).period(null),
                                ChannelDto().name("Бренд-зона (тариф органика)").budgetPlan(80000)
                                    .period(ChannelPeriodDto().minCount(1).maxCount(12).unit("мес"))
                            )
                        )
                    )
                ), PromotionCatteamDto().name("FMCG").categories(
                    listOf(
                        CategoryDto().name("Медийное размещение Главная").channels(
                            listOf(
                                ChannelDto().name("Главная страница. Hero-баннер 400 тыс. показов").budgetPlan(480000L)
                                    .period(ChannelPeriodDto().minCount(1).maxCount(7).unit("нед"))
                            )
                        )
                    )
                ), PromotionCatteamDto().name("ЭиБТ").categories(
                    listOf(
                        CategoryDto().name("Прочее").channels(
                            listOf(ChannelDto().name("Прочее").period(null))
                        ), CategoryDto().name("Контентное присутствие").channels(
                            listOf(
                                ChannelDto().name("Триггерные коммуникации: напоминание о частотных товарах (1-10 MSKU)")
                                    .budgetPlan(100000L).period(null)
                            )
                        )
                    )
                )
            )
        )


        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/promotion")).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResult)))
    }

    private fun uploadDictionary(filename: String): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/promotion/upload").file(file))
    }


    private fun <ENUM> getDictionarySuccess(
        path: String,
        items: Array<ENUM>,
        unknown: ENUM,
        getCode: (ENUM) -> Int,
        getName: (ENUM) -> String,
        getDisplayName: (ENUM) -> String
    ) {
        val expectedResponse = items
            .filter { f -> f != unknown }
            .map {
                DictionaryItemDto().order(getCode.invoke(it)).value(getName.invoke(it))
                    .displayName(getDisplayName.invoke(it))
            }

        mockMvc.perform(MockMvcRequestBuilders.get(path))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponse)))
    }

    @Test
    fun getAssortmentLoadMethodsSuccess() {
        getDictionarySuccess(
            "/api/v1/assortmentLoadMethods",
            AssortmentLoadMethod.values(),
            AssortmentLoadMethod.UNKNOWN,
            { item -> item.code },
            { item -> item.name },
            { item -> item.displayName }
        )
    }

    @Test
    fun getPromocodeTypesSuccess() {
        getDictionarySuccess(
            "/api/v1/promocodeTypes",
            PromocodeType.values(),
            PromocodeType.UNKNOWN,
            { item -> item.code },
            { item -> item.name },
            { item -> item.displayName }
        )
    }

    @Test
    fun getPromoKindsSuccess() {
        getDictionarySuccess(
            "/api/v1/promo-kinds",
            PromoKind.values(),
            PromoKind.UNKNOWN,
            { item -> item.code },
            { item -> item.name },
            { item -> item.displayName }
        )
    }

    private fun getPromoKindsAllowedSuccessArguments(): Stream<Arguments> {
        val all = PromoKind.values().filter { it != PromoKind.UNKNOWN }.toSet()

        return Stream.of(
            Arguments.of(AssortmentLoadMethod.TRACKER.name, all),
            Arguments.of(AssortmentLoadMethod.LOYALTY.name, all),
            Arguments.of(
                AssortmentLoadMethod.PI.name,
                setOf(PromoKind.NATIONAL, PromoKind.CROSS_CATEGORY, PromoKind.CATEGORY)
            )
        )
    }

    @ParameterizedTest
    @MethodSource("getPromoKindsAllowedSuccessArguments")
    fun getPromoKindsAllowedSuccess(assortmentLoadMethod: String?, expectedPromoKinds: Set<PromoKind>) {
        val expectedResponse = expectedPromoKinds
            .map {
                DictionaryItemDto()
                    .order(it.code)
                    .value(it.name)
                    .displayName(it.displayName)
            }

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/api/v1/promo-kinds-allowed")
                .param("assortmentLoadMethod", assortmentLoadMethod)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponse)))
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["INCORRECT"])
    fun getPromoKindsAllowedError(assortmentLoadMethod: String?) {
        val expectedResponse = ErrorResponse()
            .errorCode("PROMO_ASSORTMENT_LOAD_METHOD_UNKNOWN")
            .message("Способ загрузки ассортимента \"$assortmentLoadMethod\" не распознан")
            .errorFields(listOf("assortmentLoadMethod"))

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/api/v1/promo-kinds-allowed")
                .param("assortmentLoadMethod", assortmentLoadMethod)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponse)))
    }

    @Test
    fun getPromoKindsAllowedOnNullError() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/api/v1/promo-kinds-allowed")
                .param("assortmentLoadMethod", null)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string(""))
    }
}
