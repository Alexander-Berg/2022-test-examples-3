const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};

CommonObjects.docsViewLoading = new Entity('.Docs-View__Loading');
CommonObjects.docsViewFrame = new Entity('.Docs-View__Frame');

CommonObjects.docviewer = new Entity('#app > .embed-docs');
CommonObjects.docviewer.title = new Entity('> div[class^="wrapper_"]');

CommonObjects.docviewer.viewport = new Entity('div[class^="viewport_"]');
CommonObjects.docviewer.page = new Entity('.js-doc-page');
CommonObjects.docviewer.page.img = new Entity('img');
CommonObjects.docviewer.pageCounter = new Entity('div[class^="pageCounterWrapper_"]');
CommonObjects.docviewer.pageCounter.text = new Entity('> span[class^="pageCounter_"]');
CommonObjects.docviewer.pageCounter.input = new Entity('> input');
CommonObjects.docviewer.upButton = new Entity('[class*="upButton_"]');

CommonObjects.docsViewSaveModal = new Entity('.Docs-View-Save-Modal__Content');

// ToDo: сделать в DV нормальных селекторов и поменять тут после выкатки CHEMODAN-80281 в прод
CommonObjects.docviewerEditButton =
    new Entity('//span[text() = \'Редактировать\' and contains(@class, \'Button2-Text\')]/parent::*');
CommonObjects.docviewerEditCopyButton =
    new Entity('//span[text() = \'Редактировать копию\' and contains(@class, \'Button2-Text\')]/parent::*');
CommonObjects.saveToDiskButton =
    new Entity('//span[text() = \'Сохранить на Яндекс Диск\' and contains(@class, \'Button2-Text\')]/parent::*');
CommonObjects.openDiskButton =
    new Entity('//span[text() = \'Открыть Яндекс Диск\' and contains(@class, \'Button2-Text\')]/parent::*');

module.exports = {
    common: bemPageObject.create(CommonObjects)
};
