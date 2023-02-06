const Entity = require('bem-page-object').Entity;

const submissionWorkflowForm = new Entity({
    block: 'f-submission-workflow-form',
});

submissionWorkflowForm.header = new Entity({
    block: 'f-submission-workflow-form',
    elem: 'header',
});

submissionWorkflowForm.duplicatesField = new Entity({
    block: 'f-duplicates-table',
    elem: 'body',
});

submissionWorkflowForm.duplicatesField.first = new Entity({
    block: 'f-duplicates-table-row',
}).nthChild(1);

submissionWorkflowForm.duplicatesField.first.radio = new Entity({
    block: 'f-duplicates-table-row',
    elem: 'cell',
}).mods({ type: 'radio' })
    .descendant(new Entity({
        block: 'radiobox',
    }));

submissionWorkflowForm.createCandidate = new Entity({
    block: 's-field',
    elem: 'none-wrap',
}).descendant(new Entity({
    block: 'radiobox',
    elem: 'radio',
}));

submissionWorkflowForm.isRejectionField = new Entity({
    block: 'f-submission-workflow-form',
    elem: 'field',
}).mods({
    type: 'is-rejection',
}).descendant(new Entity({
    block: 'checkbox',
}));

submissionWorkflowForm.submit = new Entity({
    block: 's-form',
    elem: 'button',
}).mods({
    type: 'submit',
});

module.exports = submissionWorkflowForm;
