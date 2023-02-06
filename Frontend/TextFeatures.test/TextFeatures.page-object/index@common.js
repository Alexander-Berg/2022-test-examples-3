const { ReactEntity } = require('../../../../../../vendors/hermione');
const { textCut } = require('../../../../../../components/TextCut/TextCut.test/TextCut.page-object/index@common');
const { oneOrgSection } = require('../../../../Companies.components/OneOrgSection/OneOrgSection.test/OneOrgSection.page-object/index@common');

const elems = {};

elems.textFeatures = new ReactEntity({ block: 'TextFeatures' });
elems.textFeatures.textCut = textCut.copy();
elems.textFeatures.title = oneOrgSection.title.mix({ block: 'TextFeatures' });

module.exports = elems;
