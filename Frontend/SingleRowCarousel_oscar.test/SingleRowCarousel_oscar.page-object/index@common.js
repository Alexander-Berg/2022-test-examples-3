const { specialEvent } = require('../../../../../../hermione/page-objects/common');
const { ReactEntity } = require('../../../../../vendors/hermione');
const { showcase } = require('../../../../../components/Showcase/Showcase.test/Showcase.page-object/index@common');

const elems = {};

elems.specialEvent = specialEvent.copy();
elems.specialEvent.reactHeader = new ReactEntity({ block: 'SpecialEventHeader' });
elems.specialEvent.reactHeader.icon = new ReactEntity({ block: 'SpecialEventHeader', elem: 'Icon' });
elems.specialEvent.reactHeader.title = new ReactEntity({ block: 'SpecialEventHeader', elem: 'Title' });
elems.specialEvent.reactHeader.title.link = new ReactEntity({ block: 'SpecialEventHeader', elem: 'TitleLink' });
elems.specialEvent.firstItem.reactShowcase = showcase.copy();

module.exports = elems;
