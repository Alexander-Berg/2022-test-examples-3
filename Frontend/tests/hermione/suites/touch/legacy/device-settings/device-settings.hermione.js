const { Light, Socket } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');

describe('Устройство - настройки', () => {
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
        await browser.yaOpenPage(`iot/device/${device.id}/edit`, PO.NavbarButtonRemove());
        await browser.yaAssertView('plain', 'body');

        await browser.yaWaitForVisibleAndClick(PO.NavbarButtonRemove());
        await browser.waitForVisible(PO.ConfirmContent(), 10_000);
        await browser.yaAssertView('modal', 'body');
    });
});
