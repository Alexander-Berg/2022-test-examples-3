const { Entity, ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.crocodileGame = new Entity({ block: 't-construct-adapter', elem: 'crocodile-game' });

elems.crocodileGame.value = new ReactEntity({ block: 'CrocodileGame-Word' });

elems.crocodileGame.submitButton = new ReactEntity({ block: 'Button2', modName: 'type', modType: 'submit' });

elems.crocodileGame.reportButton = new ReactEntity({ block: 'ExtraActions', elem: 'ReportItem' });

elems.feedbackDialog = new ReactEntity({ block: 'FeedbackDialog' });

module.exports = elems;
