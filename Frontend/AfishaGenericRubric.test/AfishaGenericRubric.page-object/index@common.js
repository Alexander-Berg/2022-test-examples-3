const { Entity } = require('../../../../../../vendors/hermione');
const { Organic } = require('../../../../../../features/Organic/Organic.test/Organic.page-objects');
const { scroller } = require('../../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');
const { more } = require('../../../../../../components/LinkMore/LinkMore.test/LinkMore.page-object/index@common');
const { link } = require('../../../../../../components/Link/Link.test/Link.page-object/index@common');

const elems = {};

elems.afishaGenericRubric = new Entity({ block: 't-construct-adapter', elem: 'afisha-generic-rubric' });

elems.afishaItem = new Entity({ block: 'AfishaItem' });
elems.afishaItem.link = link.copy();

elems.afishaGenericRubric.Organic = Organic.copy();
elems.afishaGenericRubric.scroller = scroller.copy();
elems.afishaGenericRubric.more = more.copy();
elems.afishaGenericRubric.item = elems.afishaItem.copy();
elems.afishaGenericRubric.firstItem = elems.afishaItem.nthChild(1);

module.exports = elems;
