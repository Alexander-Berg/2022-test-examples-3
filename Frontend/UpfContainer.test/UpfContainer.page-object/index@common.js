const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.upfContainer = new ReactEntity({ block: 'UpfContainer' });
elems.upfContainer.cover = new ReactEntity({ block: 'UpfContainer', elem: 'Cover' });
elems.upfContainer.cover.thumb = new ReactEntity({ block: 'UpfContainer', elem: 'Thumb' });
elems.upfContainer.cover.button = new ReactEntity({ block: 'UpfContainer', elem: 'Button' });
elems.upfContainer.cover.thumb.image = new ReactEntity({ block: 'Image' });
elems.upfContainer.settings = new ReactEntity({ block: 'ChoiceSettings' });

elems.settingsPopup = new ReactEntity({ block: 'Modal', elem: 'Cell' });
elems.settingsPopup.modalContent = new ReactEntity({ block: 'Modal', elem: 'Content' });
elems.settingsPopup.modalContent.content = new ReactEntity({ block: 'ChoiceSettings', elem: 'Content' });
elems.settingsPopup.modalContent.content.sixItem = new ReactEntity({ block: 'CinemaCheckbox' }).nthChild(6);

module.exports = elems;
