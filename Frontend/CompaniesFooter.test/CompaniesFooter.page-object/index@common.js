const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg: oneOrgBase } = require('../../../../Companies.test/Companies.page-object/index@common');

const oneOrg = oneOrgBase.mods({ 't-mod': '1org' });
const footer = new ReactEntity({ block: 'ObjectFooter' });
footer.owner = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'owner' });
footer.feedback = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'feedback' });
footer.travel = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'travel' });
footer.source = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'source' });
footer.sprav = new ReactEntity({ block: 'Link' }).mods({ 't-mod': 'sprav' });

oneOrg.footer = footer.copy();

const elems = {
    oneOrg,
    footer,
};

module.exports = elems;
