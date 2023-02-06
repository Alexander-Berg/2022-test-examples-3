const { Entity, ReactEntity, create } = require('../../../../vendors/hermione');

const elems = {};

elems.poetry = new Entity({ block: 't-construct-adapter', elem: 'poetry-lover' });
elems.poetry.content = new ReactEntity({ block: 'Lyrics', elem: 'ContentDefault' });
elems.poetry.contentExpanded = new ReactEntity({ block: 'Lyrics', elem: 'ContentExpanded' });
elems.poetry.moreButton = new ReactEntity({ block: 'Lyrics', elem: 'Button' }).mods({ action: 'more' });
elems.poetry.lessButton = new ReactEntity({ block: 'Lyrics', elem: 'Button' }).mods({ action: 'less' });
elems.poetry.keyValue = new ReactEntity({ block: 'KeyValue' });
elems.poetry.keyValue.firstLink = new ReactEntity({ block: 'Link' });

module.exports = create(elems);
