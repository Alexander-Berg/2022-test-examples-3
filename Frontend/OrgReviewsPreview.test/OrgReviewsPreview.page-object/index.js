const { create } = require('../../../../../../vendors/hermione');

/**
 * Возвращает сформированные PO для указанной платформы.
 *
 * @param {String} platform
 *
 * @returns {Object}
 */
module.exports = function(platform) {
    return create(require(`./index@${platform}`));
};
