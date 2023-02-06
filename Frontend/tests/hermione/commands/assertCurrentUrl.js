const URL = require('url');

/**
 * Проверяет тукещую ссылку
 *
 * @returns {Promise}
 */
module.exports = async function assertCurrentUrl(nameLink, urlLinkExpected) {
    const currentUrl = await this.getCurrentUrl();
    const url = URL.parse(currentUrl, true);
    assert.strictEqual(url.href, urlLinkExpected, `По клику на ссылку "${nameLink}" открывается урл "${url.href}", а должен "${urlLinkExpected}"`);
};
