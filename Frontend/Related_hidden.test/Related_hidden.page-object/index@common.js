const { ReactEntity } = require('../../../../../vendors/hermione');
const { EntityCarousel, serpItem, serpAdvItem } = require('../../../../../../hermione/page-objects/common/blocks');
const { organic } = require('../../../../../../hermione/page-objects/common/construct/organic');
const { link } = require('../../../../../components/Link/Link.test/Link.page-object/index@common');
const { scroller } = require('../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');
const { relatedButton, relatedButtonWithThumb } = require('../../../Related.test/Related.page-object/index@common');
const block = 'RelatedHidden';
const elems = {};

elems.relatedHidden = new ReactEntity({ block });
elems.relatedHidden.scroller = scroller.copy();
elems.relatedHidden.row = new ReactEntity({ block, elem: 'Row' });
elems.relatedHidden.firstRow = elems.relatedHidden.row.nthChild(1);
elems.relatedHidden.secondRow = elems.relatedHidden.row.nthChild(2);
elems.relatedHidden.item = new ReactEntity({ block, elem: 'Item' });
elems.relatedHidden.item.link = link.copy();
elems.relatedHidden.itemFirst = elems.relatedHidden.item.nthChild(1);
elems.relatedHidden.button = relatedButton.copy();
elems.relatedHidden.buttonWithThumb = relatedButtonWithThumb.copy();

elems.organic = organic.copy();
elems.EntityCarousel = EntityCarousel.copy();
elems.firstSnippet = serpItem.not(serpAdvItem).nthType(1);

module.exports = elems;
