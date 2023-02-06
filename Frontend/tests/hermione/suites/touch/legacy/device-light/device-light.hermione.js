const { LightWithScenes } = require('../../../../devices');
const { Api } = require('../../../../helpers/api');
const { UserStorageKey } = require('../../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../../helpers/user-storage/types');

describe('Устройство - лампочка', () => {
    it('Страница лампочки', async function() {
        const browser = this.browser;
        const api = Api(browser);

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.yaLoginWritable();

        // Добавляем устройства пользователю
        await browser.yaAddDevices([new LightWithScenes()]);

        // Получаем последний добавленное устройство
        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];

        await browser.yaUpdateUserStorage({
            [UserStorageKey.FAVORITES_STAR_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        // переходим на страницу устройства
        await browser.yaOpenPage(`iot/device/${device.id}`);
        await browser.yaAssertView('plain', 'body');
    });

    it('Вкладка "Режимы" на странице лампочки', async function() {
        const browser = this.browser;
        const api = Api(browser);

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.yaLoginWritable();

        // Добавляем устройства пользователю
        await browser.yaAddDevices([new LightWithScenes()]);

        // Удаляем всплывающее окошко во вкладке "Режимы"
        await browser.yaUpdateUserStorage({
            [UserStorageKey.IOT_SCENE_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        // Получаем информацию о только что созданном девайсе
        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];

        // переходим на страницу устройства
        await browser.yaOpenPage(`iot/device/${device.id}`);
        // переходим во вкладку "Режимы"
        await browser.yaClickToSelectorByText('button', 'Режимы');

        await browser.yaAssertView('plain', 'html');

        await browser.yaAssertView('scenes', '.iot-scenes', { selectorToScroll: '.app__wrapper' });
    });
});
