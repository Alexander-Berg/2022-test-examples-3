const { ReactEntity } = require('../../../../../../vendors/hermione');

const { scroller } = require('../../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');

const elems = {};

elems.visitsHistogram = new ReactEntity({ block: 'OrgVisitsHistogram' });
elems.visitsHistogram.scroller = scroller.copy();

module.exports = elems;
