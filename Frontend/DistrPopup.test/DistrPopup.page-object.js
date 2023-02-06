'use strict';

const { Entity: El, create } = require('../../../vendors/hermione');

const PO = {};

PO.distrPopup = new El({ block: 'distr-popup' });
PO.distrPopup.closeButton = new El({ block: 'distr-popup', elem: 'button', modName: 'type', modVal: 'close' });
PO.distrPopup.cross = new El({ block: 'distr-popup', elem: 'close' });
PO.distrPopup.installButton = new El({ block: 'distr-popup', elem: 'button', modName: 'type', modVal: 'click' });
PO.distrPopup.signUpButton = new El({ block: 'distr-popup', elem: 'button', modName: 'reg', modVal: 'yes' });
PO.distrPopup.loginButton = new El({
    block: 'distr-popup',
    elem: 'button',
    modName: 'type',
    modVal: 'click',
}).not(PO.distrPopup.signUpButton);
PO.distrPopupSimple = new El({ block: 'distr-popup', modName: 'layout', modVal: 'simple' });
PO.distrPopupFlat = new El({ block: 'distr-popup', modName: 'layout', modVal: 'flat' });

PO.promoCurtain = new El({ block: 'promo-curtain' });
PO.promoCurtain.close = new El({ block: 'promo-curtain', elem: 'close' });

PO.page = new El({ block: 'b-page' });
PO.header = new El({ block: 'serp-header' });
PO.header.arrow = new El({ block: 'search2' });
PO.header.arrow.input = new El({ block: 'input' });
PO.header.arrow.input.control = new El({ block: 'input', elem: 'control' });
PO.yaplus = new El({ block: 'yaplus' });
PO.serpNavigation = new El({ block: 'serp-navigation' });
PO.navigation = new El({ block: 'navigation' });
PO.mainSuggest = new El({ block: 'mini-suggest' });
PO.mainSuggest.content = new El({ block: 'mini-suggest', elem: 'popup' });
PO.main = new El({ block: 'main' });
PO.mainContent = new El({ block: 'main', elem: 'content' });
PO.serpList = new El({ block: 'serp-list' });
PO.rightColumn = new El({ block: 'content', elem: 'right' });
PO.serpItem = new El({ block: 'serp-item' });
PO.serpAdvItem = new El({ block: 't-construct-adapter', elem: 'adv' });
PO.organic = new El({ block: 'organic' });
PO.organic.title = new El({ block: 'organic', elem: 'title-wrapper' });
PO.organic.title.link = new El({ block: 'link' }).mods({ theme: 'normal' });
PO.firstSnippet = PO.serpItem.not(PO.serpAdvItem).nthType(1);

module.exports = create(PO);
