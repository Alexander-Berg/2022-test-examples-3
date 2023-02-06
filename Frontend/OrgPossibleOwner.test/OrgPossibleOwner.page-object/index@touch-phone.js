const { possibleOwner } = require('../../../../../../components/PossibleOwner/PossibleOwner.test/PossibleOwner.page-object/index@common');
const { oneOrg } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');

const elems = {};

elems.oneOrg = oneOrg.copy();
elems.oneOrg.possibleOwner = possibleOwner.copy();

module.exports = elems;
