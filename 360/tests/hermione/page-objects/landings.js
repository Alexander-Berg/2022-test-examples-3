const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;

const BusinessObjects = {};

BusinessObjects.main = new Entity('[class*="Section_main"]');
BusinessObjects.stickyTurnOnButton = new Entity('[class*="ActionButtonBlock"]');

const BusinessTariffObjects = {};

BusinessTariffObjects.cards = new Entity('[class*="TariffsCards__Wrapper"]');
BusinessTariffObjects.headerBackdrop = new Entity('[class*="TariffsGridHeader__Backdrop"]');

const BusinessTariffEducationObjects = {};

BusinessTariffEducationObjects.cards = new Entity('[class*="TariffsCards__Wrapper"]');
BusinessTariffEducationObjects.headerBackdrop = new Entity('[class*="TariffsGridHeader__Backdrop"]');

const BusinessCorporateMailObjects = {};

BusinessCorporateMailObjects.root = new Entity('[class*="BusinessCorporateMail"]');
BusinessCorporateMailObjects.stickyButton = new Entity('.Button2[class*="BusinessCorporateMail__Button"]');
BusinessCorporateMailObjects.animation = new Entity('[class*="BusinessCorporateMail__Animation"]');

const BusinessMessenger = {};

BusinessMessenger.root = new Entity('[class*="BusinessMessenger"]');
BusinessMessenger.stickyButton = new Entity('.Button2[class*="BusinessMessenger__Button"]');

const MailLanding = {};
MailLanding.root = new Entity('[class*="PageWrapper"]');
MailLanding.stickyButtons = new Entity('[class*="FreezedToolbar"]');

module.exports = {
    business: bemPageObject.create(BusinessObjects),
    businessTariff: bemPageObject.create(BusinessTariffObjects),
    businessTariffEducation: bemPageObject.create(BusinessTariffEducationObjects),
    businessCorporateMail: bemPageObject.create(BusinessCorporateMailObjects),
    businessMessenger: bemPageObject.create(BusinessMessenger),
    mailLanding: bemPageObject.create(MailLanding)
};
