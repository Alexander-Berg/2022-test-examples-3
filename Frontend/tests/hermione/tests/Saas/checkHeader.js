const { assert } = require('chai');
const paths = require('./paths');
const getUrl = require('../../utils/getUrl');

module.exports = async function() {
    const { browser } = this;

    await browser.url(getUrl() + '/service/direct/saas');
    await browser.pause(2000);
    const headerElement = await browser.$(paths.pagePath + paths.header);
    const header = await headerElement.getText();

    assert.equal(header, 'SaaS');
};
