package ru.yandex.direct.core.entity.banner.model

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

private typealias ExportBannerClass = KClass<out BaseBannerWithResourcesForBsExport>

@RunWith(JUnitParamsRunner::class)
class BsExportBannerInterfacesCheck {

    @Suppress("unused")
    private fun endTypesForExport(): List<List<Any>> {
        return reflections
            .getSubTypesOf(BaseBannerWithResourcesForBsExport::class.java)
            .map { it.kotlin }
            .filter { !it.isAbstract }
            .filter { it.isSubclassOf(BaseBannerWithResourcesForBsExport::class) }
            .flatMap { endType ->
                baseClasses.map { (exportInterface, _) ->
                    listOf(endType, exportInterface)
                }
            }
    }

    /**
     * Проверка, не забыт ли интерфейс ForBsExport в конечном типе баннера
     *
     * Если класс реализует базовые классы интерфейса *ForBsExport,
     * то он или реализует этот интерфейс, или в белом списке
     *
     * Если этот тест падает - нужно или поправить схему баннера, или добавить тип в белый список
     */
    @Test
    @Parameters(method = "endTypesForExport")
    @TestCaseName("{0} must implement {1} or be whitelisted")
    fun `class with base interfaces must implement export interface`(
        cls: ExportBannerClass,
        exportInterface: ExportBannerClass,
    ) {
        val baseClasses = baseClasses.getValue(exportInterface)
        val hasBaseClasses = cls.superclasses.containsAll(baseClasses)
        val hasExportInterface = cls.isSubclassOf(exportInterface)
        val whiteListed = isWhiteListed(cls, exportInterface)
        val cause = "Class ${cls.simpleName} implements " +
            "${baseClasses.joinToString { it.simpleName!! }}, " +
            "but not ${exportInterface.simpleName}. " +
            "Please implement ${cls.simpleName} or update white list."
        assertThat(!hasBaseClasses || hasExportInterface || whiteListed)
            .withFailMessage(cause)
            .isTrue
    }

    @Suppress("unused")
    private fun whiteListEntries(): List<List<Any>> {
        return whiteList.entries
            .flatMap { (exportInterface, endTypes) ->
                endTypes.map { endType ->
                    listOf(endType, exportInterface)
                }
            }
    }

    /**
     * Проверка, есть ли в белом списке классы, которые в принципе не могут реализовать интерфейс *ForBsExport
     *
     * Если класс находится в белом списке интерфейса *ForBsExport,
     * то он реализует базовые классы этого интерфейса
     *
     * Если этот тест падает, нужно убрать класс из белого списка
     */
    @Test
    @Parameters(method = "whiteListEntries")
    @TestCaseName("{0} must not be in the white list, because it can't implement {1}")
    fun `class without base interfaces cannot be in white list`(
        cls: ExportBannerClass,
        exportInterface: ExportBannerClass,
    ) {
        val baseClasses = baseClasses.getValue(exportInterface)
        val cause = "Class ${cls.simpleName} is in white list, but does not implement " +
            "base interfaces of ${exportInterface.simpleName}"
        assertThat(cls.superclasses)
            .withFailMessage(cause)
            .containsAll(baseClasses)
    }

    /**
     * Проверка, есть ли в белом списке классы, которые уже можно убирать оттуда
     *
     * Если класс находится в белом списке интерфейса *ForBsExport,
     * то он не реализует этот интерфейс
     *
     * Если этот тест падает, нужно убрать лишний класс из белого списка
     */
    @Test
    @Parameters(method = "whiteListEntries")
    @TestCaseName("{0} must not be in the white list, because it implements {1}")
    fun `class with export interface cannot be in white list`(
        cls: ExportBannerClass,
        exportInterface: ExportBannerClass,
    ) {
        val hasExportInterface = cls.isSubclassOf(exportInterface)
        val cause = "Class ${cls.simpleName} is in white list, " +
            "but implements ${exportInterface.simpleName}"
        assertThat(hasExportInterface)
            .withFailMessage(cause)
            .isFalse
    }

    companion object {
        /**
         * Белый список конечных классов
         * Здесь записаны конечные классы баннера, которые не реализуют интерфейс *ForBsExport
         * но реализуют базовые классы для этого интерфейса
         * (например, BannerWithHref для интерфейса BannerWithHrefForBsExport)
         * Этот список можно и нужно обновлять вручную при добавлении таких конечных классов
         */
        private val whiteList = mapOf(
            BannerWithMobileContentAdGroupForBsExport::class to setOf(
                ContentPromotionBanner::class,
                CpmAudioBanner::class,
                CpmIndoorBanner::class,
                CpmOutdoorBanner::class,
                DynamicBanner::class,
                McBanner::class,
                ModerationableBanner::class,
                TextBanner::class,
            ),
            BannerWithHrefForBsExport::class to setOf(
                DynamicBanner::class,
                ModerationableBanner::class,
            ),
        )

        private fun isWhiteListed(cls: KClass<out Banner>, exportInterface: ExportBannerClass) =
            whiteList[exportInterface]?.contains(cls) ?: false

        private val reflections = Reflections("ru.yandex.direct.core.entity.banner.model")

        /**
         * Базовые классы интерфейсов ForBsExport
         */
        private val baseClasses = reflections
            .getSubTypesOf(BaseBannerWithResourcesForBsExport::class.java)
            .map { it.kotlin }
            .filter { it.java.simpleName.endsWith("ForBsExport") }
            .filter { it != BaseBannerWithResourcesForBsExport::class }
            .associateWith { endType ->
                @Suppress("unchecked_cast") // для `it as KClass<out Banner>`
                endType.superclasses
                    .filter { it.isSubclassOf(Banner::class) }
                    .filter { it != BaseBannerWithResourcesForBsExport::class }
                    .map { it as KClass<out Banner> }
                    .toSet()
            }
    }
}
