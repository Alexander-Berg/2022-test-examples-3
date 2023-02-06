const Entity = require('../Entity').ReactEntity;
const { blocks: rBlocks, methods } = require('./SForm');

const BLOCK_NAME = 'InterviewSendToReviewForm';

const sendToReviewForm = new Entity({ block: BLOCK_NAME });

sendToReviewForm.submit = new Entity({
    block: BLOCK_NAME,
    elem: 'Button',
}).mods({ type: 'submit' });

sendToReviewForm.formError = rBlocks.formError.copy();
sendToReviewForm.commentField = methods.getSFieldOfName('comment');

module.exports = sendToReviewForm;
