const { ReactEntity } = require('../../../../vendors/hermione');
const menuElems = require('../../../../components/Menu/Menu.test/Menu.page-object/index@common.js');
const popupElems = require('../../../../components/Popup/Popup.test/Popup.page-object/index@common.js');
const commonElems = require('./index@common');

const elems = commonElems();

elems.translate.sourceLangSwitch.text = new ReactEntity({ block: 'Button2', elem: 'Text' });
elems.translate.sourceLangSwitch.control = new ReactEntity({ block: 'Select2', elem: 'Control' });

elems.translate.targetLangSwitch.text = new ReactEntity({ block: 'Button2', elem: 'Text' });
elems.translate.targetLangSwitch.control = new ReactEntity({ block: 'Select2', elem: 'Control' });

elems.translate.valuesContent = new ReactEntity({ block: 'Translate', elem: 'ValuesContent' });
elems.translate.definitionContent = new ReactEntity({ block: 'Translate', elem: 'DefinitionContent' });

elems.translatePopup = popupElems.popupVisible.copy();
elems.translatePopup.menu = menuElems.menu.copy();
elems.translatePopup.menu.item = menuElems.menu.item.copy();
elems.translatePopup.AzerbajaniLang = elems.translatePopup.menu.item.nthChild(1);
elems.translatePopup.EnglishLang = elems.translatePopup.menu.item.nthChild(4);
elems.translatePopup.ArabicLang = elems.translatePopup.menu.item.nthChild(5);
elems.translatePopup.ArmenianLang = elems.translatePopup.menu.item.nthChild(6);
elems.translatePopup.PortugueseLang = elems.translatePopup.menu.item.nthChild(64);

elems.translatePopupLego = popupElems.popupVisible;
elems.translatePopupLego.menu = menuElems.menu;
elems.translatePopupLego.menu.item = menuElems.menu.item;
elems.translatePopupLego.AzerbajaniLang = elems.translatePopupLego.menu.item.nthChild(1);
elems.translatePopupLego.EnglishLang = elems.translatePopupLego.menu.item.nthChild(4);
elems.translatePopupLego.ArabicLang = elems.translatePopupLego.menu.item.nthChild(5);
elems.translatePopupLego.ArmenianLang = elems.translatePopupLego.menu.item.nthChild(6);
elems.translatePopupLego.PortugueseLang = elems.translatePopupLego.menu.item.nthChild(64);

elems.translate.clear = new ReactEntity({ block: 'Translate', elem: 'Clear' });

module.exports = elems;
