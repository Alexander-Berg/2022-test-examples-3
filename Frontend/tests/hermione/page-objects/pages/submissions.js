const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;
const blocks = require('../blocks/common');

const fSubmission = require('../blocks/f-submission');
const fSubmissionWorkflowForm = require('../blocks/f-submission-workflow-form');

blocks.submissionsTable = new Entity({
    block: 'f-submissions-table',
    elem: 'body',
});

blocks.submissionsTable.first = new Entity({
    block: 'f-submissions-table-row',
}).nthChild(1);

blocks.submissionsTable.first.id = new Entity({
    block: 'f-submissions-table-row',
    elem: 'cell',
}).mods({
    type: 'status',
})
    .descendant(new Entity({
        block: 'link',
    }));

blocks.submissionsTable.first.candidateLink = new Entity({
    block: 'f-submissions-table-row',
    elem: 'cell',
}).mods({
    type: 'candidate',
})
    .descendant(new Entity({
        block: 'link',
    }));

blocks.fSubmission = fSubmission.copy();
blocks.fSubmissionWorkflowForm = fSubmissionWorkflowForm.copy();

blocks.sidePopup = new Entity({
    block: 'f-side-popup',
    elem: 'content',
});

module.exports = pageObject.create(blocks);
