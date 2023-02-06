package ru.yandex.direct.jobs.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.jobs.internal.InternalBannerModerationStuckNotificationJob.Companion.MAX_AGE_HOURS
import ru.yandex.direct.jobs.internal.model.BannerModerationStuckMessage
import ru.yandex.direct.mail.MailSender
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy


class InternalBannerModerationStuckNotificationJobTest {

    lateinit var bannerTypedRepository: BannerTypedRepository
    lateinit var mailSender: MailSender
    lateinit var job: InternalBannerModerationStuckNotificationJob

    @BeforeEach
    fun setUp() {
        bannerTypedRepository = mock(BannerTypedRepository::class.java)
        mailSender = mock(MailSender::class.java)
        job = InternalBannerModerationStuckNotificationJob(bannerTypedRepository, mailSender).withShard(1)
                as InternalBannerModerationStuckNotificationJob
    }

    @Test
    fun shouldSendMessage_IfBannersOnModerationExistsOlderThanFourHours() {
        val banners = listOf(
            InternalBanner().withId(1L).withStatusModerate(BannerStatusModerate.SENT)
        )
        val expectedMsg = BannerModerationStuckMessage(banners, MAX_AGE_HOURS)
        val captor = ArgumentCaptor.forClass(BannerModerationStuckMessage::class.java)

        `when`(bannerTypedRepository.getInternalBannersOnModerationOlderThan(anyInt(), any()))
            .thenReturn(banners)

        job.execute()

        verify(mailSender)
            .send(captor.capture())
        assertThat(captor.value)
            .`is`(matchedBy(beanDiffer(expectedMsg)))
    }

    @Test
    fun shouldDoNothing_IfNoBannersOnModeration() {
        `when`(bannerTypedRepository.getInternalBannersOnModerationOlderThan(anyInt(), any()))
            .thenReturn(emptyList())

        job.execute()

        verifyNoInteractions(mailSender)
    }
}
