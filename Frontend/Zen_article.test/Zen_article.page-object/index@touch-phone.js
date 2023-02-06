const { Entity } = require('../../../../../vendors/hermione');
const blocks = require('../../../../../../hermione/page-objects/common/blocks');

const elems = {};
elems.zenArticle = new Entity({ block: 't-construct-adapter', elem: 'zen' });
elems.zenArticle.organic = blocks.organic.copy();

module.exports = elems;
