const pageObject = require('@yandex-int/bem-page-object');

const Entity = require('../Entity').ReactEntity;
const { blocks: reactSForm, methods } = require('../react-blocks/SForm');

const blocks = {};

blocks.interviewCreateView = new Entity({ block: 'InterviewCreateView' });
blocks.interviewCreateView.error = new Entity({ block: 'MessageBar', type: 'error' })
blocks.interviewCreateView.candidate = new Entity({ block: 'InterviewCreateView', elem: 'CandidateInfo' });
blocks.interviewCreateView.candidate.id = new Entity({ block: 'CandidateInfo', elem: 'Id' });

blocks.interviewCreateForm = new Entity({
    block: 'InterviewCreateForm',
});

const getInterviewCreateFormElem = (elem, mods = null) => {
    const _elem = new Entity({
        block: 'InterviewCreateForm',
        elem,
    });
    if (!mods) {
        return _elem;
    }
    return _elem.mods(mods);
};

blocks.interviewCreateForm.header = new Entity({ block: 'SGroup', elem: 'Header'})

blocks.interviewCreateForm.submit = getInterviewCreateFormElem('Button', { type: 'submit' });

blocks.interviewCreateForm.formError = reactSForm.formError.copy();
blocks.interviewCreateForm.gridTimeSlots = getInterviewCreateFormElem('TimeSlots');
blocks.interviewCreateForm.gridTimeSlots.add = reactSForm.gridAdd.copy();
blocks.interviewCreateForm.interviewSlots = getInterviewCreateFormElem('InterviewSlots');
blocks.interviewCreateForm.interviewSlots.add = reactSForm.gridAdd.copy();

blocks.interviewCreateForm.email = methods.getSFieldOfName('email');
blocks.interviewCreateForm.type = methods.getSFieldOfName('type');
blocks.interviewCreateForm.isAnyTime = methods.getSFieldOfName('is-any-time');
blocks.interviewCreateForm.office = methods.getSFieldOfName('office');
blocks.interviewCreateForm.timezone = methods.getSFieldOfName('timezone');
blocks.interviewCreateForm.comment = methods.getSFieldOfName('comment');
blocks.interviewCreateForm.subject = methods.getSFieldOfName('subject');
blocks.interviewCreateForm.text = methods.getSFieldOfName('text');
blocks.interviewCreateForm.text.addMacros = new Entity({
    block: 'MacrosInput',
    elem: 'TopPanel',
}).descendant(new Entity({
    block: 'MacrosInput',
    elem: 'Macros',
}));
blocks.interviewCreateForm.attachments = methods.getSFieldOfName('attachments ');
blocks.interviewCreateForm.ordering = methods.getSFieldOfName('ordering');
blocks.interviewCreateForm.lunchDuration = methods.getSFieldOfName('lunch-duration');
blocks.interviewCreateForm.needNotify = methods.getSFieldOfName('need-notify-candidate');
blocks.interviewCreateForm.sequenceInfo = methods.getSGroupOfType('interview_sequence_info');
blocks.interviewCreateForm.templates = getInterviewCreateFormElem('Template', { type: 'template' });
blocks.interviewCreateForm.templates.categories = getInterviewCreateFormElem('TemplateCategories');
blocks.interviewCreateForm.templates.options = getInterviewCreateFormElem('TemplateOptions');
blocks.interviewCreateForm.templates.apply = getInterviewCreateFormElem('TemplateApply');
blocks.interviewCreateForm.signatures = getInterviewCreateFormElem('Template', { type: 'signature' });
blocks.interviewCreateForm.signatures.apply = getInterviewCreateFormElem('TemplateApply');
blocks.interviewCreateForm.signatures.options = getInterviewCreateFormElem('TemplateOptions');

function createTimeSlot(to, index) {
    to[`timeSlot${index}`] = reactSForm.gridRow.copy().nthChild(index);
    to[`timeSlot${index}`].date = methods.getGridFieldInputOfName('date');
    to[`timeSlot${index}`].start = methods.getGridFieldInputOfName('start');
    to[`timeSlot${index}`].end = methods.getGridFieldInputOfName('end');
    to[`timeSlot${index}`].isFullDay = methods.getGridFieldInputOfName('is-full-day');
    to[`timeSlot${index}`].delete = reactSForm.gridDelete.copy();
}

function createInterviewSlot(to, index) {
    to[`interviewSlot${index}`] = reactSForm.gridRow.copy().nthChild(index);
    to[`interviewSlot${index}`].application = methods.getGridFieldInputOfName('application');
    to[`interviewSlot${index}`].type = methods.getGridFieldInputOfName('interview-type');
    to[`interviewSlot${index}`].aaType = methods.getGridFieldInputOfName('aa-type');
    to[`interviewSlot${index}`].isCode = methods.getGridFieldInputOfName('is-code');
    to[`interviewSlot${index}`].title = methods.getGridFieldInputOfName('name');
    to[`interviewSlot${index}`].preset = methods.getGridFieldInputOfName('preset');
    to[`interviewSlot${index}`].additionalInterviewers = methods.getGridFieldInputOfName('additional-interviewers');
    to[`interviewSlot${index}`].interviewerTables = methods.getSRow('tables');
    to[`interviewSlot${index}`].interviewerTables.delete = new Entity({
        block: 'InterviewersTable',
        elem: 'Delete',
    });
    to[`interviewSlot${index}`].interviewerTables.checkbox = new Entity({
        block: 'Checkbox',
    });
    to[`interviewSlot${index}`].interviewerTables.loadMore = getInterviewCreateFormElem('LoadMore');

    to[`interviewSlot${index}`].delete = reactSForm.gridDelete.copy();
}

createTimeSlot(blocks.interviewCreateForm.gridTimeSlots, 1);
createInterviewSlot(blocks.interviewCreateForm.interviewSlots, 1);
createInterviewSlot(blocks.interviewCreateForm.interviewSlots, 2);

module.exports = pageObject.create(blocks);
