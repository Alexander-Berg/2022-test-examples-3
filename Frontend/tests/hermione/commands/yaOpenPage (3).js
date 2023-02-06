const qs = require('query-string');

module.exports = function(path, qParams) {
    const queryParams = {
        ...qParams,
        'enable-test-cn': 1,
    };

    return this
        .url(`/quasar/${path}?${qs.stringify(queryParams)}`)
        .yaWaitForLoadPage();
};
