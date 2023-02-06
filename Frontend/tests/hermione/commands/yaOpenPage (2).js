const qs = require('query-string');

module.exports = async function yaOpenPage(path, target, qParams) {
    const expFlags = await this.getMeta('exp-flags');
    const queryParams = {
        ...qParams,
        'enable-test-cn': 1,
        'test-exp-flags': expFlags,
        'srcrwr': 'QUASAR_HOST:testing.quasar.yandex.ru',
    };

    await this.url(`/iot/${path}?${qs.stringify(queryParams)}`);
    await this.yaWaitForLoadPage();

    if (target) {
        await this.waitForVisible(target, 10_000);
    }
};
