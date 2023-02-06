const { create } = require('../../../../../../vendors/hermione');
const PO = require('./index@common');

/**
 * Возвращает сформированные PO для каждой платформы.
 *
 * @returns {Object}
 */
module.exports = create(PO);
