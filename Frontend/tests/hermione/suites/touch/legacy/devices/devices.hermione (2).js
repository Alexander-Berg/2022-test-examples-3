const { Light, Socket } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');

describe('Устройства', () => {
    afterEach(async function cleaner() {
        const { browser } = this;

        await browser.yaRemoveHouseholds();
    });
    it('Длинное название устройства', async function() {
        const { browser, PO } = this;
        const api = Api(browser);

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.authAnyOnRecord('with-devices');
        // Добавляем устройства пользователю
        await browser.yaAddDevices([
            new Light(Array(25).fill('Ш').join('')),
            new Socket(Array(25).fill('Ж').join('')),
        ]);

        await browser.yaOpenPage('promo');

        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];

        // Создаем комнату и добавляем туда первое из устройств
        await browser.yaAddRoom('Комнатка', household.id, [device.id]);

        // Открываем список устройств и смотрим:
        // - Список устройств
        // - Список устройств (устройство без комнаты)
        await browser.yaOpenPage('iot');
        await browser.waitForVisible(PO.IotHome.DevicesList(), 10_000);
        await browser.assertView('plain', 'body');

        // Очищаем устройства с пользователя
        await browser.yaUnlinkDevices();
        // Удаляем все комнаты
        await browser.yaRemoveRooms();
    });
});
