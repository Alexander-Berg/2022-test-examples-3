const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

CommonObjects.downloadIframe = new Entity('#download-iframe');

CommonObjects.shareEditOnboarding = new Entity('.onboarding-shareedit');
CommonObjects.shareEditOnboarding.image = new Entity('.onboarding-shareedit__image');

DesktopObjects.removeAdsButton = new Entity('.direct__remove-ads a');
CommonObjects.directFrame = new Entity('.direct__iframe');
CommonObjects.directTop = new Entity('.root__top-ad');
CommonObjects.directBottom = new Entity('.root__bottom-ad');
CommonObjects.directInner = new Entity('#yadisk-yap > div');

CommonObjects.langSwicher = new Entity('.footer__language-switcher');
CommonObjects.langSwicher.currentLang = new Entity('.lang-select__text');

CommonObjects.langMenu = new Entity('.lang-select__menu');

CommonObjects.langMenuEN = new Entity('//span[text() = \'EN\']');
CommonObjects.langMenuRU = new Entity('//span[text() = \'RU\']');

TouchObjects.drawerHandle = new Entity('.Drawer-Handle');
TouchObjects.drawerContent = new Entity('.Drawer-Content');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    desktop: bemPageObject.create(DesktopObjects),
    touch: bemPageObject.create(TouchObjects)
};
