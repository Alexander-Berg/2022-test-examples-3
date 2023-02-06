const api = {
    iot: require('./iot'),
};

/**
 * @param {{
 *     yaFetch: function(path, method = 'GET', body): Promise
 * }} browser
 *
 * @returns {{
 *     iot: {
 *         rooms: Rooms.methods,
 *         devicesV2: DevicesV2.methods,
 *         scenarios: Scenarios.methods,
 *         scenariosV3: ScenariosV3.methods,
 *         househols: Households.methods,
 *     }
 * }}
 * @constructor
 */
const Api = (browser) => {
    const result = {};

    Object.keys(api).forEach(backend => {
        result[backend] = {};
        Object.keys(api[backend]).forEach(namespace => {
            const apiInfo = api[backend][namespace];
            result[backend][namespace] = {};

            Object.keys(api[backend][namespace].methods).forEach(method => {
                result[backend][namespace][method] = async(props) => {
                    const methodInfo = apiInfo.methods[method](props);
                    const path = apiInfo.namespace + methodInfo.path;
                    const options = methodInfo.options || {};

                    return await browser.yaFetch(path, options.method, options.body);
                };
            });
        });
    });

    return result;
};

module.exports = {
    Api,
};
