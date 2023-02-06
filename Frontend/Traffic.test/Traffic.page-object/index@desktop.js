const blocks = require('../../../../../hermione/page-objects/common/blocks');
const common = require('./index@common');

const elems = common();

elems.traffic.organic = blocks.organic.copy();
elems.traffic.title = blocks.organic.title.copy();
elems.traffic.greenurl = blocks.organic.path.copy();
elems.traffic.semaphore.link = blocks.link.copy();

module.exports = elems;
