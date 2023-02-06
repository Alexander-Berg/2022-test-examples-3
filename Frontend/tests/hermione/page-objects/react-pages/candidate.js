const pageObject = require('@yandex-int/bem-page-object');

const Entity = require('../Entity').ReactEntity;
const { blocks: rBlocks, methods } = require('../react-blocks/SForm');
const sendToReviewForm = require('../react-blocks/InterviewSendToReviewForm');

const blocks = {};

blocks.sendToReviewForm = sendToReviewForm.copy();

const getCandidateEditFormField = methods.getSFieldOfName;
const getCandidateCertificateCreateFormFieldOfType = methods.getSFieldOfType;
const getCandidateEditFormFieldOfType = methods.getSFieldOfType;

const getElem = elem => {
    return new Entity({
        block: 'CandidateEditForm',
        elem,
    });
};

const getCandidateEditFormSGridField = methods.getGridFieldInputOfName;
const getCandidateEditFormSGridFieldComponent = methods.getGridFieldComponentOfNameAndType;

blocks.candidateEditForm = new Entity({
    block: 'CandidateEditForm',
});

blocks.candidateEditForm.content = new Entity({
    block: 'CandidateEditForm',
    elem: 'Content',
});

blocks.candidateEditForm.submit = new Entity({
    block: 'CandidateEditForm',
    elem: 'Button',
}).mods({ type: 'submit' });

blocks.candidateEditForm.header = new Entity({ block: 'Heading' })
blocks.candidateEditForm.formError = rBlocks.formError.copy();
blocks.candidateEditForm.tagsField = getCandidateEditFormField('tags');
blocks.candidateEditForm.mainRecruiterField = getCandidateEditFormField('main-recruiter');
blocks.candidateEditForm.recruitersField = getCandidateEditFormField('recruiters');
blocks.candidateEditForm.fieldMainEmail = getCandidateEditFormField('main-email');
blocks.candidateEditForm.fieldMainPhone = getCandidateEditFormField('main-phone');
blocks.candidateEditForm.fieldMainSkype = getCandidateEditFormField('main-skype');
blocks.candidateEditForm.gridContacts = getElem('Contacts');
blocks.candidateEditForm.gridProfessions = getElem('Professions');
blocks.candidateEditForm.source = getCandidateEditFormField('source');
blocks.candidateEditForm.sourceDescription = getCandidateEditFormField('source-description');
blocks.candidateEditForm.gridContacts = new Entity({
    block: 'CandidateEditForm',
    elem: 'Contacts',
});
blocks.candidateEditForm.gridContacts.add = rBlocks.gridAdd.copy();
blocks.candidateEditForm.gridJobs = getElem('Jobs');
blocks.candidateEditForm.gridJobs.add = rBlocks.gridAdd.copy();
blocks.candidateEditForm.gridJobs.add = rBlocks.gridAdd.copy();
blocks.candidateEditForm.gridProfessions.add = rBlocks.gridAdd.copy();
blocks.candidateEditForm.firstName = getCandidateEditFormField('first-name');
blocks.candidateEditForm.middleName = getCandidateEditFormField('middle-name');
blocks.candidateEditForm.lastName = getCandidateEditFormField('last-name');
blocks.candidateEditForm.gridEducations = getElem('Educations');
blocks.candidateEditForm.gridContacts.add = rBlocks.gridAdd.copy();
blocks.candidateEditForm.gridProfessions.add = rBlocks.gridAdd.copy();
blocks.candidateEditForm.gridEducations.add = rBlocks.gridAdd.copy();

blocks.candidateEditForm.birthday = getCandidateEditFormField('birthday');
blocks.candidateEditForm.birthday.input = new Entity('input');
blocks.candidateEditForm.gender = getCandidateEditFormField('gender');
blocks.candidateEditForm.country = getCandidateEditFormField('country');
blocks.candidateEditForm.city = getCandidateEditFormField('city');
blocks.candidateEditForm.targetCities = getCandidateEditFormField('target-cities');
blocks.candidateEditForm.docs = getCandidateEditFormField('attachments');

function createContact(to, index) {
    to[`contact${index}`] = rBlocks.gridRow.copy().nthChild(index);
    to[`contact${index}`].delete = rBlocks.gridDelete.copy();
    to[`contact${index}`].select = getCandidateEditFormFieldOfType('select');
    to[`contact${index}`].input = getCandidateEditFormFieldOfType('textinput');
}

function createProfession(to, index) {
    to[`professions${index}`] = rBlocks.gridRow.copy().nthChild(index);
    to[`professions${index}`].delete = rBlocks.gridDelete.copy();
    to[`professions${index}`].profession = getCandidateEditFormFieldOfType('suggest');
    to[`professions${index}`].salary = getCandidateEditFormFieldOfType('textinput');
}

function createEducation(to, index) {
    to[`educations${index}`] = rBlocks.gridRow.copy().nthChild(index);
    to[`educations${index}`].delete = rBlocks.gridDelete.copy();
    to[`educations${index}`].institution = methods.getSRow('institution').descendant(getCandidateEditFormFieldOfType('textinput'));
    to[`educations${index}`].faculty = methods.getSRow('faculty').descendant(getCandidateEditFormFieldOfType('textinput'));
    to[`educations${index}`].degree = getCandidateEditFormFieldOfType('select');
    to[`educations${index}`].endYear = getCandidateEditFormFieldOfType('year');
}

createContact(blocks.candidateEditForm.gridContacts, 1);
createContact(blocks.candidateEditForm.gridContacts, 2);
createContact(blocks.candidateEditForm.gridContacts, 3);
createContact(blocks.candidateEditForm.gridContacts, 4);

blocks.candidateCloseForm = new Entity({ block: 'CandidateCloseForm' });
blocks.candidateCloseForm.submit = new Entity({ block: 'CandidateCloseForm', elem: 'Button' }).mods({ type: 'submit' });
blocks.candidateCloseForm.resolutionSelect = new Entity({ block: 'SField' }).mods({ name: 'consideration-resolution' });
blocks.candidateCloseForm.resolutionSelect.button = new Entity({ block: 'Select2', elem: 'Button' });
blocks.candidateCloseForm.resolutionSelect.popup = new Entity({ block: 'Select2', elem: 'Popup' });
blocks.candidateCloseForm.resolutionSelect.rejectedAfterTestTask = new Entity({
    block: 'Menu',
    elem: 'Group',
}).nthChild(2).descendant(
    new Entity({ block: 'Menu', elem: 'Item' })
        .mods({ type: 'option' })
        .nthChild(3)
);

blocks.interviewRoundsList = new Entity({
    block: 'InterviewRoundsList',
});

blocks.interviewRound = new Entity({
    block: 'InterviewRounds',
}).firstChild();

blocks.interviewRounds = new Entity({
    block: 'InterviewRounds',
});

blocks.interviewRound.interviewRoundInterview = new Entity({
    block: 'InterviewRounds',
    elem: 'Interviews',
});

blocks.interviewRound.interviewRoundActions = new Entity({
    block: 'InterviewRounds',
    elem: 'Actions',
});

blocks.interviewRound.interviewRoundActions.deleteButton = new Entity({
    block: 'Icon',
}).mods({
    type: 'trash',
});

blocks.interviewRoundsModal = new Entity({
    block: 'InterviewRounds',
    elem: 'Modal',
});

blocks.interviewRoundsModal.content = new Entity({
    block: 'FModal',
    elem: 'Content',
});

blocks.interviewRoundsModalSubmitButton = new Entity({
    block: 'InterviewRounds',
    elem: 'ModalButton',
}).mods({
    type: 'submit',
});

blocks.interviewRoundsModalCancelButton = new Entity({
    block: 'InterviewRounds',
    elem: 'ModalButton',
}).mods({
    type: 'cancel',
});
createProfession(blocks.candidateEditForm.gridProfessions, 1);
createProfession(blocks.candidateEditForm.gridProfessions, 2);
createProfession(blocks.candidateEditForm.gridProfessions, 3);

function createJob(to, index) {
    to[`jobs${index}`] = rBlocks.gridRow.copy().nthChild(index);
    to[`jobs${index}`].delete = rBlocks.gridDelete.copy();
    to[`jobs${index}`].employer = getCandidateEditFormSGridField('employer');
    to[`jobs${index}`].startMonth = getCandidateEditFormSGridFieldComponent('start-date', 'month');
    to[`jobs${index}`].startYear = getCandidateEditFormSGridFieldComponent('start-date', 'year');
    to[`jobs${index}`].endMonth = getCandidateEditFormSGridFieldComponent('end-date', 'month');
    to[`jobs${index}`].endYear = getCandidateEditFormSGridFieldComponent('end-date', 'year');
    to[`jobs${index}`].position = getCandidateEditFormSGridField('position');
    to[`jobs${index}`].salaryEvaluation = getCandidateEditFormSGridField('salary-evaluation');
}

createJob(blocks.candidateEditForm.gridJobs, 1);
createJob(blocks.candidateEditForm.gridJobs, 2);
createEducation(blocks.candidateEditForm.gridEducations, 1);
createEducation(blocks.candidateEditForm.gridEducations, 2);

blocks.candidateCreateCertificateForm = new Entity({
    block: 'CandidateCreateCertificateForm',
});

blocks.candidateCreateCertificateForm.submit = new Entity({
    block: 'CandidateCreateCertificateForm',
    elem: 'Button',
}).mods({ type: 'submit' });

blocks.candidateCreateCertificateForm.consideration = getCandidateCertificateCreateFormFieldOfType('select');

module.exports = pageObject.create(blocks);
