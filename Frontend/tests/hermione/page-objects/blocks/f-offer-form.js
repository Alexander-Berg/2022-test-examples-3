const Entity = require('bem-page-object').Entity;

const blocks = require('../blocks/common');
const sForm = require('../blocks/s-form');
const sField = require('../blocks/s-field');
const { sMessageTypeSuccess } = require('../blocks/s-message');

const fOfferForm = new Entity({ block: 'f-offer-form' });

fOfferForm.submit = sForm.submit.copy();
fOfferForm.submitDisabled = sForm.submitDisabled.copy();

fOfferForm.fieldFullName = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'full-name' });
fOfferForm.fieldEmployeeType = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'employee-type' });
fOfferForm.fieldPosition = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'position' });
fOfferForm.fieldStaffPositionName = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'staff-position-name' });
fOfferForm.fieldJoinAt = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'join-at' });
fOfferForm.fieldDepartment = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'department' });
fOfferForm.fieldGrade = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'grade' });
fOfferForm.fieldSalary = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'salary' });
fOfferForm.fieldHourlyRate = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'hourly-rate' });
fOfferForm.fieldOrg = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'org' });
fOfferForm.fieldOrg.button = new Entity({ block: 'button2' });
fOfferForm.fieldWorkPlace = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'work-place' });
fOfferForm.fieldOffice = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'office' });
fOfferForm.fieldOffice.button = new Entity({ block: 'button2' });
fOfferForm.fieldPaymentType = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'payment-type' });
fOfferForm.fieldEmploymentType = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'employment-type' });
fOfferForm.fieldUserName = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'username' });
fOfferForm.fieldContractType = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'contract-type' });
fOfferForm.fieldNeedRelocation = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'need-relocation' });
fOfferForm.fieldVmi = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'vmi' });
fOfferForm.fieldProfession = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'profession' });
fOfferForm.fieldProfessionSphere = new Entity({ block: 'f-offer-form', elem: 'field' }).mods({ type: 'professional-sphere' });

fOfferForm.groupPersonal = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'list' }).nthChild(1);
fOfferForm.groupWorkPlace = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'list' }).nthChild(2);
fOfferForm.groupWorkPlace.cat = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'cat' }).descendant(blocks.link.copy());
fOfferForm.groupWorkPlace.arrow = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'cat' }).descendant(blocks.arrow.copy());
fOfferForm.groupProcessingConditions = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'list' }).nthChild(3);
fOfferForm.groupProcessingConditions.cat = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'cat' }).descendant(blocks.link.copy());
fOfferForm.groupBonus = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'list' }).nthChild(4);
fOfferForm.groupBonus.cat = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'cat' }).descendant(blocks.link.copy());
fOfferForm.groupExtraBonus = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'list' }).nthChild(5);
fOfferForm.groupExtraBonus.cat = new Entity({ block: 'f-offer-form', elem: 'group' }).mods({ type: 'cat' }).descendant(blocks.link.copy());

fOfferForm.fieldUserName.messageTypeSuccess = sMessageTypeSuccess.copy();
fOfferForm.fieldUserNameDisabled = fOfferForm.fieldUserName.mix(sField.mods({ disabled: 'yes' }));
fOfferForm.fieldUserName.input = blocks.input.control.copy();

module.exports = fOfferForm;
