const { create, Entity } = require('@yandex-int/bem-page-object');

const elems = {};

elems.TextButton = new Entity({ block: 'text-button' });

elems.ListItem = new Entity({ block: 'list-item' });

elems.TextInput = new Entity({ block: 'text-input' });
elems.TextInputReal = new Entity({ block: 'text-input', elem: 'input' });

// Настройки - оплата и история операций: выбор карты
elems.CardSelect = new Entity({ block: 'card-select' });

// Главная страница
elems.IotHome = new Entity({ block: 'iot-home' });
// Главная страница - устройства
elems.IotHome.DevicesList = new Entity({ block: 'iot-home', elem: 'devices-list' });
// Главная страница - сценарии
elems.IotScenarios = new Entity({ block: 'iot-scenarios' });
elems.IotScenarios.ScenariosList = new Entity({ block: 'iot-scenarios', elem: 'command-scenarios-section' });

elems.Tab = new Entity({ block: 't-tab' });
elems.Tab.button = new Entity({ block: 't-tab', elem: 'button' });

// Страница устройства
elems.IotDevice = new Entity({ block: 'iot-device-state' });
// Страница устройства - пульт управления
elems.IotDevice.Settings = new Entity({ block: 'iot-device-state', elem: 'settings' });
// Страница устройства - голосовые команды
elems.IotDevice.Voice = new Entity({ block: 'iot-device-state', elem: 'suggestions' });

// Настройки устройства
elems.IotDeviceEdit = new Entity({ block: 'iot-device-edit' });

// Навбар кнопка
elems.NavbarButton = new Entity({ block: 'navbar-button' });
// Навбар - кнопка настройки
elems.NavbarButtonSettings = elems.NavbarButton.mods({ type: 'settings' });
// Навбар - кнопка удалить
elems.NavbarButtonRemove = elems.NavbarButton.mods({ type: 'remove' });

// Контент модального окна
elems.ModalContent = new Entity({ block: 'modal', elem: 'content' });
// Контент окна confirmation
elems.ConfirmContent = new Entity({ block: 'confirm', elem: 'content' });

elems.SettingsShow = new Entity({ block: 'account-settings-show' });
elems.SettingsShow.section = new Entity({ block: 'account-settings-show__section' });
elems.SettingsShow.section.listItem = new Entity({ block: 'list-item' });
elems.SettingsShow.section.listItem.toggle = new Entity({ block: 'toggle-switch' });
elems.SettingsShow.section.expandButton = new Entity({ block: 'expand-button' });

elems.ContentAccessPage = new Entity({ block: 'content-access' });
elems.ContentAccessVoiceTab = new Entity({ block: 't-content-access-voice', elem: 'button' });
elems.ContentAccessVoiceTabAdult = elems.ContentAccessVoiceTab.mods({ type: 'adult' });
elems.ContentAccessVoiceTabChild = elems.ContentAccessVoiceTab.mods({ type: 'child' });

elems.DayOfWeekSelectDayButton = new Entity({ block: 'day-of-week-select', elem: 'day' });

elems.ScenarioEditPage = new Entity({ block: 'iot-scenario-edit' });
elems.ScenarioEditPage.content = new Entity({ block: 'iot-scenario-edit__content' });
elems.ScenarioEditPage.content.listItem = new Entity({ block: 'list-item' });
elems.ScenarioEditPage.content.listItem.toggle = new Entity({ block: 'toggle-switch' });
elems.ScenarioEditPageSaveButton = new Entity({ block: 'iot-scenario-edit', elem: 'continue' });

elems.ScenarioIcon = new Entity({ block: 'iot-scenario-icon' });

elems.ScenarioActionSelectorCurrentSpeaker = new Entity({ block: 't-iot-scenario-edit-action-selector', elem: 'current-speaker' });
elems.ScenarioActionSelectorDeviceItem = new Entity({ block: 't-iot-scenario-edit-action-selector', elem: 'device' });
elems.ScenarioActionSelectorDeviceItemSpeaker = elems.ScenarioActionSelectorDeviceItem.mods({ 'is-speaker': true });
elems.ScenarioActionSelectorDeviceItemSmartHome = elems.ScenarioActionSelectorDeviceItem.mods({ 'is-speaker': false });

elems.ScenarioSpeakerActionListItem = new Entity({ block: 't-iot-scenario-speaker-capabilities-list', elem: 'item' });
elems.ScenarioSpeakerActionListItemQuasarServerActionText = elems.ScenarioSpeakerActionListItem.mods({ type: 'devices_capabilities_quasar_server_action', instance: 'text_action' });
elems.ScenarioSpeakerActionListItemQuasarServerActionPhrase = elems.ScenarioSpeakerActionListItem.mods({ type: 'devices_capabilities_quasar_server_action', instance: 'phrase_action' });

elems.ScenarioSpeakerCapabilityEditTextActionInput = new Entity({ block: 't-iot-scenario-speaker-capability-edit-text-action', elem: 'text-input' }).descendant(elems.TextInputReal);
elems.ScenarioSpeakerCapabilityEditTextActionCompleteButton = new Entity({ block: 't-iot-scenario-speaker-capability-edit-text-action', elem: 'complete-button' });

elems.ScenarioSpeakerCapabilityEditTtsInput = new Entity({ block: 't-iot-scenario-speaker-capability-edit-tts', elem: 'text-input' }).descendant(elems.TextInputReal);
elems.ScenarioSpeakerCapabilityEditTtsCompleteButton = new Entity({ block: 't-iot-scenario-speaker-capability-edit-tts', elem: 'complete-button' });

elems.ScenarioSummaryAddTriggerButton = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'add-trigger-button' });
elems.ScenarioSummaryAddActionButton = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'add-action-button' });
elems.ScenarioSummaryTriggerList = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'trigger-list' });
elems.ScenarioSummaryActionList = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'action-list' });

elems.ScenarioTimeTriggerIntermediary = new Entity({ block: 'iot-scenario-edit-time-trigger-intermediary', elem: 'content ' });

elems.IotNewsFeed = new Entity({ block: 'iot-news-feed' });
elems.IotNewsFeed.list = new Entity({ block: 'iot-news-feed', elem: 'list' });
elems.IotNewsFeed.list.wrapper = new Entity({ block: 'iot-news-feed', elem: 'item-wrapper' });
elems.IotNewsFeed.list.wrapper.item = new Entity({ block: 'story-tile' });
elems.IotNewsFeed.list.wrapper.item.title = new Entity({ block: 'story-tile', elem: 'title' });

elems.Story = new Entity({ block: 'story-modal_visible' });
elems.Story.wrapper = new Entity({ block: 'story__story-item-wrapper_active' });
elems.Story.wrapper.content = new Entity({ block: 'story-item-custom', elem: 'content' });
elems.Story.wrapper.content.button = new Entity({ block: 'button' });
elems.Story.wrapper.content.background = new Entity({ block: 'story-item-custom', elem: 'background' });
elems.StoriesModalItemWrappers = new Entity({ block: 'stories-modal__item-wrapper' });
elems.StoriesModalActiveItemWrapper = elems.StoriesModalItemWrappers.mods({ active: true });
elems.StoriesModalActiveStoryWrapper = elems.StoriesModalActiveItemWrapper.descendant(elems.Story.wrapper);


elems.TimeRoll = new Entity({ block: 'time-roll' });
elems.TimeRoll.hour = new Entity({ block: 'time-roll', elem: 'hour-roll' });
elems.TimeRoll.minute = new Entity({ block: 'time-roll', elem: 'minute-roll' });

module.exports = {
    loadPageObject() {
        return create(elems);
    },
};
