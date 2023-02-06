const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;
const blocks = require('../blocks/common');
const fEvent = require('../blocks/f-event');
const fTabs = require('../blocks/f-tabs');

const InterviewSendToReviewForm = require('../react-blocks/InterviewSendToReviewForm');

blocks.interviewPage = new Entity({ block: 'f-page-interview' });
blocks.interviewPage.assigned = new Entity({ block: 'f-page-interview', elem: 'assigned' });
blocks.interviewPage.actions = new Entity({ block: 'f-page-interview', elem: 'actions' });
blocks.interviewPage.info = new Entity({ block: 'f-page-interview', elem: 'info' });
blocks.interviewPage.actions.rename = new Entity({ block: 'f-page-interview', elem: 'action' }).mods({ action: 'rename' });
blocks.interviewPage.actions.sendToReview = new Entity({ block: 'f-page-interview', elem: 'action' }).mods({ action: 'send-to-review' });
blocks.interviewPage.actions.sendToReview.icon = new Entity({ block: 'awesome-icon_icon_bug'});
blocks.interviewPage.grade = new Entity({ block: 'f-page-interview', elem: 'grade' });
blocks.interviewPage.grade.hire3 = new Entity({
    block: 'radio-button',
    elem: 'radio',
}).nthChild(4);
blocks.interviewPage.grade.nohire = new Entity({
    block: 'radio-button',
    elem: 'radio',
}).nthChild(1);
blocks.interviewPage.assigned.list = new Entity({ block: 'f-expandable-list', elem: 'list' });
blocks.interviewPage.header = new Entity({ block: 'f-page-interview', elem: 'header' });
blocks.interviewPage.comments = new Entity({ block: 'f-page-interview', elem: 'comments' });
blocks.interviewPage.comments.list = new Entity({ block: 'f-comments-list' });
blocks.interviewPage.comments.list.first = new Entity({ block: 'f-comment' }).nthChild(1);

blocks.interviewComment = new Entity({ block: 'f-interview-comment' });
blocks.interviewComment.comment = new Entity({ block: 'f-wiki-editor', elem: 'field' });
blocks.interviewComment.comment.control = new Entity({ block: 'textarea', elem: 'control' });
blocks.interviewComment.buttons = new Entity({ block: 'f-interview-comment', elem: 'buttonset' });
blocks.interviewComment.buttons.save = new Entity({ block: 'button2' }).mods({ type: 'submit' });

blocks.interviewFinish = new Entity({ block: 'f-page-interview', elem: 'finish' });
blocks.problemsList = new Entity({ block: 'f-problems-list' });
blocks.problemsList.first = new Entity({ block: 'f-problem' }).nthChild(1);
blocks.problemsList.first.add = new Entity({ block: 'f-problem', elem: 'assign' });
blocks.assignmentsList = new Entity({ block: 'f-assignments-list' });
blocks.assignmentsList.first = new Entity({ block: 'f-assignment' }).nthChild(1);
blocks.assignmentsList.addFavourite = new Entity({
    block: 'f-assignments-list',
    elem: 'action',
}).mods({
    action: 'add-favorite',
});

blocks.assignmentsList.addFromCatalogue = new Entity({
    block: 'f-assignments-list',
    elem: 'action',
}).mods({
    action: 'add-from-catalog',
});

blocks.titleText = blocks.interviewPage.descendant(new Entity({ block: 'f-layout', elem: 'header' }));

blocks.renameForm = new Entity({ block: 'f-interview-action', elem: 'form' }).mods({ type: 'rename' });
blocks.renameForm.field = new Entity({ block: 'input', elem: 'control' });
blocks.renameForm.submit = new Entity({ block: 's-form', elem: 'button' }).mods({ type: 'submit' });

blocks.sendToReviewForm = InterviewSendToReviewForm.copy();

blocks.event = fEvent.copy();
blocks.tabs = fTabs.copy();

module.exports = pageObject.create(blocks);
