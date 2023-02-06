const { ReactEntity } = require('../../../../vendors/hermione');

const block = 'CinemaChoicePopup';

const elems = {};

elems.cinemaChoicePopup = new ReactEntity({ block });
elems.cinemaChoicePopup.header = new ReactEntity({ block, elem: 'Header' });
elems.cinemaChoicePopup.scrollableContent = new ReactEntity({ block, elem: 'Scrollable' });
elems.cinemaChoicePopup.link = new ReactEntity({ block, elem: 'PartnerLink' });
elems.cinemaChoicePopup.partnerLink = elems.cinemaChoicePopup.link.nthChild(2);
elems.cinemaChoicePopup.settingsLink = new ReactEntity({ block: 'Link' });
elems.cinemaChoicePopup.footer = new ReactEntity({ block, elem: 'Footer' });

elems.cinemaChoicePopup.item = new ReactEntity({ block: 'CinemaItem' });
elems.cinemaChoicePopup.item2 = elems.cinemaChoicePopup.item.nthChild(2);
elems.cinemaChoicePopup.item.variant = new ReactEntity({ block: 'CinemaItem', elem: 'Variant' });
elems.cinemaChoicePopup.item.variantAvailable = elems.cinemaChoicePopup.item.variant.copy().mods({ avail: true });
elems.cinemaChoicePopup.settings = new ReactEntity({ block: 'ChoiceSettings' });
elems.cinemaChoicePopup.close = new ReactEntity({ block, elem: 'Close' });

elems.cinemaChoicePopupSettings = new ReactEntity({ block: 'ChoiceSettings', elem: 'Popup' });
elems.cinemaChoicePopupSettings.drawerContent = new ReactEntity({ block: 'Drawer', elem: 'Content' });
elems.cinemaChoicePopupSettings.drawerContent.sixItem = new ReactEntity({ block: 'CinemaCheckbox' }).nthChild(6);
elems.cinemaChoicePopupSettings.footer = new ReactEntity({ block: 'ChoiceSettings', elem: 'Footer' });
elems.cinemaChoicePopupSettings.footer.closeButton = new ReactEntity({ block: 'ChoiceSettings', elem: 'CloseButton' });

elems.cardHeader = new ReactEntity({ block: 'CardHeader' });
elems.cardHeader.title = new ReactEntity({ block: 'CardHeader', elem: 'Title' });

module.exports = elems;
