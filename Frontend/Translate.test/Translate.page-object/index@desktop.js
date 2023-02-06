const { ReactEntity } = require('../../../../../vendors/hermione');

const commonElems = require('./index@common');

const elems = commonElems();

elems.translate.dropZone = new ReactEntity({ block: 'TranslateOcr', elem: 'DropZone' });

module.exports = elems;
