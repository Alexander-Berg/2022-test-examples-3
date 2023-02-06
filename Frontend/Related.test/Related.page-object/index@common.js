const { ReactEntity } = require('../../../../vendors/hermione');
const elems = {};

elems.relatedButton = new ReactEntity({ block: 'Related', elem: 'Button' });
elems.relatedButtonWithThumb = elems.relatedButton.mods({ withThumb: true });

module.exports = elems;
