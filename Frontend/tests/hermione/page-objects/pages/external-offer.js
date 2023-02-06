const pageObject = require('@yandex-int/bem-page-object');

const Entity = require('../Entity').ReactEntity;

const blocks = {};

blocks.externalOfferForm = new Entity({
    block: 'ExternalOfferForm',
});

blocks.externalOfferForm.submit = new Entity({
    block: 'ExternalOfferForm',
    elem: 'Button',
}).mods({ type: 'submit' });

blocks.externalOfferForm.formError = new Entity({
    block: 'SValidationMessage',
}).mods({
    entity: 'form',
});

const FieldValidationError = new Entity({
    block: 'SValidationMessage',
}).mods({
    type: 'error',
});

const FieldValidationSuccess = new Entity({
    block: 'SValidationMessage',
}).mods({
    type: 'normal',
});

const getElem = elem => {
    return new Entity({
        block: 'ExternalOfferForm',
        elem,
    });
};

const getExternalOfferField = name => {
    return new Entity({ block: 'SField' }).mods({
        name,
    });
};

const getExternalOfferRow = name => {
    return new Entity({ block: 'SRow' }).mods({
        name,
    });
};

const clear = new Entity({
    block: 'Textinput',
    elem: 'Clear',
});

blocks.english = new Entity({
    block: 'LanguageSwitcher',
    elem: 'Lang',
}).nthChild(2);
blocks.externalOfferForm.fieldLastName = getExternalOfferField('last-name');
blocks.externalOfferForm.fieldFirstName = getExternalOfferField('first-name');
blocks.externalOfferForm.fieldLastNameEn = getExternalOfferField('last-name-en');
blocks.externalOfferForm.fieldFirstNameEn = getExternalOfferField('first-name-en');
blocks.externalOfferForm.preferredFirstAndLastName = getExternalOfferField('preferred-first-and-last-name');
blocks.externalOfferForm.fieldMiddleName = getExternalOfferField('middle-name');
blocks.externalOfferForm.fieldPhoto = getExternalOfferField('photo');
blocks.externalOfferForm.fieldSnils = getExternalOfferField('snils');
blocks.externalOfferForm.fieldJoinAt = getExternalOfferField('join-at');
blocks.externalOfferForm.fieldEducation = getExternalOfferField('education');
blocks.externalOfferForm.fieldEducationSphere = getExternalOfferField('education-direction');
blocks.externalOfferForm.fieldEducationInstitution = getExternalOfferField('educational-institution');
blocks.externalOfferForm.fieldEducationEndDate = getExternalOfferField('education-end-date');
blocks.externalOfferForm.fieldPassportPages = getExternalOfferField('passport-pages');
blocks.externalOfferForm.fieldComment = getExternalOfferField('comment');
blocks.externalOfferForm.rowPhoto = getExternalOfferRow('photo');

blocks.externalOfferForm.rowBirthday = getExternalOfferRow('birthday');
blocks.externalOfferForm.fieldBirthday = getExternalOfferField('birthday');
blocks.externalOfferForm.rowBirthday.clear = clear.copy();

blocks.externalOfferForm.fieldGender = getExternalOfferField('gender');

blocks.externalOfferForm.rowUsername = getExternalOfferRow('username');
blocks.externalOfferForm.fieldUsername = getExternalOfferField('username');
blocks.externalOfferForm.rowUsername.success = FieldValidationSuccess.copy();
blocks.externalOfferForm.rowUsername.error = FieldValidationError.copy();
blocks.externalOfferForm.rowUsername.clear = clear.copy();

blocks.externalOfferForm.rowPhone = getExternalOfferRow('phone');
blocks.externalOfferForm.fieldPhone = getExternalOfferField('phone');
blocks.externalOfferForm.rowPhone.clear = clear.copy();

blocks.externalOfferForm.rowHomeEmail = getExternalOfferRow('home-email');
blocks.externalOfferForm.rowHomeEmail.clear = clear.copy();
blocks.externalOfferForm.fieldHomeEmail = getExternalOfferField('home-email');

blocks.externalOfferForm.fieldOS = getExternalOfferField('os');
blocks.externalOfferForm.fieldCitizenship = getExternalOfferField('citizenship');
blocks.externalOfferForm.fieldResidenceAddress = getExternalOfferField('residence-address');
blocks.externalOfferForm.fieldID = getExternalOfferField('id-card');
blocks.externalOfferForm.fieldDocuments = getExternalOfferField('documents');
blocks.externalOfferForm.fieldEmploymentBook = getExternalOfferField('employment-book');
blocks.externalOfferForm.fieldBankName = getExternalOfferField('bank-name');
blocks.externalOfferForm.fieldBic = getExternalOfferField('bic');
blocks.externalOfferForm.rowBic = getExternalOfferRow('bic');
blocks.externalOfferForm.rowBic.clear = clear.copy();

blocks.externalOfferForm.fieldBankAccount = getExternalOfferField('bank-account');
blocks.externalOfferForm.rowBankAccount = getExternalOfferRow('bank-account');
blocks.externalOfferForm.rowBankAccount.clear = clear.copy();

blocks.externalOfferForm.fieldCardNumber = getExternalOfferField('card-number');
blocks.externalOfferForm.rowCardNumber = getExternalOfferRow('card-number');
blocks.externalOfferForm.rowCardNumber.clear = clear.copy();

blocks.externalOfferForm.fieldMainLanguage = getExternalOfferField('main-language');
blocks.externalOfferForm.fieldSpokenLanguages = getExternalOfferField('spoken-languages');

blocks.externalOfferForm.fieldIsFullTimeEducation = getExternalOfferField('is-full-time-education');
blocks.externalOfferForm.hasEducationConfirmation = getExternalOfferField('has-education-confirmation');
blocks.externalOfferForm.fieldIsAgree = getExternalOfferField('is-agree');
blocks.externalOfferForm.fieldNdaAccepted = getExternalOfferField('nda-accepted');
blocks.externalOfferForm.fieldEdsNeeded = getExternalOfferField('is-eds-needed');
blocks.externalOfferForm.rowEdsNeeded = getExternalOfferRow('is-eds-needed');
blocks.externalOfferForm.rowEds = getExternalOfferRow('eds');
blocks.externalOfferForm.edsSign = new Entity({
    block: 'EdsSign',
});
blocks.externalOfferForm.edsStart = new Entity({
    block: 'EdsSign',
    elem: 'Start',
});
blocks.externalOfferForm.edsConfirmation = new Entity({
    block: 'EdsSign',
    elem: 'Step',
}).mods({
    type: 'confirmation',
});
blocks.externalOfferForm.edsError = new Entity({
    block: 'EdsSign',
    elem: 'Step',
}).mods({
    type: 'error',
});

blocks.externalOfferForm.edsSuccess = new Entity({
    block: 'EdsSign',
    elem: 'Step',
}).mods({
    type: 'success',
});

blocks.externalOfferForm.edsInputCode = new Entity({
    block: 'EdsSign',
    elem: 'InputCode',
});
blocks.externalOfferForm.edsInputCode.input = new Entity({ block: 'Textinput', elem: 'Control' });

blocks.externalOfferForm.groupContract = new Entity({ block: 'SGroup' }).mods({
    type: 'contract',
});

blocks.ndaAgreement = getElem('ModalContent');
blocks.eds = getElem('ModalContent');

blocks.modal = new Entity({
    block: 'Modal',
});
blocks.ndaAgreement.agree = new Entity({
    block: 'SField',
    elem: 'ModalButton',
});
blocks.eds.agree = new Entity({
    block: 'SField',
    elem: 'ModalButton',
});
blocks.header = new Entity({
    block: 'ExternalOfferView',
    elem: 'Header',
});
blocks.html = 'html';

module.exports = pageObject.create(blocks);
