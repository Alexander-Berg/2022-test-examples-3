const url = require('url');

/**
 * Добавляет к урлу test-id с контрольной группой, чтобы не попадать в эксперименты
 *
 * @param {string} inputUrl
 * @returns {string}
 */
const getUrlWithControlTestId = (inputUrl) => {
    const parsedUrl = url.parse(inputUrl, true);
    parsedUrl.query['test-id'] = '76858';
    delete parsedUrl.search;
    return url.format(parsedUrl);
};

module.exports = {
    getUrlWithControlTestId
};
