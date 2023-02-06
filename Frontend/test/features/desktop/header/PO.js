const { create, Entity } = require('bem-page-object');

const PO = {};

PO.header = new Entity({ block: 'm-head' });

PO.messenger = new Entity({ block: 'messenger' });

module.exports = create(PO);
