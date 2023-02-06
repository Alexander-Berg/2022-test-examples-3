package ru.yandex.market.contentmapping.testutils

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import ru.yandex.market.ir.http.Formalizer
import ru.yandex.market.ir.http.Formalizer.FormalizedOffer
import ru.yandex.market.ir.http.Formalizer.FormalizerRequest
import ru.yandex.market.ir.http.Formalizer.FormalizerResponse
import ru.yandex.market.ir.http.FormalizerService
import ru.yandex.market.ir.http.UltraController
import ru.yandex.market.ir.http.UltraController.DataRequest
import ru.yandex.market.ir.http.UltraController.DataResponse
import ru.yandex.market.ir.http.UltraControllerService
import java.util.function.Function
import java.util.stream.Collectors

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
object RequestMockUtils {
    fun mockUltraControllerEnrich(
            ultraControllerService: UltraControllerService,
            converter: (UltraController.Offer) -> UltraController.EnrichedOffer,
    ) {
        Mockito.doAnswer { args ->
            val request = args.getArgument<DataRequest>(0)
            DataResponse.newBuilder()
                    .addAllOffers(request.offersList.map(converter))
                    .build()
        }.`when`(ultraControllerService).enrich(Mockito.any())
    }

    fun mockFormalizerFormalize(
            formalizerService: FormalizerService,
            converter: (Formalizer.Offer) -> FormalizedOffer,
    ) {
        Mockito.doAnswer { args ->
            val request = args.getArgument<FormalizerRequest>(0)
            FormalizerResponse.newBuilder()
                    .addAllOffer(request.offerList.map(converter))
                    .build()
        }.`when`(formalizerService).formalize(Mockito.any())
    }
}
