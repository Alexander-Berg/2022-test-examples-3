const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;
const PageObjects = {};

// список альбомов в клиенте
PageObjects.albums = new Entity('.root__content_page_albums');
PageObjects.albums.title = new Entity('.albums2__header');
PageObjects.albums.newAlbumButton = new Entity('.albums2__item_new');
PageObjects.albums.stub = new Entity('.listing-stub_albums');

PageObjects.albumByName = new Entity('.albums2__item[title=":titleText"]');

// страница конкретного альбома в клиенте
PageObjects.album = new Entity('.root__content-inner_page_album');

PageObjects.publicAlbum = new Entity('.public__root');
PageObjects.publicAlbum.avatar = new Entity('.user-pic');
PageObjects.publicAlbum.title = new Entity('.folder-content__header-name');

PageObjects.albumSettingsButton = new Entity('.album-actions-dropdown__more-button');

PageObjects.albumSettingsPopup = new Entity('.album-actions-dropdown');
PageObjects.albumSettingsPopup.delete = new Entity('.album-actions-dropdown__action_type_delete-album');
PageObjects.albumSettingsPopup.addPhoto = new Entity('.album-actions-dropdown__action_type_add-items');
PageObjects.albumSettingsPopup.rename = new Entity('.album-actions-dropdown__action_type_rename-album');

PageObjects.albumPublishButton = new Entity('.preview-album__button_access');

// новая страница личного альбома
PageObjects.album2 = new Entity('.root__content-inner_page_album');
PageObjects.album2.container = new Entity('.album2');
PageObjects.album2.title = new Entity('.section-header__title');
PageObjects.album2.backButton = new Entity('.section-header__back-button');
PageObjects.album2.item = new Entity('.album2__item');
PageObjects.album2.item.preview = new Entity('.scalable-preview__image');
PageObjects.album2.itemByName = new Entity('.album2__item[title=":title"]');
PageObjects.album2.spin = new Entity('.load-potions__spin');
PageObjects.album2.stub = new Entity('.listing-stub_album');
PageObjects.album2.stub.title = new Entity('h1');
PageObjects.album2.stub = new Entity('.album2__stub');
PageObjects.album2.grid = new Entity('.virtual-grid');
PageObjects.album2.addToAlbumButton = new Entity('.personal-album-header__add-button');
PageObjects.album2.addToFavoritesAlbumButton = new Entity('.favorites-album-header__add-button');
PageObjects.album2.publishButton = new Entity('.personal-album-header__publish-button');
PageObjects.album2.actionsMoreButton = new Entity('.album-actions-dropdown__more-button');
PageObjects.album2.headerName = new Entity('.section-header__name');

PageObjects.album2ActionsDropdown = new Entity('.album-actions-dropdown');
PageObjects.album2ActionsDropdown.addItemsButton = new Entity('.album-actions-dropdown__action_type_add-items');
PageObjects.album2ActionsDropdown.publishButton = new Entity('.album-actions-dropdown__action_type_publish-album');
PageObjects.album2ActionsDropdown.unpublishButton = new Entity('.album-actions-dropdown__action_type_unpublish-album');
PageObjects.album2ActionsDropdown.setViewButton = new Entity('.album-actions-dropdown__action_type_set-view');
PageObjects.album2ActionsDropdown.renameButton = new Entity('.album-actions-dropdown__action_type_rename-album');
PageObjects.album2ActionsDropdown.deleteButton = new Entity('.album-actions-dropdown__action_type_delete-album');
PageObjects.album2ActionsDropdown.downloadButton = new Entity('.album-actions-dropdown__action_type_download-album');

PageObjects.album2ActionsDropdown.setView = new Entity('.album-actions-dropdown__radiobox');
PageObjects.album2ActionsDropdown.setView.tile = new Entity('[value="tile"]');
PageObjects.album2ActionsDropdown.setView.wow = new Entity('[value="wow"]');

// альбомы-срезы
PageObjects.albums2RootContent = new Entity('.root__content-inner_page_albums');
PageObjects.albums2 = new Entity('.albums2');
PageObjects.albums2.header = new Entity('.albums2__header');
PageObjects.albums2.item = new Entity('.albums2__item');
PageObjects.albums2.item.preview = new Entity('.scalable-preview img');
PageObjects.albums2.shimmer = new Entity('.albums2__item_shimmer');
PageObjects.albums2.personal = new Entity('.albums2_listing');
PageObjects.albums2.personal.album = new Entity('.albums2__item:not(.albums2__item_new)');
PageObjects.albums2.personal.createAlbumButton = new Entity('.albums2__item_new');
PageObjects.albums2.geo = new Entity('.albums2__item_geo');
PageObjects.albums2.faces = new Entity('.albums2__item_faces');
PageObjects.albums2.favorites = new Entity('.albums2__item_favorites');

PageObjects.onboarding = new Entity('.personal-albums-onboarding');
PageObjects.onboarding.image = new Entity('.personal-albums-onboarding__image');

PageObjects.onboardingFaces = new Entity('.albums2-onboarding-faces');
PageObjects.onboardingFaces.image = new Entity('.albums2-onboarding-faces__image');

module.exports = bemPageObject.create(PageObjects);
