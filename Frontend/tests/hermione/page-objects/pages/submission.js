const pageObject = require('bem-page-object');
const blocks = require('../blocks/common');

blocks.mCalendar = require('../blocks/m-calendar');

const fSubmission = require('../blocks/f-submission');
const fSubmissionWorkflowForm = require('../blocks/f-submission-workflow-form');

blocks.fSubmission = fSubmission.copy();
blocks.fSubmissionWorkflowForm = fSubmissionWorkflowForm.copy();

module.exports = pageObject.create(blocks);
