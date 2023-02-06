const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;

const Objects = {};

Objects.tariffsGrid = new Entity('[class*="TariffsListContainer"]');
Objects.tariffsGrid.header = new Entity('tbody > tr:first-child');
Objects.tariffsGrid.arrowButtons = new Entity('[class*="ArrowButtons"]');

Objects.trust = new Entity('[class*="Card"] button');
Objects.paymentFormClose = new Entity('[class*="Close"]');
Objects.OnboardingPaidFeatures = new Entity('[class*="OnboardingPaidFeatures"]');
Objects.Modal = new Entity('.Modal .Modal-Content');
Objects.Modal.button = new Entity('button');
Objects.Modal.skipWrapper = new Entity('[class*="skipWrapper"]');
Objects.Modal.skipWrapper.button = new Entity('button');
Objects.Modal.Lottie = new Entity('[class*="Lottie"]');

module.exports = {
    objects: bemPageObject.create(Objects)
};
