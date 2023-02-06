const path = require('path');
const translit = require('../helpers/transliterate.js');

/**
 * Создаёт путь для хранения данных теста
 * @param {string} dataDir Директория для хранения данных
 * @param {Object} test Объект теста
 */

module.exports = function createDataPath(dataDir, test) {
    // складываем дампы в test-data для git-lfs в монорепе
    let dir = dataDir === 'dumps' ? 'test-data' : dataDir;

    return path.join(
        path.dirname(test.file),
        translit(test.title).toLowerCase(),
        dir,
        test.browserId
    );
};
