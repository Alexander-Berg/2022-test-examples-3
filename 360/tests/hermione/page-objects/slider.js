const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

CommonObjects.contentSlider = new Entity('.slider');
CommonObjects.contentSliderWait = new Entity('.slider_wait');
CommonObjects.contentSlider.item = new Entity('.slider__item');
CommonObjects.contentSlider.items = new Entity('.slider__items');
CommonObjects.contentSlider.activeItem = new Entity('.slider__item_active');
CommonObjects.contentSlider.activeItem.resourceName = new Entity('.resource-name');
CommonObjects.contentSlider.activeItem.openButton = new Entity('.resource-preview__open-button');
CommonObjects.contentSlider.activeItem.spin = new Entity('.Spin2');
CommonObjects.contentSlider.previewImage = new Entity('.scalable-preview__image');
CommonObjects.contentSlider.activePreview = new Entity('.scalable-preview_active');
CommonObjects.contentSlider.activePreview.image = new Entity('.scalable-preview__image');
CommonObjects.contentSlider.nextImage = new Entity('.switch-arrow-button_right');
CommonObjects.contentSlider.previousImage = new Entity('.switch-arrow-button_left');
CommonObjects.contentSlider.deleteInfoPopup = new Entity('.notifications__item_moved');
CommonObjects.contentSlider.videoPlayer = new Entity('.video-player');
CommonObjects.contentSlider.videoPlayer.overlayButton = new Entity('.video-player__touch-overlay-button');
CommonObjects.contentSlider.audioPlayer = new Entity('.audio-player');
CommonObjects.contentSlider.audioPlayerPlay = new Entity('.audio-player_play');
CommonObjects.contentSlider.audioPlayer.playPauseButton = new Entity('.audio-player__play-pause-button');
CommonObjects.contentSlider.fakeItemSpin = new Entity('.slider__fake-item-spin');

CommonObjects.activeItemTitle =
    new Entity('//*[contains(@class, "slider__item slider__item_active")]//*[text()=":title"]');

CommonObjects.sliderButtons = new Entity('.slider__toolbar');
CommonObjects.sliderButtons.infoButton = new Entity('.resources-info-dropdown .Button2');
CommonObjects.sliderButtons.closeButton = new Entity('.slider__button_close');
CommonObjects.sliderButtons.shareButton = new Entity('.Button2[type="check"]');
CommonObjects.sliderButtons.deleteButton = new Entity('.groupable-buttons__visible-button_name_delete');
CommonObjects.sliderButtons.downloadButton = new Entity('.groupable-buttons__visible-button_name_download');
CommonObjects.sliderButtons.editButton = new Entity('.groupable-buttons__visible-button_name_edit');
CommonObjects.sliderButtons.excludeFromAlbumButton =
    new Entity('.groupable-buttons__visible-button_name_exclude-from-album');
CommonObjects.sliderButtons.excludeFromPersonalAlbumButton =
    new Entity('.groupable-buttons__visible-button_name_exclude-from-personal-album');
CommonObjects.sliderButtons.moreButton = new Entity('.groupable-buttons__more-button');
CommonObjects.sliderButtons.favoriteButton = new Entity('.slider__favorite-button');
CommonObjects.sliderButtons.favoriteButtonOn = new Entity('.slider__favorite-button.favorite-button_state_on');
CommonObjects.sliderButtons.favoriteButtonOff = new Entity('.slider__favorite-button.favorite-button_state_off');

CommonObjects.sliderMoreButtonPopup = new Entity('.groupable-buttons__menu-buttons');
CommonObjects.sliderMoreButtonPopup.deleteButton = new Entity('.groupable-buttons__menu-button_action_delete');
CommonObjects.sliderMoreButtonPopup.copyButton = new Entity('.groupable-buttons__menu-button_action_copy');
CommonObjects.sliderMoreButtonPopup.moveButton = new Entity('.groupable-buttons__menu-button_action_move');
CommonObjects.sliderMoreButtonPopup.renameButton = new Entity('.groupable-buttons__menu-button_action_rename');
CommonObjects.sliderMoreButtonPopup.showFullsizeButton =
    new Entity('.groupable-buttons__menu-button_action_show-fullsize');
CommonObjects.sliderMoreButtonPopup.createAlbumButton =
    new Entity('.groupable-buttons__menu-button_action_create-album');
CommonObjects.sliderMoreButtonPopup.addToAlbumButton =
    new Entity('.groupable-buttons__menu-button_action_add-to-album');
CommonObjects.sliderMoreButtonPopup.versionsButton = new Entity('.groupable-buttons__menu-button_action_versions');
CommonObjects.sliderMoreButtonPopup.goToFileButton = new Entity('.groupable-buttons__menu-button_action_go-to-file');
CommonObjects.sliderMoreButtonPopup.setAsCoverButton =
    new Entity('.groupable-buttons__menu-button_action_set-as-cover');

CommonObjects.fileIconPdf = new Entity('.file-icon_pdf');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    desktop: bemPageObject.create(DesktopObjects),
    touch: bemPageObject.create(TouchObjects)
};
