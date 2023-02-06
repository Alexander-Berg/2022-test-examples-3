const Entity = require('bem-page-object').Entity;
const blocks = require('./common');

const fApplication = new Entity({ block: 'f-application' });

fApplication.header = new Entity({ block: 'f-application', elem: 'header' });
fApplication.fMessageForm = blocks.fMessageForm.copy();

module.exports = fApplication;
