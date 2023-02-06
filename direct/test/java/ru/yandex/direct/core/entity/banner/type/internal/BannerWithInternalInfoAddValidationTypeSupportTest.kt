package ru.yandex.direct.core.entity.banner.type.internal

import com.nhaarman.mockitokotlin2.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl
import ru.yandex.direct.core.entity.banner.model.BannerWithInternalInfo
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.model.TemplateVariable
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode
import ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdStringDefectIds
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.validation.builder.Validator
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathNode
import ru.yandex.direct.validation.result.ValidationResult

class BannerWithInternalInfoAddValidationTypeSupportTest {
    @Mock
    private lateinit var provider: BannerWithInternalInfoValidatorProvider

    @InjectMocks
    @Spy
    private lateinit var support: BannerWithInternalInfoAddValidationTypeSupport

    private lateinit var container: BannersAddOperationContainerImpl

    @Before
    fun beforeEach() {
        MockitoAnnotations.openMocks(this)
        container = defaultBannersAddOperationContainer()
    }

    @Test
    fun checkAddingWarningsOfBannerUnreachableUrlsIntoContainer_WhenCopy() {
        val bannerWithUnreachableUrlIndex = 1
        Mockito.doAnswer {
            validatorForBannersWithUnreachableUrl(setOf(bannerWithUnreachableUrlIndex))
        }.`when`(provider).bannerWithInternalExtraInfoListValidator(any())
        container.isCopy = true
        val banners = Array(3) {
            InternalBanner().withId(it + 1L).withTemplateVariables(
                listOf(
                    TemplateVariable().withInternalValue("http://invalid_url.url")
                )
            )
        }.toCollection(arrayListOf())

        val vr: ValidationResult<List<BannerWithInternalInfo>, Defect<*>> =
            ValidationResult(banners, Defect::class.java)

        support.validate(container, vr)

        assertThat(container.bannersWithUnreachableUrlIndexes).isEqualTo(setOf(bannerWithUnreachableUrlIndex))
    }
}

private fun validatorForBannersWithUnreachableUrl(indexesOfUnreachable: Set<Int>) =
    Validator { banners: List<BannerWithInternalInfo> ->
        val vr = ValidationResult<List<BannerWithInternalInfo>, Defect<*>>(banners, Defect::class.java)
        banners.forEachIndexed { index, bannerWithInternalInfo ->
            if (indexesOfUnreachable.contains(index)) {
                val subVr = ValidationResult(
                    bannerWithInternalInfo.templateVariables[0],
                    null,
                    listOf(Defect(InternalAdStringDefectIds.URL_UNREACHABLE))
                )
                vr.addSubResult(PathNode.Index(index), subVr)
            }
        }
        vr
    }

private fun defaultBannersAddOperationContainer(): BannersAddOperationContainerImpl = BannersAddOperationContainerImpl(
    1, 2, RbacRole.CLIENT, ClientId.fromLong(3), 4, 5, 6,
    emptySet(), ModerationMode.DEFAULT, false, false, true
)
