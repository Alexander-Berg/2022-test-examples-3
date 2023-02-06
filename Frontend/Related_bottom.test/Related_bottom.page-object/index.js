const { create } = require('../../../../../vendors/hermione');
const { ReactEntity } = require('../../../../../vendors/hermione');
const { link } = require('../../../../../components/Link/Link.test/Link.page-object/index@common');
const { relatedButtonWithThumb } = require('../../../Related.test/Related.page-object/index@common');

const block = 'RelatedBottom';
const elems = {};

elems.relatedBottom = new ReactEntity({ block });
elems.relatedBottom.item = new ReactEntity({ block, elem: 'Item' });
elems.relatedBottom.header = new ReactEntity({ block: 'Related', elem: 'Header' });
elems.relatedBottom.itemFirst = elems.relatedBottom.item.nthChild(1);
elems.firstButtonWithoutColumn = elems.relatedBottom.itemFirst;
elems.relatedBottom.list = new ReactEntity({ block, elem: 'Items' });
elems.relatedBottom.items = elems.relatedBottom.list;
elems.relatedBottom.column = new ReactEntity({ block, elem: 'Column' });
elems.relatedBottom.firstItemLink = elems.relatedBottom.item.nthChild(1).descendant(link.copy());
elems.relatedBottom.buttonWithThumb = relatedButtonWithThumb.copy();
elems.relatedBottom.column = new ReactEntity({ block, elem: 'Column' });
elems.relatedBottom.firstColumn = elems.relatedBottom.column.nthType(1);
elems.relatedBottom.firstColumn.itemFirst = new ReactEntity({ block, elem: 'Item' }).nthChild(1);
elems.relatedBottom.firstColumn.firstItemLink = new ReactEntity({ block, elem: 'Item' }).nthChild(1).descendant(link.copy());
elems.relatedBottom.firstColumn.firstItemIcon = new ReactEntity({ block, elem: 'Icon' }).nthChild(1);
elems.relatedBottom.secondColumn = elems.relatedBottom.column.nthType(2);
elems.relatedBottom.secondColumn.itemFirst = new ReactEntity({ block, elem: 'Item' }).nthChild(1);
elems.relatedBottom.secondColumn.firstItemLink = new ReactEntity({ block, elem: 'Item' }).nthChild(1).descendant(link.copy());
elems.relatedBottom.secondColumn.firstItemIcon = new ReactEntity({ block, elem: 'Icon' }).nthChild(1);

module.exports = create(elems);
