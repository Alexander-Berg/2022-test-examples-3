describe('Устройство - добавление', () => {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Длинное название дома', async function() {
        const { browser } = this;

        await browser.yaLoginWritable();
        await browser.yaAddHousehold(new Array(20).fill('Щ').join(''));
        await browser.yaOpenPage('iot/add/devices.types.light/household-select/list');
        await browser.yaAssertView('plain', 'body');
    });
});
