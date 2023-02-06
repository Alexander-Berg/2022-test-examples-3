const { Light, Socket } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');

describe('Устройство - настройки', () => {
    afterEach(async function cleaner() {
        const { browser } = this;

        // Очищаем устройства с пользователя
        await browser.yaUnlinkDevices();
        // Удаляем все комнаты
        await browser.yaRemoveRooms();
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

        // Добавляем второе имя устройству
        await browser.onRecord(
            async() => await api.iot.devices.addDeviceName({ id: device.id, name: Array(25).fill('Ы').join('') })
        );

        // Открываем настройки устройства и смотрим
        // - Настройки устройства (название)
        // - Настройки устройства (конфирм с удалением)
        await browser.yaOpenPage(`iot/device/${device.id}/edit`);
        await browser.waitForVisible(PO.NavbarButtonRemove(), 10_000);
        await browser.assertView('plain', 'body');

        await browser.yaWaitForVisibleAndClick(PO.NavbarButtonRemove());
        await browser.waitForVisible(PO.ConfirmContent(), 10_000);
        await browser.assertView('modal', 'body');
    });
});
