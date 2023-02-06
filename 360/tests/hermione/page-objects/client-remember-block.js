const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};

CommonObjects.block = new Entity('.root_page_remember-block');
CommonObjects.blockInner = new Entity('.root__content-inner_page_remember-block');
CommonObjects.title = new Entity('.remember-block__title');
CommonObjects.resource = new Entity('.remember-block__resource');
CommonObjects.wowResource = new Entity('.remember-block__resource_wow');
CommonObjects.wowResource.preview = new Entity('img');
CommonObjects.photoSliceButton = new Entity('.remember-block__photoslice-button');
CommonObjects.shareAlbumButton = new Entity('.remember-block__create-album-button');
CommonObjects.shareAlbumButtonProgress = new Entity('.remember-block__create-album-button.Button2_progress');
CommonObjects.sharedAlbumLink = new Entity('.remember-block__album-url');
CommonObjects.stub = new Entity('.remember-block__stub');

module.exports = {
    common: bemPageObject.create(CommonObjects)
};
