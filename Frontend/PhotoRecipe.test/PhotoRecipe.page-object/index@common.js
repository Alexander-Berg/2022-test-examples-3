const { Entity } = require('@yandex-int/bem-page-object');
const { organic } = require('../../../../../hermione/page-objects/common/construct/organic');

const elems = {};

elems.recipePhoto = new Entity({ block: 't-construct-adapter', elem: 'photo-recipe' })
    .not(new Entity({ block: 'card', elem: 'narrow' }));
elems.recipePhoto.title = organic.title.link.copy();
elems.recipePhoto.greenUrl = organic.greenurlLink.copy();
elems.recipePhoto.thumb = organic.thumb.copy();

elems.imagesViewer2 = new Entity({ block: 'images-touch-viewer2' });
elems.imagesViewer2.closeButton = new Entity({ block: 'images-touch-viewer2', elem: 'close' });
elems.imagesViewer2.activeItem = new Entity({ block: 'viewer-item2', modName: 'active', modVal: 'yes' });
elems.imagesViewer2.activeItem.panel = new Entity({ block: 'images-viewer-content2', elem: 'panel' });
elems.imagesViewer2.activeItem.domain = new Entity({ block: 'images-viewer-content2', elem: 'domain-url' });
elems.imagesViewer2.activeItem.open = new Entity({ block: 'images-viewer-content2', elem: 'save' });

module.exports = elems;
