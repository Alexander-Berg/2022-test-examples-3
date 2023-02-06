const { assert } = require('chai');
const paths = require('./paths');
const getUrl = require('../../utils/getUrl');

module.exports = async function() {
    const { browser } = this;

    await browser.url(getUrl() + '/service/direct/search');
    await browser.pause(1000);

    const serviceSelect = await browser.$(paths.root + paths.serviceSelect);
    await serviceSelect.click();

    const pi = await browser.$(paths.root + paths.piServiceButton);
    await pi.click();

    const service = await serviceSelect.getText();

    assert.equal(service, 'Пифия');
};
