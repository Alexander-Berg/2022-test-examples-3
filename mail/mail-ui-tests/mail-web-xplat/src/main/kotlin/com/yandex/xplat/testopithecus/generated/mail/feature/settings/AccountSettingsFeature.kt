// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/settings/account-settings-feature.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.YSMap
import com.yandex.xplat.mapi.SignaturePlace
import com.yandex.xplat.testopithecus.common.Feature

public interface AccountSettings {
    fun openAccountSettings(accountIndex: Int): Unit
    fun closeAccountSettings(): Unit
    fun isGroupBySubjectEnabled(): Boolean
    fun switchGroupBySubject(): Unit
    fun switchSortingEmailsByCategory(): Unit
    fun isSortingEmailsByCategoryEnabled(): Boolean
    fun openMailingListsManager(): Unit
    fun getSignature(): String
    fun changeSignature(newSignature: String): Unit
    fun switchTheme(): Unit
    fun isThemeEnabled(): Boolean
    fun getFolderToNotificationOption(): YSMap<FolderName, NotificationOption>
    fun setNotificationOptionForFolder(folder: FolderName, option: NotificationOption): Unit
    fun getNotificationOptionForFolder(folder: FolderName): NotificationOption
}

public open class AccountSettingsFeature private constructor(): Feature<AccountSettings>("AccountSettings", "Общие для iOS и Android настройки аккаунта пользователя. И в iOS, и в Android открываются с экрана Root Settings") {
    companion object {
        @JvmStatic var `get`: AccountSettingsFeature = AccountSettingsFeature()
    }
}

public interface IosAccountSettings {
    fun changePhoneNumber(newPhoneNumber: String): Unit
    fun getPushNotificationSound(): NotificationSound
    fun setPushNotificationSound(sound: NotificationSound): Unit
    fun switchPushNotification(): Unit
    fun isPushNotificationForAllEnabled(): Boolean
}

public open class IosAccountSettingsFeature private constructor(): Feature<IosAccountSettings>("IosAccountSettings", "Специфичные для iOS настройки аккаунта пользователя.") {
    companion object {
        @JvmStatic var `get`: IosAccountSettingsFeature = IosAccountSettingsFeature()
    }
}

public interface AndroidAccountSettings {
    fun switchUseAccountSetting(): Unit
    fun isAccountUsingEnabled(): Boolean
    fun openFolderManager(): Unit
    fun openLabelManager(): Unit
    fun openPassport(): Unit
    fun setPlaceForSignature(place: SignaturePlace): Unit
    fun getPlaceForSignature(): SignaturePlace
}

public open class AndroidAccountSettingsFeature private constructor(): Feature<AndroidAccountSettings>("AndroidAccountSettings", "Специфичные для Android настройки аккаунта пользователя.") {
    companion object {
        @JvmStatic var `get`: AndroidAccountSettingsFeature = AndroidAccountSettingsFeature()
    }
}

public enum class NotificationSound(val value: String) {
    standard("Standard"),
    yandexMail("Yandex.Mail"),
    ;
    override fun toString(): String = value
}
public enum class NotificationOption(val value: String) {
    doNotSync("Do not sync"),
    syncWithoutNotification("Sync without notification"),
    syncAndNotifyMe("Sync and notify me"),
    ;
    override fun toString(): String = value
}