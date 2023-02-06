const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg: oneOrgBase, popup } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { footer: footerBase } = require('./index@common');

const footer = footerBase.copy();
footer.adv = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'maps-adv' });
footer.addPhoto = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'photo' });
footer.realty = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'realty' });
footer.maps = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'maps' });

const oneOrg = oneOrgBase.copy();
oneOrg.footer = footer.copy();
popup.oneOrg.footer = footer.copy();

const elems = {
    oneOrg,
    popup,
};

module.exports = elems;
