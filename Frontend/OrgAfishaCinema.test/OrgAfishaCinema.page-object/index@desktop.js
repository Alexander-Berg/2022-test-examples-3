const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@desktop');
const { orgAfishaCinemaList } = require('../../../../../../components/OrgAfishaCinemaList/OrgAfishaCinemaList.test/OrgAfishaCinemaList.page-object');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.orgAfishaCinemaList = orgAfishaCinemaList.copy();

module.exports = elems;
