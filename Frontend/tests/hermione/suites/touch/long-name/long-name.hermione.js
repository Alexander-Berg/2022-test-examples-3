const { Socket } = require('../../../devices');
const { Api } = require('../../../helpers/api');
const { UserStorageKey } = require('../../../helpers/user-storage/keys');
const { UserStorageType } = require('../../../helpers/user-storage/types');

describe('Длинное название устройства', () => {
    beforeEach(async function addDevice() {
        const { browser } = this;
        const api = Api(browser);

        // Авторизуемся под пользователем из группы 'with-devices'
        await browser.yaLoginWritable();

        // do: Добавить устройство с максимально допустимым количеством символов в одно слово
        await browser.yaAddDevices([
            new Socket(Array(25).fill('Ж').join('')),
        ]);

        await browser.yaUpdateUserStorage({
            [UserStorageKey.IOT_HOUSEHOLDS_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: true,
            },
        });

        const devices = await api.iot.devicesV2.getDevices();
        const household = devices.households[0];
        const device = household.unconfigured_devices[0];
        await browser.setMeta('deviceId', device.id);

        // do: Создаем комнату и добавляем туда первое из устройств
        await browser.yaAddRoom('Комнатка', household.id, [device.id]);

        // // Добавляем второе имя устройству
        // await browser.onRecord(
        //     async() => await api.iot.devices.addDeviceName({ id: device.id, name: Array(25).fill('Ы').join('') })
        // );
    });
    it('Список устройств', async function() {
        const { browser, PO } = this;

        // do: Открыть "Все" на главном экране
        await browser.yaOpenPage('', PO.IotHome());
        // screenshot: Имя устройства отображается в две строки https://jing.yandex-team.ru/files/osennikovak/uhura_2020-11-16T23%3A41%3A59.850663.jpg [plain]
        await browser.yaAssertView('plain', 'body');
    });
    it('Экран голосовых команд', async function() {
        const { browser, PO } = this;

        await browser.yaUpdateUserStorage({
            [UserStorageKey.FAVORITES_STAR_TOOLTIP_CLOSED]: {
                type: UserStorageType.BOOL,
                value: false,
            },
        });

        // do: Открыть экран голосовых команд
        const deviceId = await browser.getMeta('deviceId');
        await browser.yaOpenPage(`iot/device/${deviceId}`, PO.IotDevice.Settings());
        await browser.yaClickToSelectorByText('button', 'Голосовые команды');

        // screenshot: Команды с именем устройства отображаются в несколько строк, название не уезжает за края блока; https://jing.yandex-team.ru/files/osennikovak/uhura_2020-11-16T23%3A48%3A21.287643.jpg [plain]
        await browser.waitForVisible(PO.IotDevice.Voice(), 10_000);
        await browser.yaAssertView('plain', 'body');

        // do: Удалить комнату у устройства
        const rooms = await browser.getMeta('rooms', true);
        if (rooms) {
            await browser.yaRemoveRooms();
        }

        // screenshot: Имя устройства отображается в две строки [without-room]
        await browser.yaOpenPage(`iot/device/${deviceId}`, PO.IotDevice.Settings());
        await browser.yaClickToSelectorByText('button', 'Голосовые команды');
        await browser.yaAssertView('without-room', 'body');
    });
});
