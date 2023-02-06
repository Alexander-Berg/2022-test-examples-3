const { contentMain } = require('../../../../../../hermione/page-objects/touch-phone');
const common = require('./index@common');

const elems = { ...common };

elems.contentMain = contentMain.copy();

module.exports = elems;
