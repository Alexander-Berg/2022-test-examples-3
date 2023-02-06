const pageObject = require('bem-page-object');
const Entity = pageObject.Entity;
const blocks = require('../blocks/common');

/** Страница отдельной публикации */
blocks.pagePublication = new Entity({
    block: 'f-page-publication',
});
blocks.pagePublication.recommendationAction = new Entity({
    block: 'f-publication-action',
}).mods({
    action: 'recommend',
});

blocks.pagePublication.rotationAction = new Entity({
    block: 'f-publication-action',
}).mods({
    action: 'rotate',
});

module.exports = pageObject.create(blocks);
