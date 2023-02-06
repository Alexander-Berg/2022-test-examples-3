const { Light } = require('../../../devices');
const { Api } = require('../../../helpers/api');
const { YandexStationMidi } = require('../../../speakers');
const { generatePath } = require('react-router');

describe('Zigbee', () => {
    it('Просмотр родительского хаба из экрана настройки устройства', async function() {
        const { browser, PO } = this;
        const api = Api(browser);

        await browser.yaLoginWritable();

        const station = new YandexStationMidi();
        const light = new Light();

        await browser.yaAddSpeakers([station]);
        await browser.yaAddDevicesYandexIO([new Light()], station.getId());

        const devicesResponse = await api.iot.devicesV3.getDevices();
        const householdDevices = devicesResponse.households[0].all;
        const lightId = householdDevices.find(device => light.name === device.name).id;

        await browser.yaOpenPage(generatePath('iot/device/:deviceId/edit/info', { deviceId: lightId }), PO.DeviceEditInfoParentItem());
        await browser.yaAssertView('parent-info-item', PO.DeviceEditInfoParentItem());
    });
});
