const { ReactEntity, Entity } = require('../../../../../../vendors/hermione');
const { extendedText } = require('../../../../../../components/ExtendedText/ExtendedText.test/ExtendedText.page-object/index@common');

const elems = {};

elems.OrgFeatures = extendedText.mix(new ReactEntity({ block: 'OrgFeatures' }));
elems.bcardSideBlock = new Entity({ block: 'side-block-bcard' });
elems.bcardSideBlock.OrgFeatures = elems.OrgFeatures.copy();
elems.companiesComposite = new Entity({ block: 't-construct-adapter', elem: 'companies' });
elems.companiesComposite.tabsMenu = new Entity({ block: 'tabs-menu' });
elems.companiesComposite.tabsMenu.about = new Entity({ block: 'link' }).mods({ 't-mod': 'about' });

module.exports = elems;
