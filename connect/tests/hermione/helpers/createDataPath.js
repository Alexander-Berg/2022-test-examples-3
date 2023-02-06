const path = require('path');

/**
 * Создаёт путь для хранения данных теста
 * @param {String} dataDir Директория для хранения данных
 * @param {Object} test Объект теста
 * @param {Boolean} browser Включать ли в путь имя браузера
 * @returns {String}
 */
module.exports = function createDataPath(dataDir, test, browser) {
    let slug = test.ctx.slug;

    const matches = /\/[\/\*]\s*alias:\s*(\S+)/.exec(test.body);

    if (matches) {
        slug = matches[1];
    }

    return path.join(
        path.dirname(test.file),
        path.basename(test.file, '.js'),
        slug ? `${slug}-${test.id()}` : test.id(),
        dataDir,
        browser ? test.browserId : ''
    );
};
