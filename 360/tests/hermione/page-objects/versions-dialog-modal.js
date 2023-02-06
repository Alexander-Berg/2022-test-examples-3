const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const TouchObjects = {};

//модальное окно версионирования
CommonObjects.versionsDialog = new Entity('.versions-dialog__modal');
CommonObjects.versionsDialog.payForNewVersionsButton = new Entity('.versions-dialog__pay-for-new-versions-button');
CommonObjects.versionsDialog.buttonX = new Entity('.versions-dialog__close');
CommonObjects.versionsDialog.versionItem = new Entity('.version-row__row');
CommonObjects.versionsDialog.versionItemWithActions = new Entity('.version-row__row_with-actions');
//стрелочка для разворачивания списка версий
CommonObjects.versionsDialog.versionExpand = new Entity('.version-row__version-expand-arrow');
//подсписок версий
CommonObjects.versionsDialog.versionSublist = new Entity('.version-row__sublist');
//кнопка "Открыть"
CommonObjects.versionsDialog.openButton = new Entity('.version-row__version-button_type_open');
//кнопка "Восстановить"
CommonObjects.versionsDialog.restoreButton = new Entity('.version-row__version-button_type_restore');

//диалог подтверждения восстановления версии
CommonObjects.versionsRestoreDialog = new Entity('.restore-confirm-dialog');
CommonObjects.versionsRestoreDialog.modalContent = new Entity('.Modal-Content');
CommonObjects.versionsRestoreDialog.buttonX = new Entity('.dialog__close');
CommonObjects.versionsRestoreDialog.saveAsCopyButton = new Entity('.restore-confirm-dialog__button-save-copy');
CommonObjects.versionsRestoreDialog.content = new Entity('.restore-confirm-dialog__content');
CommonObjects.versionsRestoreDialog.content.date = new Entity('.restore-confirm-dialog__date');
CommonObjects.versionsRestoreDialog.content.time = new Entity('.restore-confirm-dialog__time');

//меню Ещё для управления версиями в тачах
TouchObjects.moreButton = new Entity('.version-row__menu-button');
TouchObjects.openButton = new Entity('.versions-button-menu__item_type_open');
TouchObjects.restoreButton = new Entity('.versions-button-menu__item_type_restore');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    touch: bemPageObject.create(TouchObjects)
};
