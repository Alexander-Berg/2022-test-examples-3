const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;
const PageObjects = {};

PageObjects.publicMain = new Entity('.public__main');
PageObjects.error = new Entity('.error');
PageObjects.fileName = new Entity('.file-name');
PageObjects.desktopToolbar = new Entity('.action-buttons');
PageObjects.desktopToolbar.saveButton = new Entity('.save-button_save');
PageObjects.desktopToolbar.downloadButton = new Entity('.action-buttons__button_download');

module.exports = bemPageObject.create(PageObjects);
