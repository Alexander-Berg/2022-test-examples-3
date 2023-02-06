const CODES = {
    ARROW_LEFT: '\uE012',
    ARROW_UP: '\uE013',
    ARROW_RIGHT: '\uE014',
    ARROW_DOWN: '\uE015',
    ENTER: '\uE007',
    ESC: '\uE00C',
    BACKSPACE: '\uE003',
    TAB: '\uE004',
    PLUS: '\uE025',
    SHIFT: '\ue008',
    HOME: '\uE011',
};
const DELIMETER = '\u0007<"\'';

/**
 * Управление клавиатурой
 *
 * @param {...String|String[]} codes - коды клавиш отдельными аргументами или массив кодов клавиш
 * @returns {Promise}
 */
module.exports = async function(codes) {
    if (!Array.isArray(codes)) {
        codes = Array.prototype.slice.call(arguments);
    }

    for (const code of codes) {
        if (code.startsWith('Shift+')) {
            await this.addValue('body', code.replace('+', DELIMETER).split(DELIMETER));
        } else {
            await this.keys(CODES[code] || code);
        }
    }
};
