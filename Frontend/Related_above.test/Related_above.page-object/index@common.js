const { ReactEntity } = require('../../../../../vendors/hermione');
const { link } = require('../../../../../components/Link/Link.test/Link.page-object/index@common');
const { scroller } = require('../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');
const { relatedButton, relatedButtonWithThumb } = require('../../../Related.test/Related.page-object/index@common');

const block = 'RelatedAbove';
const elems = {};

elems.relatedAbove = new ReactEntity({ block });
elems.relatedAbove.wrap = new ReactEntity({ block, elem: 'Wrap' });
elems.relatedAbove.item = new ReactEntity({ block, elem: 'Item' });
elems.relatedAbove.itemFirst = elems.relatedAbove.item.nthType(1);
elems.relatedAbove.firstItemLink = elems.relatedAbove.itemFirst.child(link);
elems.relatedAbove.scroller = scroller.copy();
elems.relatedAbove.button = relatedButton.copy();
elems.relatedAbove.buttonWithThumb = relatedButtonWithThumb.copy();

module.exports = elems;
