const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;
const PageObjects = {};

PageObjects.loginLinkButton = new Entity('.PSHeader-NoLoginButton');
PageObjects.legoUser = new Entity('.legouser');
PageObjects.legoUser.userPic = new Entity('.user-pic');
PageObjects.legoUser.addAccount = new Entity('.legouser__add-account');
PageObjects.legoUser.ticker = new Entity('.user-account__ticker');
PageObjects.legoUser.changeUser = new Entity('.legouser__account');
PageObjects.legoUser.popup = new Entity('.legouser__popup');
PageObjects.legoUser.popup.counter = new Entity('.legouser__menu-counter');
PageObjects.legoUser.popup.logout = new Entity('.legouser__menu-item_action_exit');

PageObjects.fileName = new Entity('.file-name');
PageObjects.audioFileName = new Entity('.audio-player__resource-name');
PageObjects.imagePreview = new Entity('.content__image-preview');
PageObjects.docPreview = new Entity('.document-preview');

PageObjects.audioPlayer = new Entity('.audio-player');
PageObjects.audioPlayer.duration = new Entity('.audio-player__time_duration');

PageObjects.slider = new Entity('.slider');
PageObjects.slider.a11y = new Entity('.slider__a11y');
PageObjects.slider.sliderButtonX = new Entity('.slider__button_close');
PageObjects.slider.sliderButtonDownload = new Entity('.slider__button_download');
PageObjects.slider.sliderButtonSave = new Entity('.slider__button_save');
PageObjects.slider.openDiskButton = new Entity('.save-button_open');
PageObjects.slider.sliderButtonMore = new Entity('.slider__button_more');
PageObjects.slider.activeItem = new Entity('.slider__item_active');
PageObjects.slider.activeItem.previewInSlider = new Entity('.scalable-preview__image');
PageObjects.slider.activeItem.resourcePreview = new Entity('.resource-preview');
PageObjects.slider.activeItem.icon = new Entity('.file-icon');
PageObjects.slider.activeItem.resourcePreview.openButton = new Entity('.resource-preview__open-button');
PageObjects.slider.activePreview = new Entity('.scalable-preview_active');
PageObjects.slider.activePreview.image = new Entity('.scalable-preview__image');
PageObjects.slider.nextImage = new Entity('.switch-arrow-button_right');
PageObjects.slider.previousImage = new Entity('.switch-arrow-button_left');
PageObjects.slider.items = new Entity('.slider__items');
PageObjects.slider.audioPlayer = new Entity('.audio-player');

PageObjects.mobileBannerInstallApp = new Entity('.banner_install');
PageObjects.mobileBannerOpenInApp = new Entity('.banner_open');

PageObjects.mail360PromoBanner = new Entity('.mail360-banner');

PageObjects.mail360AntiFOTooltip = new Entity('.mail360-antifo-tooltip');
PageObjects.mail360AntiFOTooltip.close = new Entity('.mail360-antifo-tooltip__close');
PageObjects.mail360AntiFOTooltip.content = new Entity('.mail360-antifo-tooltip__content');

PageObjects.loginButton = new Entity('.PSHeader-NoLoginButton');
PageObjects.moreButton = new Entity('.more-button');

PageObjects.fileMenu = new Entity('.file-menu');
PageObjects.fileMenuOpenDV = new Entity('.file-menu__open-dv');
PageObjects.fileMenuDownload = new Entity('.file-menu__download');
PageObjects.fileMenuFileInfo = new Entity('.file-menu__file-info');

// в тачах вместо попапа открывается всплыващая панель
PageObjects.fileMenuPane = new Entity('.file-menu-pane');
PageObjects.fileMenuPane.icons = new Entity('.file-menu-pane__item_icons');
PageObjects.fileMenuPane.list = new Entity('.file-menu-pane__item_list');
PageObjects.fileMenuPane.wow = new Entity('.file-menu-pane__item_wow');

PageObjects.infoBlock = new Entity('.pane_type_file-info');
PageObjects.infoBlock.title = new Entity('.pane__title');
PageObjects.infoBlock.owner = new Entity('.info-pane__row_owner');
PageObjects.infoBlock.size = new Entity('.info-pane__row_size');
PageObjects.infoBlock.modified = new Entity('.info-pane__row_modified');
PageObjects.infoBlock.views = new Entity('.info-pane__row_views');
PageObjects.infoBlock.downloads = new Entity('.info-pane__row_downloads');
PageObjects.infoBlock.viruses = new Entity('.info-pane__row_virus');
PageObjects.infoBlock.fileCount = new Entity('.info-pane__row_file-count');

PageObjects.toolbar = new Entity('.bottom-toolbar');
PageObjects.toolbar.saveButton = new Entity('.save-button_save');
PageObjects.toolbar.openDiskButton = new Entity('.save-button_open');
PageObjects.toolbar.snackbarText = new Entity('.bottom-toolbar__snackbar-text');
PageObjects.toolbar.downloadButton = new Entity('.action-buttons__button_download');
PageObjects.toolbar.saveAndDownloadButton = new Entity('.save-button_save-and-download');
PageObjects.toolbar.snackbarAntiFo = new Entity('.bottom-toolbar__snackbar_type_text');
PageObjects.toolbar.snackbarError = new Entity('.bottom-toolbar__snackbar_type_error-text');

PageObjects.desktopToolbar = new Entity('.content .action-buttons');
PageObjects.desktopToolbar.saveButton = new Entity('.save-button_save');
PageObjects.desktopToolbar.downloadButton = new Entity('.action-buttons__button_download');
PageObjects.desktopToolbar.openDiskButton = new Entity('.save-button_open');
PageObjects.desktopToolbar.saveAndDownloadButton = new Entity('.save-button_save-and-download');
PageObjects.antiFoTooltip = new Entity('.action-buttons__antifo-tooltip');
PageObjects.snackbarText = new Entity('.bottom-toolbar__snackbar .notification_active');

// переключение типа листинга на десктопе
PageObjects.desktopListingType = new Entity('.listing-type');
PageObjects.desktopListingType.tile = new Entity('.listing-type__icon_tile');
PageObjects.desktopListingType.icons = new Entity('.listing-type__icon_icons');
PageObjects.desktopListingType.list = new Entity('.listing-type__icon_list');

PageObjects.desktopListingTypeButton = new Entity('.ListingTypeSelect .Button2');
PageObjects.desktopListingTypeMenu = new Entity('.ListingTypeSelect__Menu');
PageObjects.desktopListingTypeMenu.tile = new Entity('.Menu-Item[value=tile]');
PageObjects.desktopListingTypeMenu.icons = new Entity('.Menu-Item[value=icons]');
PageObjects.desktopListingTypeMenu.list = new Entity('.Menu-Item[value=list]');
PageObjects.desktopListingTypeMenu.wow = new Entity('.Menu-Item[value=wow]');

PageObjects.content = new Entity('.content');
PageObjects.content.folderName = new Entity('.folder-content__header-name');
PageObjects.content.header = new Entity('.folder-content__header');
PageObjects.content.header.back = new Entity('.listing-item_type_dir');
PageObjects.content.header.backButton = new Entity('.folder-content__back-button');
PageObjects.content.empty = new Entity('.folder-content__empty');

PageObjects.wowGrid = new Entity('.public-grid');
PageObjects.wowGrid.group = new Entity('.public-grid-group');
PageObjects.wowGrid.item = new Entity('.public-grid-item-preview');
PageObjects.wowGrid.item.preview = new Entity('.scalable-preview__image');
PageObjects.wowGrid.spinner = new Entity('.load-portions__spin');

PageObjects.listing = new Entity('.public-listing');
PageObjects.listing.subfolder = new Entity('.listing .listing-item_type_dir');
PageObjects.listing.listingItems = new Entity('.listing__items');
PageObjects.listing.listingItem = new Entity('.listing-item');
PageObjects.listing.listingItemTile = new Entity('.listing-item_theme_tile.listing-item_size_l');
PageObjects.listing.listingItemIcons = new Entity('.listing-item_theme_tile.listing-item_size_m');
PageObjects.listing.listingItemList = new Entity('.listing-item_theme_row');
PageObjects.listing.spinner = new Entity('.load-portions__spin');

PageObjects.listingItemXpath = new Entity(
    // эквивалент '.listing-item span:contains(:titleText)', который wdio не поддерживает
    // concat с пробелами необходим, чтобы был выбран элемент с полным именем класса
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " listing-item ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"]'
);

PageObjects.publicMain = new Entity('.public__main');

PageObjects.notification = new Entity('.notification');
PageObjects.notification.body = new Entity('.notification__body');
PageObjects.notification.body.inner = new Entity('span');

PageObjects.error = new Entity('.error');
PageObjects.error.errorIconBrokenLink = new Entity('.error__icon_broken-link');
PageObjects.error.errorTitle = new Entity('.error__title');
PageObjects.error.errorDescription = new Entity('.error__description');

PageObjects.directFrame = new Entity('.direct__iframe');
//боковой медиа-директ
PageObjects.directDesktopRight = new Entity('.direct-public_platform_desktop.direct-public_position_right');
//полоска директа в десктопе и таче
PageObjects.directDesktopTop = new Entity('.direct-public_position_top.direct-public_platform_desktop');
PageObjects.directMobileTop = new Entity('.direct-public_position_top.direct-public_platform_mobile');
PageObjects.mobileBottomDirect = new Entity('.mobile-bottom__direct');

PageObjects.appPromoBanner = new Entity('.app-promo-banner');
PageObjects.appPromoBanner.closeButton = new Entity('.app-promo-banner__close');

PageObjects.downloadIframe = new Entity('#download-iframe');

PageObjects.modalCell = new Entity('.Drawer-Overlay');

PageObjects.actionBar = new Entity('.resources-action-bar');
PageObjects.actionBar.saveButtonClearInverse = new Entity('.save-button.Button2_view_clear-inverse');
PageObjects.actionBar.saveButtonTransparent = new Entity('.save-button.Button2_view_transparent');
PageObjects.actionBar.downloadButtonClearInverse = new Entity('.download-button.Button2_view_clear-inverse');
PageObjects.actionBar.downloadButtonTransparent = new Entity('.download-button.Button2_view_transparent');

module.exports = bemPageObject.create(PageObjects);
