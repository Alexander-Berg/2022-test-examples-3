const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { orgAfishaCinemaList } = require('../../../../../../components/OrgAfishaCinemaList/OrgAfishaCinemaList.test/OrgAfishaCinemaList.page-object');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.orgAfishaCinemaList = orgAfishaCinemaList.copy();

module.exports = elems;
