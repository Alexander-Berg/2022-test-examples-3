const { create, Entity } = require('@yandex-int/bem-page-object');

const elems = {};

elems.App = new Entity({ block: 'app', elem: 'wrapper' });

elems.TextButton = new Entity({ block: 'text-button' });

elems.ListItem = new Entity({ block: 'list-item' });

elems.TextInput = new Entity({ block: 'text-input' });
elems.TextInputReal = new Entity({ block: 'text-input', elem: 'input' });

elems.Checkbox = new Entity({ block: 'check-box' });

elems.BottomSheetContent = new Entity({ block: 'bottom-sheet', elem: 'content' });
elems.BottomSheetContentLayout = new Entity({ block: 'bottom-sheet-content-layout' });
elems.BottomSheetWrapper = new Entity({ block: 'bottom-sheet', elem: 'wrapper_shown' });

elems.PageLayout = new Entity({ block: 'page-layout' });
elems.PageLayout.content = new Entity({ block: 'page-layout__content' });

// Настройки - оплата и история операций: выбор карты
elems.CardSelect = new Entity({ block: 'card-select' });

// Главная страница
elems.IotHome = new Entity({ block: 'home' });
// Главная страница - устройства
elems.IotHome.DevicesList = new Entity({ block: 'home-room' });
// Главная - стори
elems.IotHome.News = new Entity({ block: 'home', elem: 'news' });
// Главная страница - сценарии
elems.IotScenarios = new Entity({ block: 'home-scenarios' });
elems.IotScenarios.ScenariosList = new Entity({ block: 'home-scenarios', elem: 'command-scenarios-section' });
elems.IotScenarios.ScenariosListItem = new Entity({ block: 'iot-scenario-item' });
elems.IotScenarios.ScenariosLaunch = new Entity({ block: 'home-scenarios', elem: 'scenario-launch-section' });

elems.Tab = new Entity({ block: 't-tab' });
elems.Tab.button = new Entity({ block: 't-tab', elem: 'button' });

// меню добавления устройства
elems.IotAdd = new Entity({ block: 'page-layout__content' });
elems.IotAdd.ZigbeeButton = new Entity({ block: 'iot-add', elem: 'zigbee' });

// Страница устройства
elems.IotDevice = new Entity({ block: 'page-layout__content' });
// Страница устройства - пульт управления
elems.IotDevice.Settings = new Entity({ block: 'iot-control-layout' });
// Страница устройства - голосовые команды
elems.IotDevice.Voice = new Entity({ block: 'iot-device-state', elem: 'suggestions' });

// Настройки устройства
elems.IotDeviceEdit = new Entity({ block: 'iot-device-edit' });

// Навбар кнопка
elems.NavbarButton = new Entity({ block: 'navbar-button' });
// Навбар - кнопка настройки
elems.NavbarButtonSettings = elems.NavbarButton.mods(new Entity({ block: 'icon2', type: 'general__settings-icon' }));
// Навбар - кнопка удалить
elems.NavbarButtonRemove = elems.NavbarButton.mix(new Entity({ block: 'icon2', type: 'files__delete-icon' }));

// Контент модального окна
elems.ModalContent = new Entity({ block: 'modal', elem: 'content' });
// Контент окна confirmation
elems.ConfirmContent = new Entity({ block: 'confirm', elem: 'content' });

elems.ToggleSwitch = new Entity({ block: 'toggle-switch' });

elems.SettingsShow = new Entity({ block: 'page-layout' });
elems.SettingsShow.section = new Entity({ block: 'account-settings-show__section' });
elems.SettingsShow.section.listItem = new Entity({ block: 'list-item' });
elems.SettingsShow.section.listItem.toggle = new Entity({ block: 'toggle-switch' });
elems.SettingsShow.section.expandButton = new Entity({ block: 'expand-button' });

elems.ContentAccessPage = new Entity({ block: 'page-layout' });
elems.ContentAccessVoiceTab = new Entity({ block: 't-content-access-voice', elem: 'button' });
elems.ContentAccessVoiceTabAdult = elems.ContentAccessVoiceTab.mods({ type: 'adult' });
elems.ContentAccessVoiceTabChild = elems.ContentAccessVoiceTab.mods({ type: 'child' });

// Scenario
elems.DayOfWeekSelectDayButton = new Entity({ block: 'day-of-week-select', elem: 'day' });

elems.ScenarioEditPage = new Entity({ block: 'page-layout' });
elems.ScenarioEditPage.content = new Entity({ block: 'page-layout__content' });
elems.ScenarioEditPage.content.listItem = new Entity({ block: 'list-item' });
elems.ScenarioEditPage.content.listItem.toggle = new Entity({ block: 'toggle-switch' });
elems.ScenarioEditPage.primaryButton = new Entity({ block: 'button', elem: 'area_theme_primary' });
elems.ScenarioEditPageSaveButton = new Entity({ block: 'button_theme_primary' });

elems.ScenarioIcon = new Entity({ block: 'iot-scenario-icon' });

elems.ScenarioActionSelectorCurrentSpeaker = new Entity({ block: 't-iot-scenario-edit-action-selector', elem: 'current-speaker' });
elems.ScenarioActionSelectorDeviceItem = new Entity({ block: 't-iot-scenario-edit-action-selector', elem: 'device' });
elems.ScenarioActionSelectorDeviceItemSpeaker = elems.ScenarioActionSelectorDeviceItem.mods({ 'is-speaker': true });
elems.ScenarioActionSelectorDeviceItemSmartHome = elems.ScenarioActionSelectorDeviceItem.mods({ 'is-speaker': false });

elems.ScenarioSpeakerActionListItem = new Entity({ block: 't-iot-scenario-speaker-capabilities-list', elem: 'item' });
elems.ScenarioSpeakerActionListItemQuasarServerActionText = elems.ScenarioSpeakerActionListItem.mods({ type: 'devices_capabilities_quasar_server_action', instance: 'text_action' });
elems.ScenarioSpeakerActionListItemQuasarActionTts = elems.ScenarioSpeakerActionListItem.mods({ type: 'devices_capabilities_quasar', instance: 'tts' });

elems.ScenarioSpeakerCapabilityEditTextActionInput = new Entity({ block: 't-iot-scenario-speaker-capability-edit-text-action', elem: 'text-input' }).descendant(elems.TextInputReal);
elems.ScenarioSpeakerCapabilityEditTextActionCompleteButton = new Entity({ block: 't-iot-scenario-speaker-capability-edit-text-action', elem: 'complete-button' });

elems.ScenarioSpeakerCapabilityEditTtsInput = new Entity({ block: 't-iot-scenario-speaker-capability-edit-tts', elem: 'text-input' }).descendant(elems.TextInputReal);
elems.ScenarioSpeakerCapabilityEditTtsCompleteButton = new Entity({ block: 't-iot-scenario-speaker-capability-edit-tts', elem: 'complete-button' });

elems.ScenarioLightCapabilityEditColor = new Entity({ block: 'iot-color-select-dropdown-2' });

elems.ScenarioSummaryAddTriggerButton = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'add-trigger-button' });
elems.ScenarioSummaryAddActionButton = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'add-action-button' });
elems.ScenarioSummaryTriggerList = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'trigger-list' });
elems.ScenarioSummaryTriggerListItem = new Entity({ block: 'iot-scenario-trigger-item' });
elems.ScenarioSummaryActionList = new Entity({ block: 't-iot-scenario-edit-summary', elem: 'action-list' });

// IotNewsFeed
elems.IotNewsFeed = new Entity({ block: 'iot-news-feed', elem: 'content' });
elems.IotNewsFeed.wrapper = new Entity({ block: 'iot-news-feed', elem: 'item-wrapper' });
elems.IotNewsFeed.emptyItem = new Entity({ block: 'story-tile-empty' });
elems.IotNewsFeed.tile = new Entity({ block: 'story-tile' });

// StoriesGrid
elems.StoriesGrid = new Entity({ block: 'stories-grid' });
elems.StoriesGrid.item = new Entity({ block: 'story-tile' });
elems.StoriesGrid.item.title = new Entity({ block: 'story-tile', elem: 'title' });

// Story
// Открытый модальный блок со сторями
elems.StoryModal = new Entity({ block: 'story-modal', mod: 'visible' });

// Видимая сторя (блок с несколькими слайдами)
elems.StoryModal.story = new Entity({ block: 'stories-modal', elem: 'item-wrapper', mod: 'active' });

// Видимый слайд стори
elems.StoryModal.story.storyItem = new Entity({ block: 'story', elem: 'story-item-wrapper', mod: 'active' });
elems.StoryModal.story.storyItem.content = new Entity({ block: 'story-item-custom', elem: 'content' });
elems.StoryModal.story.storyItem.content.button = new Entity({ block: 'story-button', elem: 'area' });
elems.StoryModal.story.storyItem.content.background = new Entity({ block: 'story-item-custom', elem: 'background' });
elems.StoryModal.story.storyItem.closeButton = new Entity({ block: 'story-item-wrapper', elem: 'close-icon-wrapper' });

// TimeRoll
elems.TimeRoll = new Entity({ block: 'time-roll' });
elems.TimeRoll.hour = new Entity({ block: 'time-roll', elem: 'hour-roll' });
elems.TimeRoll.minute = new Entity({ block: 'time-roll', elem: 'minute-roll' });

// Настройки колонки
elems.SpeakerSettings = new Entity({ block: 't-speaker-settings' });
elems.SpeakerSettingsZigbeeList = new Entity({ block: 't-speaker-settings', elem: 'zigbee-list' });
elems.SpeakerSettingsZigbeeItem = new Entity({ block: 't-speaker-settings', elem: 'zigbee-item' });

// Настройки устройства: Об устройстве
elems.DeviceEditInfoParentItem = new Entity({ block: 't-iot-device-edit-info', elem: 'parent-device-item' });

// zigbee-wizard
elems.ZigbeeWizard = new Entity({ block: 'zigbee-wizard' });
elems.ZigbeeWizard.PageLayout = new Entity({ block: 'page-layout' });
elems.ZigbeeWizard.DeviceListLink = new Entity({ block: 'zigbee-wizard-welcome', elem: 'link' });
elems.ZigbeeWizard.SearchOneDeviceButton = new Entity({ block: 'button_theme_primary' });
elems.ZigbeeWizard.ProgressAnimation = new Entity({ block: 'zigbee-wizard-progress-circle' });

module.exports = {
    loadPageObject() {
        return create(elems);
    },
};
