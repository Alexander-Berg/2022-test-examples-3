describe('Устройство - добавление', () => {
    it('Длинное название дома', async function() {
        const { browser, PO } = this;

        await browser
            .authAnyOnRecord('with-devices')
            .yaOpenPage('promo')
            .yaAddHousehold(new Array(20).fill('Щ').join(''))
            .yaOpenPage('pairing/devices.types.smart_speaker.yandex.station/household-select/list')
            .assertView('plain', 'body');
    });
});
