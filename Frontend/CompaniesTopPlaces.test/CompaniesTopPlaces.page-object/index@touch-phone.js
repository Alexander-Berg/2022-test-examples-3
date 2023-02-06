const { ReactEntity } = require('../../../../../../vendors/hermione');
const { drawer } = require('../../../../../../components/Drawer/Drawer.test/Drawer.page-object/index@touch-phone');
const { OrgsList } = require('../../../OrgsList/OrgsList.test/OrgsList.page-object/index@touch-phone');
const { overlayOneOrg, overlayIframe } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { orgsMap } = require('../../../../Companies.components/OrgsMap/OrgsMap.test/OrgsMap.page-object/index@common');

const elems = {};

elems.companiesTopPlaces = new ReactEntity({ block: 'CompaniesTopPlaces' });
elems.companiesTopPlaces.title = new ReactEntity({ block: 'CompaniesTopPlaces', elem: 'Title' });
elems.companiesTopPlaces.title.text = new ReactEntity({ block: 'CompaniesTopPlaces', elem: 'TitleText' });
elems.companiesTopPlaces.subtitle = new ReactEntity({ block: 'CompaniesTopPlaces', elem: 'Subtitle' });
elems.companiesTopPlaces.help = new ReactEntity({ block: 'CompaniesTopPlaces', elem: 'Help' });
elems.companiesTopPlaces.List = OrgsList.copy();
elems.companiesTopPlaces.List.Item11 = OrgsList.Item.nthChild(11);
elems.companiesTopPlaces.Map = orgsMap.copy();
elems.companiesTopPlacesHelpDrawer = drawer.copy();
elems.companiesTopPlacesHelpDrawer.button = new ReactEntity({ block: 'CompaniesTopPlaces', elem: 'HelpButton' });
elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayIframe = overlayIframe.copy();

elems.michelinModal = new ReactEntity({ block: 'MichelinModal' });
elems.michelinModal.button = new ReactEntity({ block: 'MichelinModal', elem: 'Button' });

module.exports = elems;
