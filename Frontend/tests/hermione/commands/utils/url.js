const url = require('url');

/**
 * Добавляет query-параметр в распарсенный url
 * Модифицирует первый аргумент
 * @param {String} urlString - результат вызова url.parsed(val, true)
 * @param {String} name - имя query-параметра
 * @param {String} value - значение query-параметр
 * @returns {String}
 */
function appendQueryParameter(urlString, name, value) {
    const parsedUrl = url.parse(urlString, true);
    delete parsedUrl.search;

    const { query } = parsedUrl;

    // У свойства query нет Object.prototype в цепочке прототипов, поэтому не используем hasOwnProperty
    if (!(name in query)) {
        // Такого query-параметра еще не было
        query[name] = value;
    } else if (Array.isArray(query[name])) {
        // Более одного значения у query-параметра
        query[name].push(value);
    } else {
        // Было одно значение, преобразуем в массив
        query[name] = [query[name], value];
    }

    return url.format(parsedUrl);
}

module.exports = { appendQueryParameter };
