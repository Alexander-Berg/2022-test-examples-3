const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};

CommonObjects.tuningPage = new Entity('.tuning-page');
CommonObjects.tuningPage.header = new Entity('.tuning-header');
CommonObjects.tuningPage.body = new Entity('.tuning-page__body');
CommonObjects.tuningPage.spaceBar = new Entity('.available-space__bar-values');
CommonObjects.tuningPage.title = new Entity('.tuning-page-title');
CommonObjects.tuningPage.tariffContainer = new Entity('.tariff-container');
CommonObjects.tuningPage.tariffContainer.tariffWrapper = new Entity('.tariff__wrapper');
CommonObjects.tuningPage.tariffContainer.tariffWrapper.tariffSize = new Entity('.tariff__size');
CommonObjects.tuningPage.tariffContainer.tariffWrapper.buyLink = new Entity('.buy-link');
CommonObjects.tuningPage.tariffSubmitButton = new Entity('.buy-button button');
CommonObjects.tuningPage.prolongateButton = new Entity('.tuning-page-history__history-desc button');
CommonObjects.tuningPage.promoCodeActivationButton = new Entity('.tuning-page-promo button');

CommonObjects.tuningPageDiskForBusinessButton = new Entity('//a[text() = "Для бизнеса"]');

//форма оплаты
CommonObjects.iframe = new Entity('.payment-dialog__iframe');
CommonObjects.cardFormTariff = new Entity('.card-form__tariff');
CommonObjects.cardFormAmount = new Entity('.card-form__amount');
CommonObjects.paymentDialog = new Entity('.payment-dialog__iframe-wrap');
CommonObjects.spin = new Entity('spin2');

//модальное окно активации промокода
CommonObjects.modalContent = new Entity('.Modal-Content');
CommonObjects.modalContent.input = new Entity('input');
CommonObjects.modalContent.activateButton = new Entity('.promo-dialog__activate');
CommonObjects.modalContent.promoError = new Entity('.promo-dialog__promo-error');
CommonObjects.modalContent.finishedDescription = new Entity('.promo-dialog__finished-description');

// подраздел "доступное место"
CommonObjects.tuningPage.body.availableSpace = new Entity('.available-space:not(.tariff__indicator-bar)');
CommonObjects.tuningPage.body.availableSpace.bar = new Entity('.available-space__bar');
CommonObjects.tuningPage.body.availableSpace.bar.green = new Entity('.indicator-bar_load_normal');
CommonObjects.tuningPage.body.availableSpace.bar.yellow = new Entity('.indicator-bar_load_medium');
CommonObjects.tuningPage.body.availableSpace.bar.red = new Entity('.indicator-bar_load_full');

module.exports = {
    common: bemPageObject.create(CommonObjects)
};
