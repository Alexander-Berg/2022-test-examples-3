const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

CommonObjects.modalContent = new Entity('.Modal-Content');

CommonObjects.tooltip = new Entity('.Tooltip');
CommonObjects.visibleTooltip = new Entity('.Tooltip_visible');

//вспылвающий диалог с приветствием при первом посещении
CommonObjects.welcomePopup = new Entity('.dialog.welcome');
CommonObjects.welcomePopup.dialog = new Entity('.Modal-Content');
CommonObjects.welcomePopup.closeButton = new Entity('.dialog__close');

//диалог с именем ресурса
CommonObjects.renameDialog = new Entity('.resource-rename-dialog');
CommonObjects.renameDialog.nameInput = new Entity('.rename-dialog__rename-form .Textinput-Control');
CommonObjects.renameDialog.submitButton = new Entity('button.confirmation-dialog__button_submit');
CommonObjects.renameDialog.closeButton = new Entity('.dialog__close');
CommonObjects.renameDialog.renameError = new Entity('.rename-dialog__rename-error');
CommonObjects.confirmRenameDialog = new Entity('.resource-rename-dialog__confirmation-dialog');
CommonObjects.confirmRenameDialog.submitButton = new Entity('.confirmation-dialog__button_submit');
CommonObjects.createDialog = new Entity('.resource-rename-dialog');
CommonObjects.createDialog.nameInput = new Entity('.rename-dialog__rename-form .Textinput-Control');
CommonObjects.createDialog.submitButton = new Entity('button.confirmation-dialog__button_submit');
CommonObjects.createDialog.closeButton = new Entity('.dialog__close');

CommonObjects.confirmationDialog = new Entity('.confirmation-dialog.Modal_visible');
CommonObjects.confirmationDialog.content = new Entity('.confirmation-dialog__content');
CommonObjects.confirmationDialog.content.title = new Entity('.client-confirmation-dialog__title');
CommonObjects.confirmationDialog.content.tree = new Entity('.ns-view-treeWrap');
CommonObjects.confirmationDialog.content.tree.item = new Entity('.b-tree__item');
CommonObjects.confirmationDialog.content.tree.item.inner = new Entity('.b-tree__item');
CommonObjects.confirmationDialog.submitButton = new Entity('.confirmation-dialog__button_submit');
CommonObjects.confirmationDialog.cancelButton = new Entity('.confirmation-dialog__button_cancel');

//диалог заголовка для нового альбома
CommonObjects.albumTitleDialog = new Entity('.album-title-dialog');
CommonObjects.albumTitleDialog.nameInput = new Entity('.rename-dialog__rename-form .Textinput-Control');
CommonObjects.albumTitleDialog.errorText = new Entity('.rename-dialog__rename-error');
CommonObjects.albumTitleDialog.submitButton = new Entity('.confirmation-dialog__button_submit');
CommonObjects.albumTitleDialog.closeButton = new Entity('.dialog__close');

//диалог с выбором папки, куда скопировать/переместить
CommonObjects.selectFolderDialog = new Entity('.select-folder-dialog');
CommonObjects.selectFolderDialog.notification = new Entity(
    // нотифайка, появляющаяся при перемещении безлимитного файла
    '.notification'
);
CommonObjects.selectFolderDialog.content = new Entity('.select-folder-dialog__tree');
CommonObjects.selectFolderDialog.treeContent = new Entity('.ns-view-tree .ns-view-container-desc');
CommonObjects.selectFolderDialog.nameInput = new Entity('.create-folder-form__input-wrapper input[type="text"');
CommonObjects.selectFolderDialog.submitButton = new Entity('.create-folder-form__submit');
CommonObjects.selectFolderDialog.acceptButton = new Entity('.confirmation-dialog__button_submit');
CommonObjects.selectFolderDialog.cancelButton = new Entity('.confirmation-dialog__button_cancel');
CommonObjects.selectFolderDialog.closeButton = new Entity('.dialog__close');
CommonObjects.selectFolderDialogItemsXpath = new Entity(
    // эквивалент '.select-folder-dialog__content div:contains(:titleText)', который wdio не поддерживает
    // concat с пробелами необходим, чтобы был выбран элемент с полным иминем класса
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " select-folder-dialog__content ")]// div[translate(text(), "\n", "") = ":titleText"]'
);
// селектор для кнопки раскрытия списка подпапок в диалоге выбора папки
CommonObjects.selectFolderDialogItemsToggleButtonXpath = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " select-folder-dialog__content ")]// div[translate(text(), "\n", "") = ":titleText"]/ ancestor::div[contains(concat(" ", normalize-space(@class), " "), " b-tree__item ")]/div[contains(concat(" ", normalize-space(@class), " "), " b-tree__toggle " and position() = 1)]'
);

CommonObjects.selectFolderDialogItemsTreeWaitingXpath = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " select-folder-dialog__content ")]// div[translate(text(), "\n", "") = ":titleText"]/ ancestor::div[contains(concat(" ", normalize-space(@class), " "), " b-tree__item_waiting ")]'
);

//диалог для скачивания большой папки с помощью ПО
CommonObjects.downloadBigFolderDialog = new Entity('.download-big-folder-dialog');
CommonObjects.downloadBigFolderDialog.content = new Entity('.Modal-Content');
CommonObjects.downloadBigFolderDialog.close = new Entity('.dialog__close');

//диалог приглашения пользователей в ОП
CommonObjects.accessPopup = new Entity('.access-folder-dialog .Modal-Content');
CommonObjects.accessPopup.suggestInput = new Entity('._nb-suggest-input');
CommonObjects.accessPopup.spinner = new Entity('.nb-loader-thin__spinner');
CommonObjects.accessPopup.inviteeFolder = new Entity('.ns-view-inviteeFolder');
CommonObjects.accessPopup.inviteeFolder.userBadge = new Entity('.b-user__badge');
CommonObjects.accessPopup.formSend = new Entity('.b-invite-form__send');
CommonObjects.accessPopup.formSend.sendButton = new Entity('.nb-button');

//Попап для ввода нового названия для папки при копировании
CommonObjects.confirmationPopup = new Entity('.resource-rename-dialog');
CommonObjects.confirmationPopup.nameInput = new Entity('.rename-dialog__rename-form .Textinput-Control');
CommonObjects.confirmationPopup.acceptButton = new Entity('button.confirmation-dialog__button_submit');

//загрузчик
CommonObjects.uploader = new Entity('.uploader');
CommonObjects.uploader.progressBarContainer = new Entity('.uploader-progress');
CommonObjects.uploader.progressText = new Entity('.uploader-progress__progress-primary');
CommonObjects.uploader.progressBar = new Entity('.uploader-progress__progress-bar');
CommonObjects.uploader.collapseState = new Entity('.uploader-progress__opened-state-label');
CommonObjects.uploader.replaceButton = new Entity('.upload-notification__button_action_replace');
CommonObjects.uploader.closeButton = new Entity('.uploader-progress__close-button');
CommonObjects.uploader.errorText = new Entity('.upload-item-state_error');
CommonObjects.uploader.itemPreview = new Entity('.resource-image');
CommonObjects.uploader.moreButton = new Entity('.groupable-buttons__more-button');

DesktopObjects.uploader = new Entity('.uploader');
DesktopObjects.uploader.publishButton = new Entity('.upload-listing-item__publish-button-with-text');

TouchObjects.uploader = new Entity('.uploader');
TouchObjects.uploader.publishButton = new Entity('.upload-listing-item__only-icon-publish-button');
CommonObjects.uploader.listingItem = new Entity('.listing__items .listing-item');
CommonObjects.uploader.listingItem.itemTitle = new Entity('.listing-item__title');
CommonObjects.uploader.listingItem.cancelButton = new Entity('.upload-item-state__cancel-upload-button');
CommonObjects.uploader.uploadedItem = new Entity('.upload-listing-item_uploaded');
CommonObjects.uploader.listingItem.hoverButton = new Entity('.upload-item-state__hover-button');
CommonObjects.uploader.uploaderNotifications = new Entity('.uploader-notifications');
CommonObjects.uploader.uploaderNotifications.uploaderPromoNotification =
    new Entity('.uploader-promo-notification__wrapper');
CommonObjects.uploader.uploaderNotifications.uploaderPromoNotification.closeButton =
    new Entity('.uploader-promo-notification__close-button');
CommonObjects.uploader.uploaderNotifications.doNotUploadButton =
    new Entity('.upload-notification__button_action_do-not-upload');
CommonObjects.uploader.uploadConfirmation = new Entity('.upload-confirmation');
CommonObjects.uploaderUploadConfirmationCancelButtonXpath = new Entity(
    '//*[contains(concat(" ", normalize-space(@class), " "), " upload-confirmation__button-group ")]' +
    '//span[text()="Отменить"]/..'
);
CommonObjects.uploader.uploaderNotifications.fileName = new Entity('.listing-item__title');
CommonObjects.uploader.uploaderNotifications.fileIcon = new Entity('.file-icon');
CommonObjects.uploader.uploaderNotifications.buyPlaceButton = new Entity('.upload-notification__button_action_buy');

CommonObjects.promoNotification = new Entity('.promo-notification');
CommonObjects.promoNotificationClose = new Entity('.promo-notification__close');

//всплывающая плашка с сообщением о завершении действия
CommonObjects.notifications = new Entity('.notifications__item');
CommonObjects.notifications.text = new Entity('.notifications__text');
CommonObjects.notifications.link = new Entity('.notifications__link');

//плашка прогресса операций
CommonObjects.operationsProgress = new Entity('.operations-progress');

CommonObjects.shareDialog = new Entity('.ShareDialog');
CommonObjects.shareDialog.title = new Entity('.Dialog-Title');
CommonObjects.shareDialog.accessAccordion = new Entity('.ShareDialog-AccessAccordion');
CommonObjects.shareDialog.trashButton = new Entity('.ShareDialogLinkField-TrashButton');
CommonObjects.shareDialog.accessTypeButton = new Entity('.ShareDialogAccessType-AddonAfter .Button2');
CommonObjects.shareDialog.radioBoxNotChecked = new Entity(
    '.ShareDialogAccessAccordion .Radiobox-Radio:not(.Radiobox-Radio_checked)'
);
CommonObjects.shareDialog.copyButton = new Entity('.ShareDialogButtons-CopyButton');
CommonObjects.shareDialog.onlyViewInfoIcon = new Entity('.OnyViewInfo-Icon');
CommonObjects.shareDialog.textInput = new Entity('.Textinput-Control');
CommonObjects.shareDialog.qrImage = new Entity('.QR-Image');
CommonObjects.shareDialog.closeButton = new Entity('.Dialog-Close');
CommonObjects.shareDialogEditAccessTypeTitle = new Entity(
    '//div[@class="ShareDialogAccessType-Title" and text() = "Редактирование"]'
);
CommonObjects.shareDialog.securitySettings = new Entity('.SecuritySettings');
CommonObjects.shareDialog.securitySettings.lock = new Entity('.ShareDialogAccessType-AddonAfter');
CommonObjects.shareDialog.securitySettings.forbidDownloadTumbler =
    new Entity('.SecuritySettings-ForbidDownloadTumbler');
CommonObjects.shareDialog.securitySettings.forbidDownloadTumbler.button = new Entity('.Tumbler-Button');
CommonObjects.shareDialog.securitySettings.availableUntilDropdownButton = new Entity('.AvailableUntilDropdown-Button');
CommonObjects.shareDialog.securitySettings.dateTimePicker = new Entity('.DateTimePickerPopup');
CommonObjects.shareDialog.securitySettings.dateTimePicker.calendar = new Entity('.Calendar');
CommonObjects.shareDialog.securitySettings.dateTimePicker.calendar.nextMonth =
    new Entity('.Calendar-NavigationTitle ~ .Calendar-NavigationAction');
CommonObjects.shareDialog.securitySettings.dateTimePicker.timeFieldHour =
    new Entity('.DateTimeField-Control .DateTimeField-EditableSegment:nth-child(1)');
CommonObjects.shareDialog.securitySettings.dateTimePicker.apply = new Entity('.Button2');
CommonObjects.shareDialogSecuritySettingsCalendarDate15 = new Entity(
    '//*[contains(@class, "Calendar-DateButton")]//span[translate(text(), "\n", "") = "15"]'
);

CommonObjects.shareDialogAvailableUnlim = new Entity('.AvailableUntilDropdown-MenuItem_unlim');
CommonObjects.shareDialogAvailableDay = new Entity('.AvailableUntilDropdown-MenuItem_day');
CommonObjects.shareDialogAvailableDay.right = new Entity('.AvailableUntilDropdown-MenuItem_Right');
CommonObjects.shareDialogAvailableWeek = new Entity('.AvailableUntilDropdown-MenuItem_week');
CommonObjects.shareDialogAvailableWeek.right = new Entity('.AvailableUntilDropdown-MenuItem_Right');
CommonObjects.shareDialogAvailableMonth = new Entity('.AvailableUntilDropdown-MenuItem_month');
CommonObjects.shareDialogAvailableMonth.right = new Entity('.AvailableUntilDropdown-MenuItem_Right');
CommonObjects.shareDialogAvailableCustom = new Entity('.AvailableUntilDropdown-MenuItem_custom');

DesktopObjects.shareDialog = new Entity('.ShareDialog .Modal-Content');
DesktopObjects.shareDialog.lastButton = new Entity('.ShareDialogButtons-Button:last-child');
DesktopObjects.shareDialog.shareButtons = new Entity('.ShareDialogButtons');

DesktopObjects.shareDialogNoFeatureTooltip = new Entity('.NoFeatureStub-Tooltip');
DesktopObjects.shareDialogNoFeatureTooltip.button = new Entity('.NoFeatureStub-TooltipButton');
DesktopObjects.shareDialogSecuritySettingsTooltip = new Entity('.SecuritySettings-Tooltip');

TouchObjects.shareDialog = new Entity('.ShareDialog .Drawer-Curtain, .ShareDialog .Drawer-Handle');

CommonObjects.confirmDialog = new Entity('.ConfirmDialog .Dialog-Wrap');
CommonObjects.confirmDialog.cancelButton = new Entity('.ConfirmDialog-Button_cancel');
CommonObjects.confirmDialog.submitButton = new Entity('.ConfirmDialog-Button_submit');
CommonObjects.confirmDialog.closeButton = new Entity('.Dialog-Close');

CommonObjects.messageBox = new Entity('.MessageBox');

//action-bar, всплывающая плашка, показывающая список действий над элементом
DesktopObjects.actionBar = new Entity('.resources-action-bar');
DesktopObjects.actionBar.publishButton = new Entity(
    '.ufo-resources-action-bar__primary-button_desktop.ufo-resources-action-bar__primary-button_action_publish'
);
DesktopObjects.actionBar.downloadButton = new Entity(
    '.ufo-resources-action-bar__primary-button_desktop.ufo-resources-action-bar__primary-button_action_download'
);
DesktopObjects.actionBar.deleteFromTrashButton = new Entity(
    // eslint-disable-next-line max-len
    '.ufo-resources-action-bar__primary-button_desktop.ufo-resources-action-bar__primary-button_action_delete-from-trash'
);
DesktopObjects.actionBar.restoreFromTrashButton = new Entity(
    '.ufo-resources-action-bar__primary-button_desktop.ufo-resources-action-bar__primary-button_action_restore'
);

DesktopObjects.visiblePopup = new Entity('.Popup2_visible');
DesktopObjects.sortPopup = new Entity('.listing-sort .Select2-Popup');
DesktopObjects.sortPopup.ascendingSortButton = new Entity('.Menu-Item[value="1"]');
DesktopObjects.sortPopup.descendingSortButton = new Entity('.Menu-Item[value="0"]');

DesktopObjects.actionBarInviteButton = new Entity('.groupable-buttons__menu-button_action_share-access');

DesktopObjects.overdraftDialog = new Entity('.dialog.overdraft');

DesktopObjects.subscriptionOnboarding = new Entity('.Subscription-Onboarding-Root');
DesktopObjects.subscriptionOnboarding.closeButton = new Entity('.Subscription-Onboarding-Close');

TouchObjects.actionBar = new Entity('.resources-action-bar');
TouchObjects.actionBar.publishButton = new Entity(
    '.ufo-resources-action-bar__primary-button_mobile.ufo-resources-action-bar__primary-button_action_publish'
);
TouchObjects.actionBar.downloadButton = new Entity(
    '.ufo-resources-action-bar__primary-button_mobile.ufo-resources-action-bar__primary-button_action_download'
);
TouchObjects.actionBar.moreButton = new Entity('.groupable-buttons__more-button');
//действия из Корзины для тача
TouchObjects.actionBar.deleteFromTrashButton = new Entity(
    '.ufo-resources-action-bar__primary-button_mobile.ufo-resources-action-bar__primary-button_action_delete-from-trash'
);
TouchObjects.actionBar.restoreFromTrashButton = new Entity(
    '.ufo-resources-action-bar__primary-button_mobile.ufo-resources-action-bar__primary-button_action_restore'
);

// action-bar, остальные кнопки, которые в зависимости
// от ширины области просмотра переносятся раскрывающийся список "больше"
CommonObjects.actionBar = new Entity('.resources-action-bar');
CommonObjects.actionBar.selectionInfoText = new Entity('.selection-info__text');
CommonObjects.actionBar.selectionInfoSpin = new Entity('.selection-info__spin');
CommonObjects.actionBar.infoButton = new Entity('.resources-info-dropdown .Button2');
CommonObjects.actionBar.renameButton = new Entity('.groupable-buttons__visible-button_name_rename');
CommonObjects.actionBar.moveButton = new Entity('.groupable-buttons__visible-button_name_move');
CommonObjects.actionBar.deleteButton = new Entity('.groupable-buttons__visible-button_name_delete');
CommonObjects.actionBar.copyButton = new Entity('.groupable-buttons__visible-button_name_copy');
CommonObjects.actionBar.editButton = new Entity('.groupable-buttons__visible-button_name_edit');
CommonObjects.actionBar.addToAlbumButton = new Entity('.groupable-buttons__visible-button_name_add-to-album');
CommonObjects.actionBar.moreButton = new Entity('button.groupable-buttons__more-button');
CommonObjects.actionBar.closeButton = new Entity('.resources-action-bar__close');
CommonObjects.actionBar.restoreFromTrashButton = new Entity('.ufo-resources-action-bar__primary-button_action_restore');
CommonObjects.actionBar.excludeFromAlbumButton =
    new Entity('.groupable-buttons__visible-button_name_exclude-from-album');
CommonObjects.actionBar.excludeFromPersonalAlbumButton =
    new Entity('.groupable-buttons__visible-button_name_exclude-from-personal-album');

//action-bar, всплывающая плашка, показывающая список действий над элементом
CommonObjects.actionPopup = new Entity('.resources-actions-popup');
CommonObjects.actionPopup.addToCurrentAlbumButton =
    new Entity('.resources-actions-popup__action_type_add-to-current-album');
CommonObjects.actionPopup.shareAccessButton = new Entity('.resources-actions-popup__action_type_share-access');
CommonObjects.actionPopup.downloadButton = new Entity('.resources-actions-popup__action_type_download');
CommonObjects.actionPopup.renameButton = new Entity('.resources-actions-popup__action_type_rename');
CommonObjects.actionPopup.moveButton = new Entity('.resources-actions-popup__action_type_move');
CommonObjects.actionPopup.copyButton = new Entity('.resources-actions-popup__action_type_copy');
CommonObjects.actionPopup.deleteButton = new Entity('.resources-actions-popup__action_type_delete');
CommonObjects.actionPopup.addToAlbumButton = new Entity('.resources-actions-popup__action_type_add-to-album');
CommonObjects.actionPopup.createAlbumButton = new Entity('.resources-actions-popup__action_type_create-album');
CommonObjects.actionPopup.excludeFromAlbumButton =
    new Entity('.resources-actions-popup__action_type_exclude-from-album');
CommonObjects.actionPopup.publishButton = new Entity('.resources-actions-popup__action_type_publish');
CommonObjects.actionPopup.editButton = new Entity('.resources-actions-popup__action_type_edit');
CommonObjects.actionPopup.viewButton = new Entity('.resources-actions-popup__action_type_view');
CommonObjects.actionPopup.setAsCoverButton = new Entity('.resources-actions-popup__action_type_set-as-cover');
CommonObjects.actionPopup.goToFileButton = new Entity('.resources-actions-popup__action_type_go-to-file');
CommonObjects.actionPopup.versionsButton = new Entity('.resources-actions-popup__action_type_versions');
CommonObjects.actionPopup.removeFromDocsButton = new Entity('.resources-actions-popup__action_type_remove-from-docs');

//список действий, доступный из меню кнопки "больше" action-bar'a
CommonObjects.actionBarMorePopup = new Entity('.groupable-buttons__more-button-popup');
CommonObjects.actionBarMorePopup.cancelAllButton = new Entity(
    '.groupable-buttons__menu-button_action_cancel_all_uploads'
);
CommonObjects.actionBarMorePopup.menu = new Entity('.groupable-buttons__menu-buttons');
CommonObjects.actionBarMorePopup.renameButton = new Entity('.groupable-buttons__menu-button_action_rename');
CommonObjects.actionBarMorePopup.moveButton = new Entity('.groupable-buttons__menu-button_action_move');
CommonObjects.actionBarMorePopup.copyButton = new Entity('.groupable-buttons__menu-button_action_copy');
CommonObjects.actionBarMorePopup.deleteButton = new Entity('.groupable-buttons__menu-button_action_delete');
CommonObjects.actionBarMorePopup.copyButton = new Entity('.groupable-buttons__menu-button_action_copy');
CommonObjects.actionBarMorePopup.editButton = new Entity('.groupable-buttons__menu-button_action_edit');
CommonObjects.actionBarMorePopup.versionsButton = new Entity('.groupable-buttons__menu-button_action_versions');
CommonObjects.actionBarMorePopup.excludeFromAlbumButton =
    new Entity('.groupable-buttons__menu-button_action_exclude-from-album');
CommonObjects.actionBarMorePopup.excludeFromPersonalAlbumButton =
    new Entity('.groupable-buttons__menu-button_action_exclude-from-personal-album');
CommonObjects.actionBarMorePopup.goToFile = new Entity('.groupable-buttons__menu-button_action_go-to-file');
CommonObjects.actionBarMorePopup.goToFolder = new Entity('.groupable-buttons__menu-button_action_go-to-folder');
CommonObjects.actionBarMorePopup.addToAlbumButton = new Entity('.groupable-buttons__menu-button_action_add-to-album');
CommonObjects.actionBarMorePopup.createAlbumButton = new Entity('.groupable-buttons__menu-button_action_create-album');
CommonObjects.actionBarMorePopup.setAsCoverButton = new Entity('.groupable-buttons__menu-button_action_set-as-cover');
CommonObjects.actionBarMorePopup.versionsButton = new Entity('.groupable-buttons__menu-button_action_versions');

CommonObjects.selectionInfoLimitTooltip = new Entity('.selection-info__tooltip');

//Попап очистки корзины
CommonObjects.cleanTrashPopup = new Entity('.client-confirmation-dialog');
CommonObjects.cleanTrashPopup.acceptButton = new Entity('.confirmation-dialog__button_submit');

// Попап при удалении файла
CommonObjects.deletePopup = new Entity('.client-confirmation-dialog');
CommonObjects.deletePopup.acceptButton = new Entity('.confirmation-dialog__button_submit');
CommonObjects.deletePopup.deleteAllButton = new Entity('.confirmation-dialog__button_extra');

// Попап выбора папки
CommonObjects.selectFolderPopup = new Entity('.select-folder-dialog');
CommonObjects.selectFolderPopup.content = new Entity('.Modal-Content');
CommonObjects.selectFolderPopup.warningTooltip = new Entity('.select-folder-dialog__warning');
CommonObjects.selectFolderPopup.warningTooltip.hide = new Entity('.MessageBox-Close');
CommonObjects.selectFolderPopup.listing = new Entity('.select-folder-listing');
CommonObjects.selectFolderPopup.listing.currentItem = new Entity('.listing-item_current');
CommonObjects.selectFolderPopup.listing.spin = new Entity('.load-portions__spin-wrapper');
CommonObjects.selectFolderPopupListingItemInfoXpath = new Entity(
    // эквивалент '.listing-item__info span:contains(:titleText)', который wdio не поддерживает
    // concat с пробелами необходим, чтобы был выбран элемент с полным именем класса
    // eslint-disable-next-line max-len
    '//*[contains(@class, "select-folder-listing")]//*[contains(concat(" ", normalize-space(@class), " "), " listing-item__info ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"]'
);

// Содердимое попапа или мобильной панельки информации о ресурсе в топбаре или слайдере
CommonObjects.resourceInfoDropdownContent = new Entity('.resources-info-dropdown__content');
CommonObjects.resourceInfoDropdownContent.fileName =
    new Entity('.ufo-resource-info-dropdown__name .ufo-resource-info-dropdown__value');
CommonObjects.resourceInfoDropdownContent.viewCount =
    new Entity('.ufo-resource-info-dropdown__views .ufo-resource-info-dropdown__value');
CommonObjects.resourceInfoDropdownContent.downloadCount =
    new Entity('.ufo-resource-info-dropdown__downloads .ufo-resource-info-dropdown__value');
CommonObjects.resourceInfoDropdownContent.size =
    new Entity('.ufo-resource-info-dropdown__size .ufo-resource-info-dropdown__value');
CommonObjects.resourceInfoDropdownFooter = new Entity('.resources-info-dropdown__footer');
CommonObjects.resourceInfoDropdownFooter.goToResource = new Entity('.ufo-resource-info-dropdown__go-to-link');

//диалог выбора альбома
CommonObjects.selectAlbumDialog = new Entity('.select-album-dialog');
CommonObjects.selectAlbumDialog.content = new Entity('.Modal-Content');
CommonObjects.selectAlbumDialog.title = new Entity('.select-album-dialog__title');
CommonObjects.selectAlbumDialog.title.backIcon = new Entity('.icon');
CommonObjects.selectAlbumDialog.album = new Entity('.albums2__item');
CommonObjects.selectAlbumDialog.createAlbum = new Entity('.albums2__item_new');
CommonObjects.selectAlbumDialog.album = new Entity('.albums2__item');
CommonObjects.selectAlbumDialog.albumByName = new Entity('.albums2__item[title=":title"]');
CommonObjects.selectAlbumDialog.favoritesAlbum = new Entity('.albums2__item_favorites');
CommonObjects.selectAlbumDialog.closeButton = new Entity('.dialog__close');
CommonObjects.selectAlbumDialog.preview = new Entity('.albums2__item-preview .scalable-preview__image');

CommonObjects.updateEditorConfirm = new Entity('.update-editor-confirm .Modal-Content');
CommonObjects.updateEditorConfirm.cancel = new Entity('.ConfirmDialog-Button_cancel');

//всплывающая подложка после нажатия кнопки "+ создать" с типами создаваемых элементов
DesktopObjects.createPopup = new Entity('.create-resource-popup-with-anchor__popup');
DesktopObjects.createPopup.createDirectory = new Entity('.file-icon_dir_plus');
DesktopObjects.createPopup.createDocument = new Entity('.file-icon_doc');
DesktopObjects.createPopup.createTable = new Entity('.file-icon_xls');
DesktopObjects.createPopup.createPresentation = new Entity('.file-icon_ppt');
DesktopObjects.createPopup.createAlbum = new Entity('.file-icon_album');

//контекстное меню создания ресурса
DesktopObjects.contextMenuCreatePopup = new Entity('.context-menu-create-popup');
DesktopObjects.contextMenuCreatePopup.createDirectory = new Entity('.context-menu-create-popup__item_new-folder');
DesktopObjects.contextMenuCreatePopup.createAlbum = new Entity('.context-menu-create-popup__item_album');
DesktopObjects.contextMenuCreatePopup.createWord = new Entity('.context-menu-create-popup__item_word');
DesktopObjects.contextMenuCreatePopup.createExcel = new Entity('.context-menu-create-popup__item_excel');
DesktopObjects.contextMenuCreatePopup.createPowerPoint = new Entity('.context-menu-create-popup__item_powerpoint');
DesktopObjects.contextMenuCreatePopup.upload = new Entity('.context-menu-create-popup__menu-item_type_upload');
DesktopObjects.contextMenuCreatePopup.upload.uploadInput = new Entity(
    '.Menu-Text .context-menu-create-popup__upload-input-wrapper .context-menu-create-popup__upload-input'
);

//промо попап о временной скидке
DesktopObjects.discountPromoPopup = new Entity('.discount-promo');
DesktopObjects.discountPromoPopup.laterButton = new Entity('.discount-promo__later-button');
DesktopObjects.discountPromoPopup.buyButton = new Entity('.discount-promo__buy-button');

//попап календаря в разделе "История"
DesktopObjects.calendar = new Entity('.calendar');
DesktopObjects.calendar.title = new Entity('.calendar__selected-month');
DesktopObjects.calendar.prevMonthButton = new Entity('.calendar__prev-button');
// xpath для элемента таблицы с конкретным числом
DesktopObjects.calendarDay = new Entity(
    '//*[text() = ":titleText" and contains(@class, "calendar__cell")]'
);
// попап для списка вариантов, появляющийся при клике на фильтр по действиям и устройствам
DesktopObjects.selectPopup = new Entity('.Select2-Popup');
// xpath для элемента списка
DesktopObjects.selectPopupItem = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " Select2-Popup ")]//*[contains(concat(" ", normalize-space(@class), " "), " Menu-Item ")]//*[translate(text(), "\n", "") = ":titleText"]'
);

//диалог приглашения к папке по email
DesktopObjects.invitePopup = new Entity('.access-folder-dialog');
DesktopObjects.invitePopup.emailInput = new Entity('.b-invite-form__input .nb-input');
DesktopObjects.invitePopup.inviteByEmailButton = new Entity('.b-invite-form__send .nb-button:not(._nb-is-disabled)');
DesktopObjects.invitePopup.closeButton = new Entity('.dialog__close');

//попап, который появляется при наведении курсора на иконку публичности ресурса
DesktopObjects.shareLinkButtonTooltip = new Entity('.Tooltip-Content');
DesktopObjects.shareLinkButtonTooltip.viewsCount =
    new Entity('.share-link-button__tooltip-row:nth-child(1) .share-link-button__tooltip-counter');
DesktopObjects.shareLinkButtonTooltip.downloadsCount =
    new Entity('.share-link-button__tooltip-row:nth-child(2) .share-link-button__tooltip-counter');

//любая видимая мобильная панель
TouchObjects.mobilePaneVisible = new Entity('.Drawer_visible');

//меню, открывающееся после нажатия кнопки more в action bar'e
TouchObjects.moreButtonPopup = new Entity('.groupable-buttons__menu-buttons');
TouchObjects.moreButtonPopup.renameButton = new Entity('.groupable-buttons__menu-button_action_rename');
TouchObjects.moreButtonPopup.moveButton = new Entity('.groupable-buttons__menu-button_action_move');
TouchObjects.moreButtonPopup.deleteButton = new Entity('.groupable-buttons__menu-button_action_delete');
TouchObjects.moreButtonPopup.copyButton = new Entity('.groupable-buttons__menu-button_action_copy');
TouchObjects.moreButtonPopup.showFullsizeButton = new Entity('.groupable-buttons__menu-button_action_show-fullsize');
TouchObjects.moreButtonPopup.versionsButton = new Entity('.groupable-buttons__menu-button_action_versions');

//всплывающая подложка после нажатия "+" с типами добавляемых элементов
TouchObjects.createPopup = new Entity('.Drawer.touch-listing-settings__pane');
TouchObjects.createPopup.createDirectory = new Entity('.touch-listing-settings__add-pane-button_create-folder');
TouchObjects.createPopup.createAlbum = new Entity('.touch-listing-settings__add-pane-button_create-album');
TouchObjects.createPopup.uploadFile = new Entity('.touch-listing-settings__add-pane-button_upload-files');
TouchObjects.createPopup.uploadFile.input = new Entity('input[type="file"]');

//подложка настроек вида и сортировки, всплывающая после нажатия "..."
TouchObjects.settingsPopup = new Entity('.touch-listing-settings__pane');
TouchObjects.settingsPopup.tile = new Entity('.touch-listing-settings__listing-type_tile');
TouchObjects.settingsPopup.icons = new Entity('.touch-listing-settings__listing-type_icons');
TouchObjects.settingsPopup.list = new Entity('.touch-listing-settings__listing-type_list');
TouchObjects.settingsPopupAscendingSortButton = new Entity(
    '//div[contains(@class, \'ListTile-Wrapper\') and text() = \'Возрастанию\']'
);
TouchObjects.settingsPopupDescendingSortButton = new Entity(
    '//div[contains(@class, \'ListTile-Wrapper\') and text() = \'Убыванию\']'
);
//промо мобильных на таче
TouchObjects.promoAppPopup = new Entity('.promo-mobile');
TouchObjects.promoAppPopup.skip = new Entity('._link');

TouchObjects.promoUnlim = new Entity('.promo-mobile_unlim-photos');
TouchObjects.promoUnlim.skip = new Entity('._link');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    desktop: bemPageObject.create(DesktopObjects),
    touch: bemPageObject.create(TouchObjects)
};
