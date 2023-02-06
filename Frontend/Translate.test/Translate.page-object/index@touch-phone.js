const { ReactEntity } = require('../../../../../vendors/hermione');

const commonElems = require('./index@common');

const elems = commonElems();

elems.translate.camera = new ReactEntity({ block: 'TranslateOcr', elem: 'ButtonWrapper' }).nthChild(1);
elems.translate.gallery = new ReactEntity({ block: 'TranslateOcr', elem: 'ButtonWrapper' }).nthChild(2);

module.exports = elems;
