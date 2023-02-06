const Entity = require('bem-page-object').Entity;

const fOfferWorkflow = new Entity({ block: 'f-offer-workflow' });
fOfferWorkflow.approve = new Entity({ block: 'f-offer-workflow' }).mods({ action: 'approve' });
fOfferWorkflow.send = new Entity({ block: 'f-offer-workflow' }).mods({ action: 'send' });
fOfferWorkflow.form = new Entity({ block: 'f-offer-workflow', elem: 'form' });
fOfferWorkflow.buttonset = new Entity({ block: 'f-offer-workflow', elem: 'buttonset' });
fOfferWorkflow.progress = new Entity({ block: 'f-offer-workflow', elem: 'progress' });
fOfferWorkflow.dialogActionApprove = new Entity({ block: 'f-offer-workflow', elem: 'dialog' }).mods({ action: 'approve' });
fOfferWorkflow.dialogActionSend = new Entity({ block: 'f-offer-workflow', elem: 'dialog' }).mods({ action: 'send' });
fOfferWorkflow.field = new Entity({ block: 'f-offer-workflow', elem: 'field' });
fOfferWorkflow.fieldAbc = fOfferWorkflow.field.copy().mods({ type: 'abc-services' });
fOfferWorkflow.fieldProfessionalLevel = fOfferWorkflow.field.copy().mods({ type: 'professional-level' });
fOfferWorkflow.fieldNeedRelocation = fOfferWorkflow.field.copy().mods({ type: 'need-relocation' });
fOfferWorkflow.fieldReceiver = fOfferWorkflow.field.copy().mods({ type: 'receiver' });
fOfferWorkflow.fieldSource = fOfferWorkflow.field.copy().mods({ type: 'source' });

module.exports = fOfferWorkflow;
