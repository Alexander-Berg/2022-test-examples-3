const { Light, Socket } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');
const { UserStorageKey } = require('../../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../../helpers/user-storage/types');

describe('Устройство - голосовые команды', () => {
    it('Длинное название устройства', async function() {
        const { browser, PO } = this;
        const api = Api(browser);

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.yaLoginWritable();
        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(Array(25).fill('Ш').join('')),
            new Socket(Array(25).fill('Ж').join('')),
        ]);

        await browser.yaUpdateUserStorage({
            [UserStorageKey.FAVORITES_STAR_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];

        // Создаем комнату и добавляем туда первое из устройств
        await browser.yaAddRoom('Комнатка', household.id, [device.id]);

        // Открываем страницу устройства, таб голосовых команд и смотрим:
        // - Экран устройства (навбар)
        // - Экран устройства (вкладка с голосовыми саджестами)
        await browser.yaOpenPage(`iot/device/${device.id}`, PO.IotDevice.Settings());
        await browser.yaClickToSelectorByText('button', 'Голосовые команды');
        await browser.waitForVisible(PO.IotDevice.Voice(), 10_000);
        await browser.yaAssertView('plain', 'body');
    });
});
