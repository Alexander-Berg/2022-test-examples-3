const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

CommonObjects.photoGroup = new Entity('.photo__group');
CommonObjects.photoItem = new Entity('.photo__item:not(.with-checkbox-selectable-item_stub)');

CommonObjects.photo = new Entity('.root__content-inner_page_photo');
CommonObjects.photo.tile = new Entity('.photo_tile');
CommonObjects.photo.wow = new Entity('.photo_wow');
CommonObjects.photoSelecting = new Entity('.photo_selecting');
CommonObjects.photo.group = new Entity('.photo__group');
CommonObjects.photo.title = new Entity('.grid-cluster-title__title');
CommonObjects.photo.titleLabel = new Entity('.grid-cluster-title__title-label');
CommonObjects.photo.item = new Entity('.photo__item:not(.with-checkbox-selectable-item_stub)');
CommonObjects.photo.videoItem = new Entity('.photo__item .photo-grid-preview_video');
CommonObjects.photo.selectedItem = new Entity('.with-checkbox-selectable-item_selected');
CommonObjects.photo.itemByName = new Entity('.photo__item[title=":title"]');
CommonObjects.photo.item.checkbox = new Entity('.lite-checkbox');
CommonObjects.photo.item.preview = new Entity('.scalable-preview__image');
CommonObjects.photo.filterStub = new Entity('.photo__empty-filter-text');

CommonObjects.photoHeader = new Entity('.photo-header');
CommonObjects.photoHeader.title = new Entity('.section-header__title');
CommonObjects.photoHeader.menuButton = new Entity('.photo-header-menu__button');
// меню фотосреза с фильтрацией по типу "Безлимит" / "Из папок"
CommonObjects.photoHeaderMenu = new Entity('.photo-header-menu__menu');
CommonObjects.photoHeaderMenu.filterAllRadio = new Entity('.photo-header-menu__radio_all');
CommonObjects.photoHeaderMenu.filterUnlimRadio = new Entity('.photo-header-menu__radio_unlim');
CommonObjects.photoHeaderMenu.filterFoldersRadio = new Entity('.photo-header-menu__radio_folders');
// меню переключения вида сетки на таче
TouchObjects.tileViewRadio = CommonObjects.photoHeaderMenu.descendant(new Entity('.photo-header-menu__radio_tile'));
TouchObjects.wowViewRadio = CommonObjects.photoHeaderMenu.descendant(new Entity('.photo-header-menu__radio_wow'));

CommonObjects.albumSliceDescription = new Entity('.album-slice-description');
CommonObjects.albumSliceDescription.closeButton = new Entity('.album-slice-description__close-button');

// панель "Выберите фото" для создания альбома / добавления фото в альбом
CommonObjects.addToAlbumBar = new Entity('.add-to-album-bar');
CommonObjects.addToAlbumBar.submitButton = new Entity('.add-to-album-bar__button');
CommonObjects.addToAlbumBar.closeButton = new Entity('.resources-action-bar__close');

CommonObjects.filterSelect = new Entity('.photo-header-menu__photoslice-filter');
CommonObjects.filterSelect.popup = new Entity('.Select2-Popup');
CommonObjects.filterSelect.popup.all = new Entity('.Menu-Item[value="all"]');
CommonObjects.filterSelect.popup.unlim = new Entity('.Menu-Item[value="photounlim"]');
CommonObjects.filterSelect.popup.folders = new Entity('.Menu-Item[value="nonphotounlim"]');

DesktopObjects.listingType = new Entity('.ListingTypeSelect');
DesktopObjects.listingType.popup = new Entity('.Select2-Popup');
DesktopObjects.listingType.tile = new Entity('.Menu-Item_type_option[value="tile"]');
DesktopObjects.listingType.wow = new Entity('.Menu-Item_type_option[value="wow"]');

DesktopObjects.fastScroll = new Entity('.desktop-fast-scroll');
DesktopObjects.fastScroll.goToBottomButton = new Entity('.desktop-fast-scroll__border-icon_bottom .Button2');
DesktopObjects.fastScroll.goToTopButton = new Entity('.desktop-fast-scroll__border-icon_top .Button2');
DesktopObjects.fastScroll.scrollPointer = new Entity('.desktop-fast-scroll__scroll-pointer');
DesktopObjects.fastScroll.pointer = new Entity('.desktop-fast-scroll__pointer');
DesktopObjects.fastScroll.pointerLabel = new Entity('.desktop-fast-scroll__pointer-label');
DesktopObjects.fastScroll.pointerContainer = new Entity('.desktop-fast-scroll__inner');
DesktopObjects.fastScroll.month = new Entity('.desktop-fast-scroll__month');

DesktopObjects.fastScrollGoToBottomTooltip = new Entity('.desktop-fast-scroll__go-to-tooltip_bottom');
DesktopObjects.fastScrollGoToTopTooltip = new Entity('.desktop-fast-scroll__go-to-tooltip_top');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    desktop: bemPageObject.create(DesktopObjects),
    touch: bemPageObject.create(TouchObjects)
};
