// COPYPASTE https://a.yandex-team.ru/arc/trunk/arcadia/frontend/projects/web4/hermione/commands/commands-generic/common/yaMockXHR.js?rev=r7929694

/**
 * @see .config/kotik/testing/assets/mockXHR.js
 */
module.exports = function(options) {
    return this.execute(function(opts) {
        // пробрасывается из .config/kotik/testing/assets/mockXHR.js
        window.hermione.injectXhrMock(opts);
    }, options);
};
