const { LightWithScenes } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');
const { UserStorageKey } = require('../../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../../helpers/user-storage/types');

describe('Сценарии', () => {
    describe('Создание', () => {
        describe('Лампочки - режим', () => {
            afterEach(async function cleaner() {
                const { browser } = this;

                // Очищаем сценарии
                await browser.yaRemoveScenarios();
            });

            it('Выбрать режим свеча', async function() {
                // https://testpalm.yandex-team.ru/alice/testcases/3116

                const { browser, PO } = this;
                const api = Api(browser);

                await browser.yaLoginWritable();
                await browser.yaAddDevices([new LightWithScenes('Лампа')]);

                const devices = await api.iot.devicesV2.getDevices();
                const household = devices.households[0];
                const device = household.unconfigured_devices[0];

                await browser.yaAddRoom('Комнатка', household.id, [device.id]);

                await browser.yaUpdateUserStorage({
                    [UserStorageKey.IOT_SCENE_TOOLTIP_CLOSED]: {
                        type: UserStorageType.BOOL,
                        value: true,
                    },
                });

                // 1. Начать создавать сценарий
                // Пропущен: 2. Задать условие в "Если:", например срабатывать каждую среду в 15:00
                // 3. Добавить действие, из списка устройств выбрать Лампочку
                await browser.yaOpenPage('iot/scenario/add/action');
                await browser.yaWaitForVisibleAndAssertErrorMessage(
                    '.list-item',
                    10_000,
                    'Ни один вариант выбора не прогрузился за 10 секунд'
                );

                await browser.yaClickToListItemByText('Лампа');
                await browser.yaAssertView('plain', 'body');

                await browser.yaClickToSelectorByIndex(PO.Checkbox(), 1);
                await browser.yaAssertView('plain-with-color', 'body');

                // 4. Кликнуть на диаграмму выбора цветов
                await browser.yaWaitForVisibleAndClick(PO.ScenarioLightCapabilityEditColor());
                await browser.yaWaitBottomSheetAnimation();
                await browser.yaAssertView('colors', PO.BottomSheetContent());

                // 5. Раскроется шторка с доступными режимами лампочки "Цвета", "Режимы"
                await browser.yaClickTabThemeChipsByText('Режимы');
                await browser.yaAssertView('scenes', PO.BottomSheetContent());

                // 6. Выбрать режим
                await browser.yaClickToSelectorByText('.iot-scenes__name', 'свеча');

                await browser.yaAssertView('finish', 'body');
            });
        });
    });
});
