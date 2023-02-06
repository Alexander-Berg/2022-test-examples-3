const { Entity, ReactEntity } = require('../../../../vendors/hermione');
const { relatedButton } = require('../../../../components/Related/RelatedButton/RelatedButton.test/RelatedButton.page-object/index@common');
const { drawer } = require('../../../../components/Drawer/Drawer.test/Drawer.page-object/index@touch-phone');
const ecomFact = require('../../../../components/Fact/ECFragment/Fact-ECFragment.test/Fact-ECFragment.page-object/index@common.js').fact;

const elems = {};

elems.feedbackDialog = new ReactEntity({ block: 'FeedbackDialog' });
elems.richFactVerifiedTooltip = new ReactEntity({ block: 'Verified', elem: 'Tooltip' });
elems.richFact = new Entity({ block: 't-construct-adapter', elem: 'rich-fact' });
elems.factHeader = new ReactEntity({ block: 'FactHeader' });
elems.factHeaderHint = new ReactEntity({ block: 'FactHeaderHint' });
elems.searchResultsHeader = new ReactEntity({ block: 'SearchResultsHeader' });
elems.drawer = new ReactEntity({ block: 'Drawer' });
elems.modal = new ReactEntity({ block: 'Modal' });
elems.header3 = new Entity({ block: 'HeaderPhone' });
elems.serpHeader = new Entity({ block: 'serp-header' });
elems.main = new Entity({ block: 'main' });
elems.navigation = new Entity({ block: 'serp-navigation' });
elems.richFact.fold = new ReactEntity({ block: 'Fold' });
elems.richFact.foldUnfolded = elems.richFact.fold.mods({ unfolded: true });
elems.richFact.fold.more = new ReactEntity({ block: 'Fold', elem: 'More' });
elems.richFact.link = new ReactEntity({ block: 'Link' });
elems.richFact.sourceLink = new ReactEntity({ block: 'Fact', elem: 'ECSourceLink' });
elems.richFact.sourceLink.link = new ReactEntity({ block: 'Link' });
elems.richFact.collapserLabel = new ReactEntity({ block: 'Collapser', elem: 'Label' });
elems.richFact.collapser = new ReactEntity({ block: 'Collapser' });
elems.richFact.collapserOpened = elems.richFact.collapser.mods({ opened: true });
elems.richFact.chapter = new ReactEntity({ block: 'Fact', elem: 'Chapter' });
elems.richFact.ECThumb = new ReactEntity({ block: 'Fact', elem: 'ECThumb' });
elems.richFact.relatedButton = relatedButton.copy();
elems.richFact.source = new ReactEntity({ block: 'Fact', elem: 'Source' });
elems.richFact.title = new ReactEntity({ block: 'Fact', elem: 'Title' });
elems.richFact.verified = new ReactEntity({ block: 'Fact', elem: 'Verified' });
elems.richFact.listCollapserToggle = new ReactEntity({ block: 'Fact-ECListCollapserToggle' });
elems.richFact.reportButton = new ReactEntity({ block: 'ExtraActions', elem: 'ReportItem' });

elems.ecom = ecomFact.copy();

elems.ecom.FilterSelectorButton = new ReactEntity({ block: 'Fact', elem: 'FilterSelectorButtonWrapper' })
    .descendant(new ReactEntity({ block: 'Button2' }));
elems.FilterDrawer = drawer.copy();
elems.FilterDrawer.Filter = new ReactEntity({ block: 'Fact', elem: 'FilterSelectorCheckbox' });
elems.FilterDrawer.Filter.Label = new ReactEntity({ block: 'Checkbox', elem: 'Label' });
elems.FilterDrawer.firstFilter = elems.FilterDrawer.Filter.nthType(1);
elems.FilterDrawer.More = new ReactEntity({ block: 'Fact', elem: 'FilterSelectorMore' });
elems.FilterDrawer.submit = new ReactEntity({ block: 'Fact', elem: 'FilterSelectorSubmit' });

module.exports = elems;
