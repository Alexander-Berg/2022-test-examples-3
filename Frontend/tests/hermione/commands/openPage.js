'use strict';
const cookieDomains = require('../constants/cookieDomains');
const getBaseUrl = require('../helpers/getBaseUrl');

/**
 * Подготавливает страницу к тестированию
 *
 * Инициализирует дампы, открывает страницу, прячет каретку ввода.
 *
 * @this {Object}
 * @param {String} slug Короткое, уникальное в рамках файла, имя
 * @param {String} url Адрес открываемой страницы
 * @param {String} env Окружение, в котором запускается тест
 * @returns {Promise<*>}
 */
module.exports = function openPage(slug = '', url, env = 'internal') {
    const cookieDomain = cookieDomains[env];
    const baseUrl = getBaseUrl(env);

    return this.initDumpsClever(slug, {
        name: 'dumps_test_path',
        domain: cookieDomain,
        path: '/',
    }, 'test-data')
        .url(`${baseUrl}${url}`)
        .hideCaret();
};
