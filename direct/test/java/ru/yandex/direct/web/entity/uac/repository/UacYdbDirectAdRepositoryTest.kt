package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.model.direct_ad.DirectAdStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.generateUniqueRandomId
import ru.yandex.direct.tracing.util.TraceUtil
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbDirectAdRepositoryTest : AbstractUacRepositoryTest() {
    @Autowired
    private lateinit var directAdRepository: UacYdbDirectAdRepository

    @Test
    fun updateStatusByDirectAdIdsTest() {
        val directAd1 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId())
        val directAd2 = createDirectAd(status = DirectAdStatus.ERROR_UNKNOWN, directAdId = TraceUtil.randomId())
        val directAd3 = createDirectAd(status = DirectAdStatus.ARCHIVED, directAdId = TraceUtil.randomId())
        directAdRepository.saveDirectAd(directAd1)
        directAdRepository.saveDirectAd(directAd2)
        directAdRepository.saveDirectAd(directAd3)
        val directAdIdsToUpdate = listOf(directAd1, directAd2).map { it.directAdId!! }
        directAdRepository.updateStatusByDirectAdIds(directAdIdsToUpdate, DirectAdStatus.ACTIVE)

        val gotAds = directAdRepository.getByDirectAdId(listOf(directAd1, directAd2, directAd3).map { it.directAdId!! })

        assertThat(gotAds)
            .containsExactlyInAnyOrder(
                directAd1.copy(status = DirectAdStatus.ACTIVE),
                directAd2.copy(status = DirectAdStatus.ACTIVE),
                directAd3)
    }

    @Test
    fun getNotDeletedDirectAdsByContentIdsTest() {
        val titleContentId =  generateUniqueRandomId()
        val textContentId =  generateUniqueRandomId()
        val imageContentId =  generateUniqueRandomId()
        val videoContentId =  generateUniqueRandomId()
        val directAd1 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), titleContentId = titleContentId)
        val directAd2 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), textContentId = textContentId)
        val directAd3 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directImageContentId = imageContentId)
        val directAd4 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directVideoContentId = videoContentId)
        directAdRepository.saveDirectAd(directAd1)
        directAdRepository.saveDirectAd(directAd2)
        directAdRepository.saveDirectAd(directAd3)
        directAdRepository.saveDirectAd(directAd4)


        val getDirectAds = directAdRepository.getNotDeletedDirectAdsWithoutAdGroupsByContentIds(
            setOf(titleContentId), setOf(textContentId), setOf(imageContentId), setOf(videoContentId))


        assertThat(getDirectAds)
            .containsExactlyInAnyOrder(directAd1, directAd2, directAd3, directAd4)
    }

    @Test
    fun getNotDeletedDirectAdsByContentIds_TitleIdsIsEmptyTest() {
        val titleContentId =  generateUniqueRandomId()
        val textContentId =  generateUniqueRandomId()
        val imageContentId =  generateUniqueRandomId()
        val videoContentId =  generateUniqueRandomId()
        val directAd1 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), titleContentId = titleContentId)
        val directAd2 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), textContentId = textContentId)
        val directAd3 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directImageContentId = imageContentId)
        val directAd4 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directVideoContentId = videoContentId)
        directAdRepository.saveDirectAd(directAd1)
        directAdRepository.saveDirectAd(directAd2)
        directAdRepository.saveDirectAd(directAd3)
        directAdRepository.saveDirectAd(directAd4)


        val getDirectAds = directAdRepository.getNotDeletedDirectAdsWithoutAdGroupsByContentIds(
            setOf(), setOf(textContentId), setOf(imageContentId), setOf(videoContentId))


        assertThat(getDirectAds)
            .containsExactlyInAnyOrder(directAd2, directAd3, directAd4)
    }

    @Test
    fun getNotDeletedDirectAdsByContentIds_TextIdsIsEmptyTest() {
        val titleContentId =  generateUniqueRandomId()
        val textContentId =  generateUniqueRandomId()
        val imageContentId =  generateUniqueRandomId()
        val videoContentId =  generateUniqueRandomId()
        val directAd1 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), titleContentId = titleContentId)
        val directAd2 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), textContentId = textContentId)
        val directAd3 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directImageContentId = imageContentId)
        val directAd4 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directVideoContentId = videoContentId)
        directAdRepository.saveDirectAd(directAd1)
        directAdRepository.saveDirectAd(directAd2)
        directAdRepository.saveDirectAd(directAd3)
        directAdRepository.saveDirectAd(directAd4)

        val getDirectAds = directAdRepository.getNotDeletedDirectAdsWithoutAdGroupsByContentIds(
            setOf(titleContentId), setOf(), setOf(imageContentId), setOf(videoContentId))

        assertThat(getDirectAds)
            .containsExactlyInAnyOrder(directAd1, directAd3, directAd4)
    }

    /**
     * Тест проверяет, что если у баннера два подходящих контента, то он вернется только один раз
     */
    @Test
    fun getNotDeletedDirectAdsByContentIds_AdsWithSeveralSuitableContentsTest() {
        val titleContentId =  generateUniqueRandomId()
        val textContentId =  generateUniqueRandomId()
        val imageContentId =  generateUniqueRandomId()
        val videoContentId =  generateUniqueRandomId()
        val directAd1 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), titleContentId = titleContentId, textContentId = textContentId)
        val directAd2 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), textContentId = textContentId)
        val directAd3 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directImageContentId = imageContentId)
        val directAd4 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directVideoContentId = videoContentId)
        directAdRepository.saveDirectAd(directAd1)
        directAdRepository.saveDirectAd(directAd2)
        directAdRepository.saveDirectAd(directAd3)
        directAdRepository.saveDirectAd(directAd4)


        val getDirectAds = directAdRepository.getNotDeletedDirectAdsWithoutAdGroupsByContentIds(
            setOf(titleContentId), setOf(textContentId), setOf(imageContentId), setOf(videoContentId))


        assertThat(getDirectAds)
            .containsExactlyInAnyOrder(directAd1, directAd2, directAd3, directAd4)
    }

    /**
     * Тест проверяет, что если у баннера два подходящих контента, то он вернется только один раз
     */
    @Test
    fun getNotDeletedDirectAdsByContentIds_AllParamsEmptyTest() {
        val titleContentId =  generateUniqueRandomId()
        val textContentId =  generateUniqueRandomId()
        val imageContentId =  generateUniqueRandomId()
        val videoContentId =  generateUniqueRandomId()
        val directAd1 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), titleContentId = titleContentId, textContentId = textContentId)
        val directAd2 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), textContentId = textContentId)
        val directAd3 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directImageContentId = imageContentId)
        val directAd4 = createDirectAd(status = DirectAdStatus.CREATED, directAdId = TraceUtil.randomId(), directVideoContentId = videoContentId)
        directAdRepository.saveDirectAd(directAd1)
        directAdRepository.saveDirectAd(directAd2)
        directAdRepository.saveDirectAd(directAd3)
        directAdRepository.saveDirectAd(directAd4)


        val getDirectAds = directAdRepository.getNotDeletedDirectAdsWithoutAdGroupsByContentIds(
            setOf(), setOf(), setOf(), setOf())

        assertThat(getDirectAds)
            .isEmpty()
    }
}
