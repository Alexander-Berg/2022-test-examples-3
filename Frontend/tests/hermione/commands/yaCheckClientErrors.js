const NoJSErrors = require('./lib/expectations/no-js-errors');

module.exports = function yaCheckClientErrors() {
    /**
     * Проверяет наличие на странице клиентских ошибок.
     * Данную команду нужно вызывать в конце сценария, содержащего пользовательские действия.
     */
    return this.yaExpectOnPage({ create: function(browser) { return [new NoJSErrors(browser)] } });
};
