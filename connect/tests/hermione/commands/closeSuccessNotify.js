/**
 * Закрывает сообщение об успешном действии
 *
 * @param {String} assertName
 * @returns {Promise}
 */
module.exports = function closeSuccessNotify(assertName) {
    return this
        // отображается сообщение
        .waitForVisible('.status-notification', 15000)
        .assertView(assertName, '.status-notification')
        // нажать на Крестик
        .click('.status-notification__close-button');
};
