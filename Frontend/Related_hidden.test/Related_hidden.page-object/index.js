const { create } = require('../../../../../vendors/hermione');

const PO = {
    common: require('./index@common'),
    desktop: require('./index@desktop'),
    'touch-phone': require('./index@touch-phone'),
};

/**
 * Возвращает сформированные PO для указанной платформы.
 *
 * @param {String} platform
 *
 * @returns {Object}
 */
module.exports = function(platform) {
    return create(PO[platform]);
};
