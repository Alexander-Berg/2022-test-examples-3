const { str: CRC32 } = require('crc-32');

module.exports = {
    UserStorageKey: {
        IOT_SCENARIO_ACTION_LIST_DRAG_N_DROP_HINT_CLOSED: 'iot-scenario-action-list-drag-n-drop-hint-closed',
        IOT_SCENARIOS_ONETIME_HINT_CLOSED: 'iot-scenarios-onetime-hint-closed1',
        IOT_SCENARIOS_COMMAND_HINT_CLOSED: 'iot-scenarios-command-hint-closed',
        ALICE_SUBSCRIPTIONS_HINT_CLOSED: 'alice-subscriptions-hint-closed',
        FAIRY_TALES_HINT_CLOSED: 'fairy-tales-hint-closed',
        IOT_SCENE_TOOLTIP_CLOSED: 'iot-scene-tooltip-closed',
        IOT_HOUSEHOLDS_TOOLTIP_CLOSED: 'iot-households-tooltip-closed',
        IOT_PROPERTIES_HISTORY_TOOLTIP_CLOSED: 'iot-properties-history-tooltip-closed',
        NEWS_BACKTRACKING_TOOLTIP_SHOWN: 'news-backtracking-tooltip-shown',
        SHOW_BACKTRACKING_TOOLTIP_SHOWN: 'show-backtracking-tooltip-shown',
        EQUALIZER_ROOM_CORRECTION_TOOLTIP_CLOSED: 'equalizer-room-correction-tooltip-closed',
        HIDE_MODULE_GROUP_PROMO: 'hide-module-group-promo',
        BACKGROUND_TYPE: 'background-type',
        HOME_FAVORITE_PROPERTIES_GRID_HINT_CLOSED: 'home-favorite-properties-grid-hint-closed',
        HOME_FAVORITE_PROPERTIES_VIEW_TYPE: 'home-favorite-properties-view-type',
        NOT_NEW_DEVICES: 'not-new-devices',
        FAVORITES_STAR_TOOLTIP_CLOSED: 'favorites-star-tooltip-closed',
    },

    UserStorageDynamicKeys: {
        HIDE_DEVICE_INFORMER: (deviceId, message) => `hide-device-${deviceId}-informer-${String(CRC32(message))}`,
        STORY_VIEW: (targetPrefix, storyId) => `story-view__${targetPrefix}__${storyId}`,
    },
};
