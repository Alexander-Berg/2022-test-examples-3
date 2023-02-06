const { Light } = require('../../../devices');
const { Api } = require('../../../helpers/api');
const { YandexStationMidi } = require('../../../speakers');
const { generatePath } = require('react-router');

describe('Zigbee', () => {
    it('Просмотр подключённых устройств из экрана настройки колонки', async function() {
        const { browser, PO } = this;
        const api = Api(browser);

        await browser.yaLoginWritable();

        const station = new YandexStationMidi();
        await browser.yaAddSpeakers([station]);
        await browser.yaAddDevicesYandexIO([new Light()], station.getId());

        const devicesResponse = await api.iot.devicesV3.getDevices();
        const householdDevices = devicesResponse.households[0].all;
        const stationId = householdDevices.find(device => device.quasar_info && device.quasar_info.device_id === station.getId()).id;

        await browser.yaOpenPage(generatePath('iot/speaker/:speakerId', { speakerId: stationId }), PO.SpeakerSettingsZigbeeList());

        await browser.yaAssertView('midi-settings-zigbee-item', PO.SpeakerSettingsZigbeeList());

        await browser.click(PO.SpeakerSettingsZigbeeItem());
        await browser.waitForVisible(PO.PageLayout(), 10_000);
        await browser.yaAssertView('midi-devices-zigbee-page', 'body');
    });
});
