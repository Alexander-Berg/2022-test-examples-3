const NoJSErrors = require('../libs/no-js-errors');

module.exports = async function yaCheckClientErrors() {
    /**
     * Проверяет наличие на странице клиентских ошибок.
     * Данную команду нужно вызывать в конце сценария, содержащего пользовательские действия.
     */
    await this.yaExpectOnPage({ create: function (browser) {
        return [new NoJSErrors(browser)];
    } });
};
