const Entity = require('bem-page-object').Entity;

const fMessages = new Entity({ block: 'f-messages' });

fMessages.list = new Entity({ block: 'f-messages', elem: 'list' });
fMessages.list.firstFMessage = new Entity(':nth-child(2)');

module.exports = { fMessages };
