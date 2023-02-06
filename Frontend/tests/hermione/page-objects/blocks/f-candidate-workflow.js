const Entity = require('bem-page-object').Entity;
const sForm = require('./s-form');

const fCandidateWorkflow = new Entity({ block: 'f-candidate-workflow' });
const fCandidateWorkflowActionCreateVerification = fCandidateWorkflow.copy().mods({ action: 'create-verification' });
fCandidateWorkflow.form = new Entity({ block: 'f-candidate-workflow', elem: 'form' });
fCandidateWorkflow.buttonset = new Entity({ block: 'f-candidate-workflow', elem: 'buttonset' });
fCandidateWorkflow.progress = new Entity({ block: 'f-candidate-workflow', elem: 'progress' });
fCandidateWorkflow.form.field = new Entity({ block: 'f-candidate-workflow', elem: 'field' });
fCandidateWorkflow.form.fieldTypeApplication = new Entity({ block: 'f-candidate-workflow', elem: 'field' }).mods({ type: 'application' });
fCandidateWorkflow.form.fieldTypeConsideration = new Entity({ block: 'f-candidate-workflow', elem: 'field' }).mods({ type: 'consideration' });
fCandidateWorkflow.form.submit = sForm.submit.copy();
fCandidateWorkflow.dialogActionCreateVerification = new Entity({ block: 'f-candidate-workflow', elem: 'dialog' }).mods({ action: 'create-verification' });
fCandidateWorkflow.dialogActionCertificationCreate = new Entity({ block: 'f-candidate-workflow', elem: 'dialog' }).mods({ action: 'certification-create' });

fCandidateWorkflow.actionCloseCandidate = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'close' });
fCandidateWorkflow.actionCertificationCreate = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'certification-create' });

module.exports = {
    fCandidateWorkflow,
    fCandidateWorkflowActionCreateVerification,
};
