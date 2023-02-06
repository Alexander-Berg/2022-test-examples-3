const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;

const blocks = require('../blocks/common');
const event = require('../blocks/f-event');
const sForm = require('../blocks/s-form');
const sField = require('../blocks/s-field');
const { fApplicationsTable } = require('../blocks/f-applications-table');
const { fMessageForm } = require('../blocks/f-message-form');
const { fMessages } = require('../blocks/f-messages');
const { fCandidateWorkflowActionCreateVerification, fCandidateWorkflow } = require('../blocks/f-candidate-workflow');

const considerationListHeader = new Entity({
    block: 'f-considerations-list',
    elem: 'header',
});

const getConsiderationsList = (type, status) => {
    return new Entity({
        block: 'f-considerations-list',
    }).mods({
        type,
        status,
    });
};

blocks.pageCandidate = new Entity({ block: 'f-page-candidate' });

blocks.pageCandidate.recruiters = new Entity({ block: 'f-page-candidate', elem: 'recruiters' });
blocks.pageCandidate.recruitersList = new Entity({ block: 'f-captioned-list' });
blocks.pageCandidate.recruitersEdit = new Entity({ block: 'f-candidate-workflow' }).mods({ fieldset: 'recruiters' });
blocks.pageCandidate.recruitersEdit.icon = new Entity({ block: 'awesome-icon' });

blocks.pageCandidate.header = new Entity({ block: 'f-page-candidate', elem: 'header' });
blocks.pageCandidate.header.attachment = new Entity({ block: 'f-page-candidate', elem: 'attachment' });
blocks.pageCandidate.header.user = new Entity({ block: 'f-page-candidate', elem: 'user' });

blocks.pageCandidate.rightTab = new Entity({ block: 'f-page-candidate', elem: 'col' }).mods({ position: 'right' });
blocks.pageCandidate.rightTab.tagsHeader = new Entity({ block: 'f-page-candidate', elem: 'tags-header' });
blocks.pageCandidate.rightTab.contactsHeader = new Entity({ block: 'f-page-candidate', elem: 'contacts-header' });
blocks.pageCandidate.rightTab.tagsHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });
blocks.pageCandidate.rightTab.contactsHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });
blocks.pageCandidate.rightTab.tags = new Entity({ block: 'f-candidate-field' }).mods({ type: 'tags' });
blocks.pageCandidate.rightTab.contacts = new Entity({ block: 'f-captioned-list' });

blocks.pageCandidate.leftTab = new Entity({ block: 'f-page-candidate', elem: 'col' }).mods({ position: 'left' });
blocks.pageCandidate.leftTab.professionsHeader = new Entity({ block: 'f-page-candidate', elem: 'professions-header' });
blocks.pageCandidate.leftTab.professionsHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });
blocks.pageCandidate.leftTab.professions = new Entity({ block: 'f-page-candidate', elem: 'profession' });
blocks.pageCandidate.leftTab.professions2 = new Entity({ block: 'f-page-candidate', elem: 'profession' }).nthChild('2n');
blocks.pageCandidate.leftTab.educationsHeader = new Entity({ block: 'f-page-candidate', elem: 'education-header' });
blocks.pageCandidate.leftTab.educationsHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });
blocks.pageCandidate.leftTab.educations = blocks.pageCandidate.leftTab.educationsHeader.copy().adjacentSibling(new Entity({ block: 'f-captioned-list' }));
blocks.pageCandidate.leftTab.personalHeader = new Entity({ block: 'f-page-candidate', elem: 'personal-header' });
blocks.pageCandidate.leftTab.personalHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });
blocks.pageCandidate.leftTab.personal = blocks.pageCandidate.leftTab.personalHeader.copy().adjacentSibling(new Entity({ block: 'f-captioned-list' }));

blocks.tagsEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'tags' });
blocks.recruitersEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'recruiters' });
blocks.contactsEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'contacts' });
blocks.professionsEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'professions' });
blocks.educationsEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'educations' });
blocks.fullNameEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'fullName' });
blocks.considerationEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'consideration' });
blocks.personalEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'personal' });
blocks.jobsEditForm = new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mods({ fieldset: 'jobs' });

blocks.pageCandidate.considerationHeader = new Entity({ block: 'f-page-candidate', elem: 'other-header' });
blocks.pageCandidate.considerationHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });
blocks.pageCandidate.considerationBody = blocks.pageCandidate.considerationHeader.copy().adjacentSibling(new Entity({ block: 'f-captioned-list' }));

blocks.pageCandidate.jobsHeader = new Entity({ block: 'f-page-candidate', elem: 'jobs-header' });
blocks.pageCandidate.jobsBody = blocks.pageCandidate.jobsHeader.copy().adjacentSibling(new Entity({ block: 'f-captioned-list' }));
blocks.pageCandidate.jobsHeader.edit = new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' });

blocks.pageCandidate.status = new Entity({ block: 'f-state' })
blocks.pageCandidate.statusInProgress = new Entity({ block: 'f-state' }).mods({ type: 'in-progress' });
blocks.pageCandidate.statusClosed = new Entity({ block: 'Status_status_archived' });

blocks.pageCandidate.paneTypeMessages = new Entity({ block: 'f-page-candidate', elem: 'pane' }).mods({ type: 'messages' });
blocks.pageCandidate.paneTypeMessages.messages = fMessages.copy();
blocks.pageCandidate.paneTypeMessages.form = new Entity({ block: 'f-page-candidate', elem: 'form' });
blocks.pageCandidate.paneTypeMessages.formFocus = blocks.pageCandidate.paneTypeMessages.form.copy().mods({ focus: '' });
blocks.pageCandidate.paneTypeMessages.form.fMessageForm = fMessageForm.copy();

blocks.pageCandidate.conflictOfInterestsInfo = new Entity({ block: 'f-page-candidate', elem: 'conflict-of-interests-info' });

blocks.pageCandidate.tabs = new Entity({ block: 'f-page-candidate', elem: 'tabs' }).mix(blocks.fTabs);
blocks.pageCandidate.tabs.menu = blocks.fTabs.tabs;
blocks.pageCandidate.user = new Entity({ block: 'f-page-candidate', elem: 'user' });
blocks.pageCandidate.user.header = new Entity({ block: 'f-layout', elem: 'header' });
blocks.pageCandidate.user.userName = new Entity({ block: 'f-page-candidate', elem: 'name' });
blocks.pageCandidate.user.userNameEdit = new Entity({ block: 'f-page-candidate', elem: 'name' }).adjacentSibling(new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'update-part' }));

blocks.pageCandidate.actions = new Entity({ block: 'f-page-candidate', elem: 'actions' });
blocks.pageCandidate.actions.menu = new Entity({ block: 'f-menu' });

blocks.menuPopup = blocks.popup2Visible.copy();

blocks.actionCloseCandidate = fCandidateWorkflow.actionCloseCandidate.copy();
blocks.actionCertificationCreate = fCandidateWorkflow.actionCertificationCreate.copy();

blocks.pageCandidate.workflow = new Entity({ block: 'f-page-candidate', elem: 'workflow' });
blocks.actionCreateVerification = fCandidateWorkflowActionCreateVerification.copy();

blocks.candidateActionCreateVerificationDialog = fCandidateWorkflow.dialogActionCreateVerification.copy();
blocks.candidateActionCreateVerificationDialog.form = fCandidateWorkflow.form.copy();
blocks.candidateActionCreateVerificationDialog.buttonset = fCandidateWorkflow.buttonset.copy();
blocks.candidateActionCreateVerificationDialog.progress = fCandidateWorkflow.progress.copy();

blocks.candidateActionCertificationCreateDialog = fCandidateWorkflow.dialogActionCertificationCreate.copy();
blocks.candidateActionCertificationCreateDialog.form = fCandidateWorkflow.form.copy();
blocks.candidateActionCertificationCreateDialog.buttonset = fCandidateWorkflow.buttonset.copy();
blocks.candidateActionCertificationCreateDialog.progress = fCandidateWorkflow.progress.copy();

blocks.applicationsPopup = blocks.select2Popup.copy();
blocks.applicationsPopup.firstApplication = blocks.select2Item.nthType(2);

blocks.considerationsPopup = blocks.select2Popup.copy();
blocks.considerationsPopup.firstConsideration = blocks.select2Item.nthType(2);

blocks.interviewsConsiderations = new Entity({ block: 'f-considerations-list' }).mods({ type: 'interviews' });
blocks.interviewsConsiderations.spin = new Entity({ block: 'spin2' });

blocks.interview = new Entity({ block: 'f-interview' });
blocks.interview.title = new Entity({ block: 'f-interview', elem: 'title' });

blocks.event = event.copy();

blocks.interviewCreate = new Entity({ block: 'f-considerations-list', elem: 'action' }).mods({ type: 'interview-create' });

blocks.interviewActionForm = new Entity({ block: 'f-interview-action', elem: 'form' }).mods({ type: 'interview-create' }).mix(sForm);

blocks.sendForApproval = new Entity({ block: 'f-considerations-list', elem: 'action' }).mods({ type: 'send-for-approval' });
blocks.sendForApprovalForm = new Entity({ block: 'f-consideration-action', elem: 'form' }).mix(sForm);
blocks.sendForApprovalForm.application = new Entity({ block: 'f-consideration-action', elem: 'field' }).mods({ type: 'application' });
blocks.sendForApprovalTypeSelect = blocks.select2Popup.copy();
blocks.sendForApprovalTypeSelect.vac51790 = blocks.select2Item.nthType(2);
blocks.applicationsTab = new Entity({ block: 'tabs-menu', elem: 'tab' }).mods({ type: 'applications' });
blocks.interviewsTab = new Entity({ block: 'tabs-menu', elem: 'tab' }).mods({ type: 'interviews' });

blocks.interviewActionFormHeader = new Entity({ block: 'f-interview-action', elem: 'form' }).descendant(new Entity({ block: 'f-layout', elem: 'header' }));
blocks.considerationListInterviewsInProgressHeader = new Entity({ block: 'f-considerations-list' })
    .mods({ status: 'in-progress' })
    .mix(
        new Entity({ block: 'f-considerations-list' })
            .mods({ type: 'interviews' })
    )
    .descendant(new Entity({ block: 'f-layout', elem: 'header' }));

blocks.interviewActionField = new Entity({ block: 'f-interview-action', elem: 'field' });

blocks.interviewActionForm.type = blocks.interviewActionField.mods({ type: 'type' });
blocks.interviewActionForm.type.button = new Entity({ block: 'button2' });
blocks.interviewTypeSelect = blocks.select2Popup.copy();
blocks.interviewTypeSelect.screening = blocks.select2Item.nthType(3);
blocks.interviewTypeSelect.regular = blocks.select2Item.nthType(4);
blocks.interviewTypeSelect.aa = blocks.select2Item.nthType(5);

blocks.interviewActionForm.application = blocks.interviewActionField.mods({ type: 'application' });
blocks.applicationSelect = blocks.select2Popup.copy();
blocks.applicationSelect.vac50228 = blocks.select2Item.nthType(2);

blocks.interviewActionForm.interviewer = blocks.interviewActionField.mods({ type: 'interviewer' });
blocks.interviewActionForm.interviewer.button = new Entity({ block: 'button2' });
blocks.interviewActionForm.interviewer.disabled = sField.mods({ disabled: 'yes' });
blocks.interviewActionForm.interviewer.input = new Entity({ block: 'input', elem: 'control' });
blocks.interviewerSelect = blocks.select2Popup.copy();
blocks.interviewerSelect.markova = blocks.select2Item.nthType(2);
blocks.interviewerSuggest = blocks.bAutocompletePopup.copy();
blocks.interviewerSuggest.marat = blocks.bAutocompletePopupItem.nthType(1);

blocks.interviewActionForm.preset = blocks.interviewActionField.mods({ type: 'preset' });
blocks.interviewActionForm.preset.disabled = sField.mods({ disabled: 'yes' });
blocks.interviewActionForm.preset.input = new Entity({ block: 'input', elem: 'control' });
blocks.presetSuggest = blocks.bAutocompletePopup.copy();
blocks.presetSuggest.preset = blocks.bAutocompletePopupItem.nthType(1);

blocks.interviewActionForm.interview = blocks.interviewActionField.mods({ type: 'section' });
blocks.interviewActionForm.interview.input = new Entity({ block: 'input', elem: 'control' });

blocks.interviewActionForm.typeAa = blocks.interviewActionField.mods({ type: 'aa-type' });

blocks.interviewActionForm.eventUrl = blocks.interviewActionField.mods({ type: 'event-url' });
blocks.interviewActionForm.eventUrl.input = new Entity({ block: 'input', elem: 'control' });

// Кнопка создания П-та
blocks.applicationCreateButton = blocks.pageCandidate.actions.copy().child(new Entity({ block: 'f-candidate-workflow' }).mods({ action: 'application-create' }));

// Форма создания П-та
blocks.applicationCreateForm = new Entity({ block: 'f-candidate-workflow', elem: 'dialog' }).mods({ action: 'application-create' })
    .descendant(new Entity({ block: 'f-candidate-workflow', elem: 'form' }).mix(new Entity({ block: 's-form' })));
blocks.aplicationCreateFormSpinner = new Entity({ block: 'f-candidate-workflow', elem: 'dialog' }).mods({ action: 'application-create' })
    .descendant(new Entity({ block: 'f-candidate-workflow', elem: 'progress' }));
blocks.applicationCreateForm.header = new Entity({ block: 'f-layout', elem: 'header' });
blocks.applicationCreateForm.vacancies = new Entity({ block: 'f-candidate-workflow', elem: 'field' }).mods({ type: 'vacancies' });
blocks.applicationCreateForm.vacancies.input = new Entity({ block: 'input', elem: 'control' });
blocks.applicationCreateForm.vacancies.error = new Entity({ block: 's-message' }).mods({ type: 'error' });
blocks.applicationCreateForm.createActivated = new Entity({ block: 'f-candidate-workflow', elem: 'field' }).mods({ type: 'create-activated' });
blocks.applicationCreateForm.createActivated.yes = new Entity(' .radio-button__control[value=true]');
blocks.applicationCreateForm.submit = new Entity({ block: 'button2' }).mods({ type: 'submit' });

blocks.vacancySuggest = blocks.bAutocompletePopup.copy();
blocks.vacancySuggest.selectedVacancy = blocks.bAutocompletePopupItem.nthType(1);

// Таблица Активные П-ты
blocks.actualApplications = new Entity({ block: 'f-page-candidate', elem: 'applications' })
    .descendant(fApplicationsTable);

// Вкладка П-тов
blocks.applPane = new Entity({ block: 'f-page-candidate', elem: 'pane' }).mods({ type: 'applications' });
blocks.applPane.activeConsiderationApplications = new Entity({ block: 'f-consideration' })
    .mods({ status: 'in-progress' })
    .descendant(fApplicationsTable);

// Вкладка Секций
blocks.interviewsPane = new Entity({ block: 'f-page-candidate', elem: 'pane' }).mods({ type: 'interviews' });
blocks.interviewsPane.archivedConsiderationsList = getConsiderationsList('interviews', 'archived');
blocks.interviewsPane.activeConsiderationsList = getConsiderationsList('interviews', 'in-progress');
blocks.interviewsPane.archivedConsiderationsList.header = considerationListHeader.copy();
blocks.interviewsPane.activeConsiderationsList.header = considerationListHeader.copy();
blocks.interviewsPane.activeConsiderationsList.createInterviewRound = new Entity({
    block: 'f-interview-action',
}).mods({
    action: 'interview-round-create',
});
blocks.interviewsPane.activeConsiderationsList.firstInterview = new Entity({ block: 'f-interview' });
blocks.interviewsPane.activeConsiderationsList.firstInterview.actions = new Entity({ block: 'f-interview', elem: 'actions' });
blocks.interviewsPane.activeConsiderationsList.firstInterview.firstComment = new Entity({ block: 'f-comment' }).nthChild(1);
blocks.interviewsPane.activeConsiderationsList.firstInterview.actions.sendToReview = new Entity({ block: 'f-interview-action' }).mods({
    type: 'send-to-review',
});

blocks.interviewsPane.firstArchivedConsideration = new Entity({
    block: 'f-consideration',
}).mods({
    status: 'archived',
}).nthType(1);

blocks.interviewsPane.firstArchivedConsideration.firstInterview = new Entity({
    block: 'f-interview',
}).nthType(1);

blocks.interviewsPane.firstArchivedConsideration.cat = new Entity({
    block: 'm-expandable',
    elem: 'trigger',
});

module.exports = pageObject.create(blocks);
