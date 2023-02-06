const {
    format,
    parse,
} = require('url');

/**
 * Подготовка страницы к тестированию
 * Инициализирует дампы, прячет каретку
 * Чистит localstorage
 * Фризит дату если переданы соответствующие параметры
 * @param {String} url Страница, на которую надо перейти
 * @param {Array<Number>} timeArr - настройки для datetime, который надо зафризить
 * @returns {Promise<*>}
 */
module.exports = function preparePageExtended(url, timeArr) {
    if (timeArr) {
        const parsed = parse(url, true);

        parsed.query['hermione-mock-date'] = new Date(...timeArr).toISOString();

        return this
            .initDumps()
            .url(format(parsed))
            .hideCaret();
    }

    return this
        .initDumps()
        .url(url)
        .hideCaret();
};
