const { Entity } = require('../../../../vendors/hermione');
const { showcase } = require('../../../../../hermione/page-objects/common/blocks');
const { sitelinks, organic } = require('../../../../../hermione/page-objects/common/construct/organic');

const fastres = new Entity({ block: 't-construct-adapter', elem: 'fastres' });
fastres.organic = organic.copy();
fastres.greenurl = organic.greenurl.copy();
fastres.extendedText = new Entity({ block: 'extended-text' });
fastres.extendedText.link = new Entity({ block: 'link' });
fastres.showcase = showcase.copy();
fastres.app = new Entity({ block: 'InfoSection' });
fastres.sitelinks = sitelinks.copy();

module.exports = { fastres };
