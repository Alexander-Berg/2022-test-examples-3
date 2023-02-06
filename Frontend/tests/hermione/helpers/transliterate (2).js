const tr12n = require('transliteration');

const options = {
    replace: [
        [
            /[,"'.;:/\\]/g,
            '',
        ],
        [
            / /g,
            '_',
        ],
    ],
};
/**
 * Транслитерирует строку, вырезая знаки пунктуации и заменяя пробелы нижним подчеркиванием
 * @param {String} str
 * @returns {String}
 */
module.exports = function transliterate(str) {
    return tr12n.transliterate(str, options).toLowerCase();
};
