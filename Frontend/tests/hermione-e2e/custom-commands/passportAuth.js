'use strict';

/**
 * Заполняет форму и логинется на passport.yandex-team.ru.
 *
 * @param {String} login - Логин
 * @param {String} pass - Пароль
 *
 * @returns {Promise}
 */

const FORM_SELECTOR = '.passport-Domik-Form';
const PASPORT_URL = 'passport.yandex-team.ru';

module.exports = function(login, pass) {
    return this
        .url('/')
        .then(function() {
            return this
                .getUrl()
                .then((url) => {
                    return url.match(PASPORT_URL)
                        ? this
                            .waitForExist(FORM_SELECTOR, 5000)
                            .setValue(`${FORM_SELECTOR} [name=login]`, login)
                            .setValue(`${FORM_SELECTOR} [name=passwd]`, pass)
                            .click(`${FORM_SELECTOR} .passport-Button`)
                        : this;
                });
        });
};
