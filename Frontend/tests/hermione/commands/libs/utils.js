const validateFields = require('../utils/').validateFields;

const VALIDATE_URL_ARGS_CONSTRAINTS = {
    allowed: ['href', 'ignore', 'queryValidator']
};

module.exports = {
    parseUrlArgs(url) {
        const result = {};

        if (typeof url === 'string') {
            result.url = url;

            return result;
        }

        if (!url.href && !url.queryValidator) {
            throw new Error(`parseUrlArg: объект ${JSON.stringify(url)} должен содержать поля href или queryValidator`);
        }

        validateFields(url, VALIDATE_URL_ARGS_CONSTRAINTS, 'parseUrlArg');

        result.url = url.queryValidator ? { url: url.href, queryValidator: url.queryValidator } : url.href;

        const skips = {};
        url.ignore && url.ignore.forEach(item => skips[item] = true);

        result.params = {
            skipProtocol: skips.protocol,
            skipHostname: skips.hostname,
            skipPathname: skips.pathname,
            skipQuery: skips.query,
            skipHash: skips.hash
        };

        return result;
    },

    checkCounter(browser, selector, counter, message) {
        return browser.yaCheckCounter(selector, counter, message);
    }
};
