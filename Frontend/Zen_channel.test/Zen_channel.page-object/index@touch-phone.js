const { ReactEntity } = require('../../../../../vendors/hermione');
const { link } = require('../../../../../components/Link/Link.test/Link.page-object/index@common');

const elems = {};
elems.zenChannel = new ReactEntity({ block: 'ZenChannel' });
elems.zenChannel.title = new ReactEntity({ block: 'OrganicTitle' });
elems.zenChannel.title.link = link.copy();
elems.zenChannel.zenHeader = new ReactEntity({ block: 'ZenHeader' });
elems.zenChannel.zenHeader.thumb = new ReactEntity({ block: 'ZenHeader-Thumb' });
elems.zenChannel.zenHeader.link = link.copy();

module.exports = elems;
