const Entity = require('bem-page-object').Entity;

const fRemovable = new Entity({ block: 'f-removable' });
fRemovable.delete = new Entity({ block: 'f-removable', elem: 'delete' });

module.exports = fRemovable;
