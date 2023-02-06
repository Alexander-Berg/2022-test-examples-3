const Entity = require('bem-page-object').Entity;

const fApplicationWorkflow = new Entity({ block: 'f-application-workflow' });
fApplicationWorkflow.actionCreateOffer = new Entity({ block: 'f-application-workflow' }).mods({ action: 'create-offer' });
fApplicationWorkflow.form = new Entity({ block: 'f-application-workflow', elem: 'form' });
fApplicationWorkflow.buttonset = new Entity({ block: 'f-application-workflow', elem: 'buttonset' });
fApplicationWorkflow.progress = new Entity({ block: 'f-application-workflow', elem: 'progress' });
fApplicationWorkflow.dialogActionCreateOffer = new Entity({ block: 'f-application-workflow', elem: 'dialog' }).mods({ action: 'create-offer' });

fApplicationWorkflow.actionCloseApplication = new Entity({ block: 'f-application-workflow' }).mods({ action: 'close' });
fApplicationWorkflow.dialogActionCloseApplication = new Entity({ block: 'f-application-workflow', elem: 'dialog' }).mods({ action: 'close' });

fApplicationWorkflow.fApplicationWorkflowRowTypeResolution = new Entity({ block: 'f-application-workflow', elem: 'row' }).mods({ type: 'resolution' });

module.exports = fApplicationWorkflow;
