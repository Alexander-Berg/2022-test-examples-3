const { ReactEntity, Entity } = require('../../../../vendors/hermione');

const elems = {};

elems.statefulSerpItem = new Entity({ block: 't-construct-adapter', elem: 'stateful' });
elems.statefulSerpItem.stateful = new ReactEntity({ block: 'Stateful' });
elems.statefulSerpItem.stateful.title = new ReactEntity({ block: 'Stateful', elem: 'Title' });
elems.statefulSerpItem.stateful.titleLink = new ReactEntity({ block: 'Stateful', elem: 'TitleLink' });
elems.statefulSerpItem.stateful.promoLink = new ReactEntity({ block: 'Stateful', elem: 'Promo' });
elems.statefulSerpItem.stateful.moreLink = new ReactEntity({ block: 'Stateful', elem: 'MoreLink' });
elems.statefulSerpItem.stateful.secondButton = new ReactEntity({ block: 'StatefulButton' }).nthChild(2);
elems.statefulSerpItem.stateful.secondTab = new ReactEntity({ block: 'StatefulTabsMenu', elem: 'Tab' }).nthChild(2);
elems.statefulSerpItem.stateful.activeSecondTab = new ReactEntity({ block: 'TabsMenu', elem: 'Tab' })
    .mods({ active: true }).nthChild(2);

elems.statefulDrawer = new ReactEntity({ block: 'Stateful', elem: 'Drawer' });
elems.statefulDrawer.CollapserOpened = new ReactEntity({ block: 'Collapser' }).mods({ opened: true });
elems.statefulDrawer.StatefulGroupFirst = new ReactEntity({ block: 'StatefulGroup' }).mods({ first: true });
elems.statefulDrawer.StatefulGroupFirst.Link = new ReactEntity({ block: 'StatefulQuestion' });
elems.statefulDrawer.StatefulGroupLast = new ReactEntity({ block: 'StatefulGroup' }).mods({ last: true });
elems.statefulDrawer.StatefulGroupFirst.Label = new ReactEntity({ block: 'Collapser', elem: 'Label' });
elems.statefulDrawer.StatefulGroupLast.Label = new ReactEntity({ block: 'Collapser', elem: 'Label' });
elems.statefulDrawer.StatefulGroupLast.Button = new ReactEntity({ block: 'StatefulGroup', elem: 'Button' });

elems.header = new Entity({ block: 'HeaderPhone' });
elems.main = new Entity({ block: 'main' });

module.exports = elems;
