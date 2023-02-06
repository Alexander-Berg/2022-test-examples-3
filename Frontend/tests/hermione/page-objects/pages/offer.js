const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;

const blocks = require('../blocks/common');
const sForm = require('../blocks/s-form');
const fOfferWorkflow = require('../blocks/f-offer-workflow');
const fOfferForm = require('../blocks/f-offer-form');

blocks.pageOffer = new Entity({ block: 'f-page-offer' });
blocks.pageOffer.progress = new Entity({ block: 'f-page-offer', elem: 'progress' });
blocks.pageOffer.tip = new Entity({ block: 'f-page-offer', elem: 'tip' });

blocks.pageForm = new Entity({ block: 'f-page-form' });
blocks.pageForm.form = fOfferForm.copy();

blocks.pageForm.form.submit = fOfferForm.submit.copy();
blocks.pageForm.form.submitDisabled = fOfferForm.submitDisabled.copy();

blocks.pageForm.form.fieldFullName = fOfferForm.fieldFullName.copy();
blocks.pageForm.form.fieldFullName.input = blocks.input.control.copy();
blocks.pageForm.form.fieldPosition = fOfferForm.fieldPosition.copy();
blocks.pageForm.form.fieldPosition.input = blocks.input.control.copy();
blocks.pageForm.form.fieldStaffPositionName = fOfferForm.fieldStaffPositionName.copy();
blocks.pageForm.form.fieldStaffPositionName.input = blocks.input.control.copy();
blocks.pageForm.form.fieldJoinAt = fOfferForm.fieldJoinAt.copy();
blocks.pageForm.form.fieldDepartment = fOfferForm.fieldDepartment.copy();
blocks.pageForm.form.fieldDepartment.input = blocks.textinput.control.copy();
blocks.pageForm.form.fieldGrade = fOfferForm.fieldGrade.copy();
blocks.pageForm.form.fieldGrade.input = blocks.input.control.copy();
blocks.pageForm.form.fieldSalary = fOfferForm.fieldSalary.copy();
blocks.pageForm.form.fieldSalary.input = blocks.input.control.copy();
blocks.pageForm.form.fieldHourlyRate = fOfferForm.fieldHourlyRate.copy();
blocks.pageForm.form.fieldHourlyRate.input = blocks.input.control.copy();
blocks.pageForm.form.fieldNeedRelocation = fOfferForm.fieldNeedRelocation.copy();
blocks.pageForm.form.fieldOrg = fOfferForm.fieldOrg.copy();
blocks.pageForm.form.fieldOrg.button = fOfferForm.fieldOrg.button.copy();
blocks.pageForm.form.fieldWorkPlace = fOfferForm.fieldWorkPlace.copy();
blocks.pageForm.form.fieldOffice = fOfferForm.fieldOffice.copy();
blocks.pageForm.form.fieldOffice.button = fOfferForm.fieldOffice.button.copy();
blocks.pageForm.form.fieldPaymentType = fOfferForm.fieldPaymentType.copy();
blocks.pageForm.form.fieldNeedRelocation = fOfferForm.fieldNeedRelocation.copy();
blocks.pageForm.form.fieldVmi = fOfferForm.fieldVmi.copy();
blocks.pageForm.form.fieldProfession = fOfferForm.fieldProfession.copy();
blocks.pageForm.form.fieldProfession.select = blocks.select2.control.copy();
blocks.pageForm.form.fieldProfessionSphere = fOfferForm.fieldProfessionSphere.copy();
blocks.pageForm.form.fieldUserName = fOfferForm.fieldUserName.copy();
blocks.pageForm.form.fieldUserNameDisabled = fOfferForm.fieldUserNameDisabled.copy();

blocks.pageForm.form.groupWorkPlace = fOfferForm.groupWorkPlace.copy();
blocks.pageForm.form.groupWorkPlace.cat = fOfferForm.groupWorkPlace.cat.copy();
blocks.pageForm.form.groupWorkPlace.arrow = fOfferForm.groupWorkPlace.arrow.copy();
blocks.pageForm.form.groupProcessingConditions = fOfferForm.groupProcessingConditions.copy();
blocks.pageForm.form.groupProcessingConditions.cat = fOfferForm.groupProcessingConditions.cat.copy();
blocks.pageForm.form.groupBonus = fOfferForm.groupBonus.copy();
blocks.pageForm.form.groupBonus.cat = fOfferForm.groupBonus.cat.copy();
blocks.pageForm.form.groupExtraBonus = fOfferForm.groupExtraBonus.copy();
blocks.pageForm.form.groupExtraBonus.cat = fOfferForm.groupExtraBonus.cat.copy();

blocks.pageOffer.actions = new Entity({ block: 'f-page-offer', elem: 'actions' });
blocks.pageOffer.actions.approve = fOfferWorkflow.approve.copy();
blocks.pageOffer.actions.send = fOfferWorkflow.send.copy();
blocks.pageOffer.actions.update = new Entity({ block: 'f-page-offer', elem: 'action' }).mods({ action: 'update' });

blocks.offerActionApproveDialog = fOfferWorkflow.dialogActionApprove.copy();
blocks.offerActionApproveDialog.form = fOfferWorkflow.form.copy();
blocks.offerActionApproveDialog.buttonset = fOfferWorkflow.buttonset.copy();
blocks.offerActionApproveDialog.progress = fOfferWorkflow.progress.copy();

blocks.offerActionSendDialog = fOfferWorkflow.dialogActionSend.copy();
blocks.offerActionSendDialog.form = fOfferWorkflow.form.copy();
blocks.offerActionSendDialog.form.fieldReceiver = fOfferWorkflow.fieldReceiver.copy();
blocks.offerActionSendDialog.form.submit = sForm.submit.copy();

blocks.offerActionProgress = fOfferWorkflow.progress.copy();
blocks.offerActionApproveDialog.form.submit = sForm.submit.copy();
blocks.offerActionApproveDialog.form.field = fOfferWorkflow.field.copy();
blocks.offerActionApproveDialog.form.fieldAbc = fOfferWorkflow.fieldAbc.copy();
blocks.offerActionApproveDialog.form.fieldProfessionalLevel = fOfferWorkflow.fieldProfessionalLevel.copy();
blocks.offerActionApproveDialog.form.fieldSource = fOfferWorkflow.fieldSource.copy();
blocks.offerActionApproveDialog.form.fieldAbc.input = blocks.input.control.copy();
blocks.offerActionApproveDialog.form.submitDisabled = sForm.submitDisabled.copy();

blocks.abcSuggest = blocks.bAutocompletePopup.copy();
blocks.abcSuggest.contest = blocks.bAutocompletePopupItem.nthType(1);
blocks.abcSuggest.femida = blocks.bAutocompletePopupItem.nthType(2);
blocks.abcSuggest.wiki = blocks.bAutocompletePopupItem.nthType(3);

blocks.positionSuggest = blocks.bAutocompletePopup.copy();
blocks.positionSuggest.developer = blocks.bAutocompletePopupItem.nthType(1);

blocks.userNameSuggest = blocks.bAutocompletePopup.copy();
blocks.userNameSuggest.first = blocks.bAutocompletePopupItem.nthType(1);

blocks.departmentSuggest = blocks.bAutocompletePopup.copy();
blocks.departmentSuggest.yandex = new Entity({ block: 'm-suggest-group' }).nthType(1);
blocks.departmentSuggest.yandex.item = new Entity({ block: 'm-suggest-item' });

blocks.proLevelSelect = blocks.select2Popup.copy();
blocks.proLevelSelect.internship = blocks.select2Item.nthType(2);

blocks.orgSelect = blocks.select2Popup.copy();
blocks.orgSelect.yandex = blocks.select2Item.nthType(2);

blocks.sourceSelect = blocks.select2Popup.copy();
blocks.sourceSelect.internalReference = blocks.select2Item.nthType(2);

blocks.employeeTypeSelect = blocks.select2Popup.copy();
blocks.employeeTypeSelect.rotation = blocks.select2Item.nthType(4);
blocks.employeeTypeSelect.intern = blocks.select2Item.nthType(5);

blocks.workPlaceSelect = blocks.select2Popup.copy();
blocks.workPlaceSelect.office = blocks.select2Item.nthType(2);

blocks.officeSelect = blocks.select2Popup.copy();
blocks.officeSelect.morozov = blocks.select2Item.nthType(21);

blocks.professionSelect = blocks.select2Popup.copy();
blocks.professionSelect.learnSpecialist = blocks.select2Item.nthType(2);

blocks.professionSphereSelect = blocks.select2Popup.copy();
blocks.professionSphereSelect.hr = blocks.select2Item.nthType(8);

blocks.paymentTypeSelect = blocks.select2Popup.copy();
blocks.paymentTypeSelect.monthly = blocks.select2Item.nthType(2);
blocks.paymentTypeSelect.hourly = blocks.select2Item.nthType(3);

blocks.employmentTypeSelect = blocks.select2Popup.copy();
blocks.employmentTypeSelect.full = blocks.select2Item.nthType(2);

blocks.contractTypeSelect = blocks.select2Popup.copy();
blocks.contractTypeSelect.indefinite = blocks.select2Item.nthType(2);
blocks.contractTypeSelect.project = blocks.select2Item.nthType(5);

module.exports = pageObject.create(blocks);
