const { generatePath } = require('react-router');
const { YandexStationMidi } = require('../../../speakers');

describe('Zigbee', () => {
    it('Флоу подключения через раздел добавления устройств', async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();

        const station = new YandexStationMidi();
        await browser.yaAddSpeakers([station]);

        await browser.yaOpenPage(generatePath('iot/add'), PO.IotAdd.ZigbeeButton());
        await browser.click(PO.IotAdd.ZigbeeButton());

        await browser.waitForVisible(PO.ZigbeeWizard());
        await browser.yaAssertView('zigbee-1', PO.ZigbeeWizard());

        await browser.click(PO.ZigbeeWizard.SearchOneDeviceButton());
        await browser.waitForVisible(PO.ZigbeeWizard.ProgressAnimation());
        await browser.yaAssertView('zigbee-2', PO.ZigbeeWizard.PageLayout(), { ignoreElements: [PO.ZigbeeWizard.ProgressAnimation()] });
    });

    it('Флоу подключения через раздел добавления устройств (список поддерживаемых устройств)', async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();

        const station = new YandexStationMidi();
        await browser.yaAddSpeakers([station]);

        await browser.yaOpenPage(generatePath('iot/add'), PO.IotAdd.ZigbeeButton());
        await browser.click(PO.IotAdd.ZigbeeButton());

        await browser.waitForVisible(PO.ZigbeeWizard());

        await browser.click(PO.ZigbeeWizard.DeviceListLink());
        await browser.yaAssertViewBottomSheet('zigbee-3', PO.BottomSheetWrapper());
    });
});
