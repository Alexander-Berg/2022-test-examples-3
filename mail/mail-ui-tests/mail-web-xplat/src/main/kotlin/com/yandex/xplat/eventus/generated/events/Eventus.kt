// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM events/eventus.ts >>>

package com.yandex.xplat.eventus

import com.yandex.xplat.common.YSMap
import com.yandex.xplat.common.set
import com.yandex.xplat.eventus.common.Aggregator
import com.yandex.xplat.eventus.common.EventusRegistry
import com.yandex.xplat.eventus.common.MapAggregatorProvider

public open class Eventus {
    companion object {
        @JvmStatic val version: Int = 4
        @JvmStatic var startEvents: StartEvents = StartEvents()
        @JvmStatic var messageViewEvents: MessageEvents = MessageEvents()
        @JvmStatic var groupActionsEvents: GroupActionsEvents = GroupActionsEvents()
        @JvmStatic var messageActionsEvents: MessageActionsEvents = MessageActionsEvents()
        @JvmStatic var messageListEvents: MessageListEvents = MessageListEvents()
        @JvmStatic var composeEvents: ComposeEvents = ComposeEvents()
        @JvmStatic var pushEvents: PushEvents = PushEvents()
        @JvmStatic var modelSyncEvents: ModelSyncEvents = ModelSyncEvents()
        @JvmStatic var quickReplyEvents: QuickReplyEvents = QuickReplyEvents()
        @JvmStatic var accountSettingsEvents: AccountSettingsEvents = AccountSettingsEvents()
        @JvmStatic var searchEvents: SearchEvents = SearchEvents()
        @JvmStatic var settingsEvents: SettingsEvents = SettingsEvents()
        @JvmStatic var storiesEvents: StoriesEvents = StoriesEvents()
        @JvmStatic var multiAccountEvents: MultiAccountEvents = MultiAccountEvents()
        @JvmStatic var contactEvents: ContactEvents = ContactEvents()
        @JvmStatic var ecomailEvents: EcomailEvents = EcomailEvents()
        @JvmStatic var phishingEvents: PhishingEvents = PhishingEvents()
        @JvmStatic
        open fun setup(): Unit {
            val aggregators: YSMap<String, Aggregator> = mutableMapOf<String, Aggregator>()
            aggregators.set(EventNames.MODEL_SYNC_MESSAGE_LIST, MessageListSyncAggregator())
            EventusRegistry.setAggregatorProvider(MapAggregatorProvider(aggregators))
            EventusRegistry.version = Eventus.version
        }

    }
}

