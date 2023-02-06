const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;

const blocks = require('../blocks/common');

const fApplicationWorkflow = require('../blocks/f-application-workflow');
const sForm = require('../blocks/s-form');

blocks.pageApplication = new Entity({ block: 'f-page-application' });

blocks.pageApplication.actions = new Entity({ block: 'f-application', elem: 'actions' });
blocks.pageApplication.statusClosed = new Entity({ block: 'Status' }).mods({
    status: 'closed',
});
blocks.pageApplication.actions.menu = new Entity({ block: 'f-menu' });

blocks.pageApplication.messages = new Entity({ block: 'f-application', elem: 'messages' });
blocks.pageApplication.messages.messagesList = new Entity({ block: 'f-application', elem: 'messages' });
blocks.pageApplication.messageForm = new Entity({ block: 'f-application', elem: 'message-form' });
blocks.pageApplication.messageForm.textAreaControl = blocks.textAreaControl.copy();
blocks.pageApplication.messageForm.submit = sForm.submit.copy();
blocks.pageApplication.messageForm.submitDisabled = sForm.submitDisabled.copy();

blocks.pageApplication.header = new Entity({ block: 'f-application', elem: 'header' });

blocks.applicationMenuPopup = blocks.popup2Visible.copy();
blocks.actionCreateOffer = fApplicationWorkflow.actionCreateOffer.copy();

blocks.applicationActionCreateOfferDialog = fApplicationWorkflow.dialogActionCreateOffer.copy();
blocks.applicationActionCreateOfferDialog.form = fApplicationWorkflow.form.copy();
blocks.applicationActionCreateOfferDialog.buttonset = fApplicationWorkflow.buttonset.copy();
blocks.applicationActionCreateOfferDialog.progress = fApplicationWorkflow.progress.copy();
blocks.applicationActionCreateOfferDialog.form = fApplicationWorkflow.form.copy();
blocks.applicationActionCreateOfferDialog.form.submit = sForm.submit.copy();
blocks.applicationActionCreateOfferDialog.form.submitDisabled = sForm.submitDisabled.copy();

blocks.applicationActionClose = fApplicationWorkflow.actionCloseApplication.copy();

blocks.applicationActionCloseDialog = fApplicationWorkflow.dialogActionCloseApplication.copy();
blocks.applicationActionCloseDialog.form = fApplicationWorkflow.form.copy();
blocks.applicationActionCloseDialog.buttonset = fApplicationWorkflow.buttonset.copy();
blocks.applicationActionCloseDialog.progress = fApplicationWorkflow.progress.copy();
blocks.applicationActionCloseDialog.form = fApplicationWorkflow.form.copy();
blocks.applicationActionCloseDialog.fieldTypeResolution = new Entity({ block: 'f-application-workflow', elem: 'field' }).mods({ type: 'resolution' });
blocks.applicationActionCloseDialog.form.submit = sForm.submit.copy();
blocks.applicationActionCloseDialog.form.submitDisabled = sForm.submitDisabled.copy();

blocks.resolutionsSelect = blocks.select2Popup.copy();
blocks.resolutionsSelect.didNotPassAssessments = blocks.select2Item.nthType(2);

module.exports = pageObject.create(blocks);
