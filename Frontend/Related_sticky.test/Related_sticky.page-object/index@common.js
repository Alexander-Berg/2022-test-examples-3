const { ReactEntity } = require('../../../../../vendors/hermione');
const { link } = require('../../../../../components/Link/Link.test/Link.page-object/index@common');
const { scroller } = require('../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');
const { relatedButton, relatedButtonWithThumb } = require('../../../Related.test/Related.page-object/index@common');
const elems = {};

elems.relatedSticky = new ReactEntity({ block: 'RelatedAbove' });
elems.relatedSticky.item = new ReactEntity({ block: 'RelatedAbove', elem: 'Item' });
elems.relatedSticky.itemFirst = elems.relatedSticky.item.nthType(1);
elems.relatedSticky.firstItemLink = elems.relatedSticky.itemFirst.child(link);
elems.relatedSticky.scroller = scroller.copy();
elems.relatedSticky.button = relatedButton.copy();
elems.relatedSticky.buttonWithThumb = relatedButtonWithThumb.copy();

module.exports = elems;
