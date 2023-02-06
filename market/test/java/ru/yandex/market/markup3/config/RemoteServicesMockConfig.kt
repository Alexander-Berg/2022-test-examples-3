package ru.yandex.market.markup3.config

import com.fasterxml.jackson.core.type.TypeReference
import com.googlecode.protobuf.format.JsonFormat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.market.aliasmaker.AliasMaker
import ru.yandex.market.aliasmaker.AliasMakerService
import ru.yandex.market.markup3.core.TolokaTask
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.mocks.MboMappingsServiceMock
import ru.yandex.market.markup3.remote.HintsReader
import ru.yandex.market.markup3.remote.HoneypotsReader
import ru.yandex.market.markup3.remote.YtHintsReader
import ru.yandex.market.markup3.remote.YtHoneypotsReader
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.mbo.export.CategoryModelsService
import ru.yandex.market.mbo.export.CategoryParametersService
import ru.yandex.market.mbo.export.MboParameters
import ru.yandex.market.mbo.http.ModelStorageService
import ru.yandex.market.mbo.http.YangLogStorage
import ru.yandex.market.mbo.http.YangLogStorageService
import ru.yandex.market.mbo.model.forms.ModelFormService
import ru.yandex.market.mbo.model.forms.ModelForms
import ru.yandex.market.mbo.users.MboUsersService
import ru.yandex.market.mboc.http.MboCategoryService
import ru.yandex.market.mboc.http.MboMappingsService
import java.io.InputStreamReader

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
@Profile("test")
@Configuration
open class RemoteServicesMockConfig : RemoteServicesConfig {
    @Bean
    override fun mboCategoryService(): MboCategoryService {
        return MboCategoryServiceMock()
    }

    @Bean
    override fun mboUserService(): MboUsersService {
        return mock()
    }

    @Bean
    override fun yangLogStorageService(): YangLogStorageService {
        return spy {
            doReturn(
                YangLogStorage.YangLogStoreResponse.newBuilder()
                    .setSuccess(true)
                    .build()
            ).`when`(it).yangLogStore(Mockito.any())
        }
    }

    @Bean
    override fun aliasMakerService(): AliasMakerService {
        return mock {
            doAnswer { invocation ->
                val builder = AliasMaker.GetTaskOffersResponse.newBuilder()
                val resource =
                    javaClass.getResourceAsStream("/remote/am-getTaskOffers.json") ?: error("test file not found")
                JsonFormat.merge(InputStreamReader(resource), builder)

                val request = invocation.arguments[0] as AliasMaker.GetTaskOffersRequest
                val offerIds = request.offerIdList.toSet()
                val offers = builder.offerList.filter { offer -> offerIds.contains(offer.offerId) }.toList()
                return@doAnswer builder.clearOffer().addAllOffer(offers).build()
            }.`when`(it).getTaskOffers(Mockito.any())
        }
    }

    @Bean
    override fun categoryModelsService(): CategoryModelsService {
        return mock { }
    }

    @Bean
    override fun modelFormService(): ModelFormService {
        val builder = ModelForms.GetModelFormsResponse.newBuilder()
        val resource =
            javaClass.getResourceAsStream("/remote/exporter-getModelForms.json") ?: error("test file not found")
        JsonFormat.merge(InputStreamReader(resource), builder)
        return mock {
            doReturn(builder.build()).`when`(it).getModelForms(Mockito.any())
        }
    }

    @Bean
    override fun modelStorageService(): ModelStorageService {
        return mock()
    }

    @Bean
    override fun categoryParametersService(): CategoryParametersService {
        val builder = MboParameters.GetCategoryParametersResponse.newBuilder()
        val resource =
            javaClass.getResourceAsStream("/remote/exporter-getParameters.json") ?: error("test file not found")
        JsonFormat.merge(InputStreamReader(resource), builder)
        return mock {
            doReturn(builder.build()).`when`(it).getParameters(Mockito.any())
        }
    }

    @Bean
    override fun mboMappingsService(): MboMappingsService {
        return MboMappingsServiceMock()
    }

    @Bean
    fun honeypotsReader(): HoneypotsReader {
        val result = mock<HoneypotsReader> { }
        val honeyPotsString = javaClass.classLoader.getResourceAsStream("honeypots.json")
        val honeyPostRows = CommonObjectMapper.readValue<List<YtHoneypotsReader.HoneyPotInnerRow>>(
            honeyPotsString,
            object : TypeReference<List<YtHoneypotsReader.HoneyPotInnerRow>>() {}
        )
        val resultRows = honeyPostRows.map { TolokaTask("1", it.inputValues, it.knownSolutions) }
        doReturn(resultRows).`when`(result).loadHoneypots(any(), any(), any())
        return result
    }

    @Bean
    fun hintsReader(): HintsReader {
        val result = mock<HintsReader> { }
        val honeyPotsString = javaClass.classLoader.getResourceAsStream("hints.json")
        val honeyPostRows = CommonObjectMapper.readValue<List<YtHintsReader.HintInnerRow>>(
            honeyPotsString,
            object : TypeReference<List<YtHintsReader.HintInnerRow>>() {}
        )
        val resultRows = honeyPostRows.map { TolokaTask("1", it.inputValues, it.knownSolutions, it.hint) }
        doReturn(resultRows).`when`(result).loadHints(any(), any(), any())
        return result
    }

    @Bean
    fun ytHttpApi(): Yt {
        return mock { }
    }
}
