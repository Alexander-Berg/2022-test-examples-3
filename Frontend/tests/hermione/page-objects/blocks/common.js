/** Page Objects для внешних блоков */

const Entity = require('bem-page-object').Entity;
const blocks = {};

blocks.button2 = new Entity({ block: 'button2' });
blocks.button2Submit = new Entity({ block: 'button2' }).mods({ type: 'submit' });

blocks.checkbox = new Entity({ block: 'checkbox' });

blocks.link = new Entity({ block: 'link' });

blocks.arrow = new Entity({ block: 'm-expandable', elem: 'arrow' });

blocks.input = new Entity({ block: 'input' });
blocks.input.control = new Entity({ block: 'input', elem: 'control' });
blocks.input.settings = new Entity({ block: 'input', elem: 'settings' });
blocks.inputPopupItems = new Entity({ block: 'popup2' }).mods({ visible: 'yes' }).descendant(new Entity({ block: 'input', elem: 'popup-items' }));
blocks.inputPopupItems.first = new Entity({ block: 'b-autocomplete-item' }).nthChild(1);

blocks.textinput = new Entity({ block: 'textinput' });
blocks.textinput.control = new Entity({ block: 'textinput', elem: 'control' });

blocks.popup2 = new Entity({ block: 'popup2' });
blocks.popup2Visible = blocks.popup2.mods({ visible: 'yes' });

blocks.popup = new Entity({ block: 'popup' });
blocks.popupVisible = blocks.popup.mods({ visibility: 'visible' });

blocks.modal = new Entity({ block: 'modal' });
blocks.modal.content = new Entity({ block: 'modal', elem: 'content' });

blocks.select2Popup = new Entity({ block: 'select2', elem: 'popup' }).mix(blocks.popup2Visible);
blocks.mSuggestPopup = new Entity({ block: 'm-suggest', elem: 'popup' }).mix(blocks.popup2Visible);

blocks.mSuggestPopup.items = new Entity({ block: 'm-suggest', elem: 'popup-items' });
blocks.mSuggestItem = new Entity({ block: 'm-suggest-item' });

blocks.menu = new Entity({ block: 'menu' });
blocks.menu.item = new Entity({ block: 'menu', elem: 'item' });
blocks.menu.secondItem = new Entity({ block: 'menu', elem: 'item' }).nthChild(2);
blocks.menu.thirdItem = new Entity({ block: 'menu', elem: 'item' }).nthChild(3);

blocks.select2Popup.menu = blocks.menu.copy();
blocks.select2Item = blocks.menu.item.copy();

blocks.select2 = new Entity({ block: 'select2' });
blocks.select2.control = new Entity({ block: 'select2', elem: 'control' });

blocks.bAutocompletePopup = blocks.popup2Visible.copy();
blocks.bAutocompletePopup.items = new Entity({ block: 'popup2', elem: 'items' });
blocks.bAutocompletePopupItem = new Entity({ block: 'b-autocomplete-item' });

blocks.fTabs = new Entity({ block: 'f-tabs' });
blocks.fTabs.tabs = new Entity({ block: 'f-tabs', elem: 'tabs' });

blocks.radioButton = new Entity({ block: 'radio-button' });
blocks.radioButton.radioSideRight = new Entity({ block: 'radio-button', elem: 'radio' }).mods({ side: 'right' });
blocks.radioButton.radio = new Entity({ block: 'radio-button', elem: 'radio' });
blocks.radioButton.radioFirst = new Entity({ block: 'radio-button', elem: 'radio' }).nthChild(1);
blocks.radioButton.radioThird = new Entity({ block: 'radio-button', elem: 'radio' }).nthChild(3);

blocks.staffPager = new Entity({ block: 'staff-pager' });
blocks.staffPager.page = new Entity({ block: 'staff-pager', elem: 'page' });
blocks.staffPager.secondPage = blocks.staffPager.page.copy().nthType(2);

blocks.textAreaControl = new Entity({ block: 'textarea', elem: 'control' });

blocks.dropdown2 = new Entity({ block: 'dropdown2' });

blocks.fSidePopupContent = new Entity({ block: 'f-side-popup', elem: 'content' });

blocks.fPage = new Entity({ block: 'f-page' });

blocks.fMessageForm = new Entity({ block: 'f-message-form' });
blocks.fMessageForm.form = new Entity({ block: 'f-message-form', elem: 'form' });

blocks.bodyModal = new Entity('body').child(blocks.modal.copy());

module.exports = blocks;
