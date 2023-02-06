const { Light, Socket } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');
const { UserStorageDynamicKeys } = require('../../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../../helpers/user-storage/types');
const { UserStorageKey } = require('../../../../helpers/user-storage/keys');

describe('Устройства', () => {
    // todo: со временем отваливается синяя точка, ее тут проверить нужно, но надо понять, как
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Длинное название устройства', async function() {
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
            [UserStorageDynamicKeys.STORY_VIEW('iot-news-feed', 'app')]: {
                type: UserStorageType.BOOL,
                value: false,
            },
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];

        // Создаем комнату и добавляем туда первое из устройств
        await browser.yaAddRoom('Комнатка', household.id, [device.id]);

        // Открываем список устройств и смотрим:
        // - Список устройств
        // - Список устройств (устройство без комнаты)
        await browser.yaOpenPage('', PO.IotHome.DevicesList());
        await browser.yaAssertView('plain', 'body');
    });
});
