const Entity = require('bem-page-object').Entity;

const fInfo = new Entity({ block: 'f-info' });
fInfo.body = new Entity({ block: 'f-info', elem: 'body' });
fInfo.closer = new Entity({ block: 'f-info', elem: 'closer' });

module.exports = fInfo;
