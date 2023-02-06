package ru.yandex.market.mapi.core.model.screen.showcondition

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.AbstractShowConditionTest
import ru.yandex.market.mapi.core.MapiHeaders

class AppVersionShowConditionTest : AbstractShowConditionTest() {

    @Test
    fun testContextHasNoPlatfrom() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }

            context.appPlatform = null
            context.appVersionRaw = "1.1.1"
        }
    }

    @Test
    fun testContextHasNoVersion() {
        assertConditionFail<AppVersionShowCondition>(
            expectedMessage = "AppVersionShowCondition. User request has no app version"
        ) { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = null
        }
    }

    @Test
    fun testContextHasEmptyVersion() {
        assertConditionFail<AppVersionShowCondition>(
            expectedMessage = "AppVersionShowCondition. User request has no app version"
        ) { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = ""
        }
    }

    @Test
    fun testConditionHasNoAndroid() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1.1"
        }
    }

    @Test
    fun testConditionHasNoIOS() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.1.1"
        }
    }

    @Test
    fun testConditionHasNoSign() {
        assertConditionFail<AppVersionShowCondition>(
            expectedMessage = "AppVersionShowCondition. Sign is not present: ANDROID"
        ) { condition, context ->
            condition.ios = appVersionMatcher {
                sign = null
                version = "1.1"
            }
            condition.android = appVersionMatcher {
                sign = null
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1"
        }
    }

    @Test
    fun testConditionHasNoVersion() {
        assertConditionFail<AppVersionShowCondition>(
            "AppVersionShowCondition. Version is not present: ANDROID"
        ) { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = null
            }
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE
                version = null
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1.1"
        }
    }

    @Test
    fun testVersionMore() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.1.1"
        }
    }

    @Test
    fun testVersionMoreForEqual() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = null
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1"
        }
    }

    @Test
    fun testVersionMoreForLess() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.0"
        }
    }

    @Test
    fun testVersionLess() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = null
            condition.android = appVersionMatcher {
                sign = VersionSign.LESS
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1"
        }
    }

    @Test
    fun testVersionLessForEqual() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.LESS
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.1"
        }
    }

    @Test
    fun testVersionLessForMore() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.LESS
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.2"
        }
    }

    @Test
    fun testVersionMoreOrEqualForMore() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE_OR_EQUAL
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.2"
        }
    }

    @Test
    fun testVersionMoreOrEqualForEqual() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = null
            condition.android = appVersionMatcher {
                sign = VersionSign.MORE_OR_EQUAL
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1"
        }
    }

    @Test
    fun testVersionMoreOrEqualForLess() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.MORE_OR_EQUAL
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.0"
        }
    }

    @Test
    fun testVersionLessOrEqualForLess() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.LESS_OR_EQUAL
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.0"
        }
    }

    @Test
    fun testVersionLessOrEqualForEqual() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = null
            condition.android = appVersionMatcher {
                sign = VersionSign.LESS_OR_EQUAL
                version = "1.1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1"
        }
    }

    @Test
    fun testVersionLessOrEqualForMore() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = appVersionMatcher {
                sign = VersionSign.LESS_OR_EQUAL
                version = "1.1"
            }
            condition.android = null

            context.appPlatform = MapiHeaders.PLATFORM_IOS
            context.appVersionRaw = "1.2"
        }
    }

    @Test
    fun testVersionIsNotInteger() {
        assertConditionHide<AppVersionShowCondition> { condition, context ->
            condition.ios = null
            condition.android = appVersionMatcher {
                sign = VersionSign.LESS_OR_EQUAL
                version = "broken.version"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.1"
        }
    }

    @Test
    fun testVersionIsNotIntegerGt() {
        assertConditionShow<AppVersionShowCondition> { condition, context ->
            condition.ios = null
            condition.android = appVersionMatcher {
                sign = VersionSign.LESS_OR_EQUAL
                version = "1.2.a1"
            }

            context.appPlatform = MapiHeaders.PLATFORM_ANDROID
            context.appVersionRaw = "1.2"
        }
    }
}
