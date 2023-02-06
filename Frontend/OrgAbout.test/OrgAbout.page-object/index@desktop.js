const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@desktop');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.OrgAbout = new ReactEntity({ block: 'OrgAbout' });
elems.oneOrg.OrgAbout.more = new ReactEntity({ block: 'OrgAbout', elem: 'More' });

module.exports = elems;
