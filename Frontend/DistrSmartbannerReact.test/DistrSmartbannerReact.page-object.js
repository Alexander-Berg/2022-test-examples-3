'use strict';

const { Entity: El, create, ReactEntity } = require('../../../../src/vendors/hermione');
const legacyPO = require('../../../../hermione/page-objects/touch-phone');

const PO = {};

PO.header = legacyPO.header;
PO.main = new El({ block: 'main' });
PO.footer = new ReactEntity({ block: 'SerpFooter' });

PO.distrPopup = new El({ block: 'distr-popup' });
PO.distrPopup.closeButton = new El({ block: 'distr-popup', elem: 'button', modName: 'type', modVal: 'close' });
PO.distrPopup.cross = new El({ block: 'distr-popup', elem: 'close' });

PO.smartInfo = new ReactEntity({ block: 'DistrSmartbanner' });
PO.smartInfo.button = new ReactEntity({ block: 'DistrSmartbanner', elem: 'Control' });
PO.smartInfo.title = new ReactEntity({ block: 'DistrSmartbanner', elem: 'Title' });
PO.smartInfo.action = PO.smartInfo.button.mods({ type: 'action' });
PO.smartInfo.close = PO.smartInfo.button.mods({ type: 'close' });

PO.yandexSearch = legacyPO.yandexSearch;

PO.miniSuggest = new El({ block: 'mini-suggest' });
PO.miniSuggest.content = new El({ block: 'mini-suggest', elem: 'popup' });

PO.headerBurgerButton = legacyPO.HeaderPhone.Actions.User;

PO.burgerMenu = new El({ block: 'user-id' });

PO.page = new El({ block: 'b-page' });

PO.serpList = new El({ block: 'serp-list' });

PO.verifiedPopup = new El({ block: 'verified-tooltip' });

PO.serpItemSecond = new El({ block: 'serp-item' }).nthType(2);

PO.smartBannerAnchor = new El({ block: 'a11y-smartbanner-anchor' });

module.exports = create(PO);
