const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

// Футер
elems.entityFooter = new ReactEntity({ block: 'EntityFeedbackFooter' });
elems.entityFooter.wrapper = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Wrapper' });
elems.entityFooter.abuseLink = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' }).lastChild();
elems.entityFooter.link = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' });
elems.entityFooter.firstLink = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' }).nthChild(1);
elems.entityFooter.secondLink = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' }).nthChild(2);
elems.entityFooter.lastLink = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' }).lastChild();
elems.entityFooter.reclaimLink = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' }).mods({ type: 'Reclaim' });
elems.entityFooter.musicCondLink = new ReactEntity({ block: 'EntityFeedbackFooter', elem: 'Link' }).mods({ type: 'MusicCond' });

// Диалог внутри модального окна с компонентами шторки
elems.feedbackDialog = new ReactEntity({ block: 'FeedbackDialog' });
elems.feedbackDialog.header = new ReactEntity({ block: 'FeedbackDialog', elem: 'Header' });
elems.feedbackDialog.header.back = new ReactEntity({ block: 'FeedbackDialog', elem: 'Back' });
elems.feedbackDialog.header.close = new ReactEntity({ block: 'FeedbackDialog', elem: 'Close' });
elems.feedbackDialog.header.title = new ReactEntity({ block: 'FeedbackDialog', elem: 'Title' });
elems.feedbackDialog.body = new ReactEntity({ block: 'FeedbackDialog', elem: 'Body' });

elems.feedbackDialog.button = new ReactEntity({ block: 'FeedbackDialog', elem: 'Button' });
elems.feedbackDialog.lastButton = elems.feedbackDialog.button.lastChild();

elems.feedbackDialog.link = new ReactEntity({ block: 'FeedbackDialog', elem: 'Link' });
elems.feedbackDialog.email = new ReactEntity({ block: 'FeedbackDialog', elem: 'Email' });
elems.feedbackDialog.attach = new ReactEntity({ block: 'FeedbackDialog', elem: 'Attach' });
elems.feedbackDialog.attach.control = new ReactEntity({ block: 'Attach-Control' });
elems.feedbackDialog.wrapper = new ReactEntity({ block: 'FeedbackDialog', elem: 'Wrapper' });
elems.feedbackDialog.message = new ReactEntity({ block: 'FeedbackDialog', elem: 'Textarea' });
elems.feedbackDialog.checkboxList = new ReactEntity({ block: 'FeedbackDialog', elem: 'CheckboxList' });
elems.feedbackDialog.checkbox = new ReactEntity({ block: 'FeedbackDialog', elem: 'Checkbox' });
const box = new ReactEntity({ block: 'Checkbox', elem: 'Box' });
elems.feedbackDialog.firstCheckbox = elems.feedbackDialog.checkbox.copy().nthChild(1);
elems.feedbackDialog.firstCheckbox.box = box.copy();
elems.feedbackDialog.secondCheckbox = elems.feedbackDialog.checkbox.copy().nthChild(2);
elems.feedbackDialog.secondCheckbox.box = box.copy();
elems.feedbackDialog.thirdCheckbox = elems.feedbackDialog.checkbox.copy().nthChild(3);
elems.feedbackDialog.thirdCheckbox.box = box.copy();
elems.feedbackDialog.fourthCheckbox = elems.feedbackDialog.checkbox.copy().nthChild(4);
elems.feedbackDialog.fourthCheckbox.box = box.copy();
elems.feedbackDialog.fifthCheckbox = elems.feedbackDialog.checkbox.copy().nthChild(5);
elems.feedbackDialog.fifthCheckbox.box = box.copy();

elems.modal = new ReactEntity({ block: 'Modal' });
elems.modal.content = new ReactEntity({ block: 'Modal', elem: 'Content' });

module.exports = elems;
