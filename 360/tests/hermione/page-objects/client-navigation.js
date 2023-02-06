const bemPageObject = require('bem-page-object');
const navigation = require('../config').consts.NAVIGATION;
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

//Юзер-блок в шапке
CommonObjects.userBlock = new Entity('.user2__current-account');
CommonObjects.userMenu = new Entity('.user2__menu');
CommonObjects.userMenu.switchEditor = new Entity('*[class$="editor"]');
CommonObjects.goToPassport = new Entity('.user2__menu-item_action_passport');

//Лого Яндекса в шапке
CommonObjects.yaLogo = new Entity('.logo__link_yandex');

//Burger кнопка в шапке
DesktopObjects.openBurgerButton = new Entity('.burger-sidebar__button');
DesktopObjects.closeBurgerButton = new Entity('.burger-sidebar__back-button');
DesktopObjects.burgerOpened = new Entity('.burger-sidebar__sidebar_visible');
DesktopObjects.iconService = new Entity('.icons-services-menu__service-icon');

//Кнопка перехода на почту
DesktopObjects.mailLink = new Entity('.inline-services-menu__link[href*=mail]');

//Сайдбар с разделами
DesktopObjects.leftColumn = new Entity('.LeftColumn');
DesktopObjects.sidebarNavigation = new Entity('.LeftColumnNavigation');
//ссылки навигации по разделам (Последние, Файлы, Фото, ...) в сайдбаре
DesktopObjects.navigationItemDisk = new Entity('.LeftColumnNavigation__Item_type_files');
DesktopObjects.navigationItemPhoto = new Entity('.LeftColumnNavigation__Item_type_photo');
DesktopObjects.navigationItemShared = new Entity('.LeftColumnNavigation__Item_type_shared');
DesktopObjects.navigationItemRecent = new Entity('.LeftColumnNavigation__Item_type_recent');
DesktopObjects.navigationItemTrash = new Entity('.LeftColumnNavigation__Item_type_trash');
DesktopObjects.navigationItemJournal = new Entity('.LeftColumnNavigation__Item_type_journal');
DesktopObjects.navigationItemAlbums = new Entity('.LeftColumnNavigation__Item_type_albums');
DesktopObjects.navigationItemArchive = new Entity('.LeftColumnNavigation__Item_type_aux');
DesktopObjects.navigationItemTuning = new Entity('.InfoSpace');
DesktopObjects.navigationItemDownloads = new Entity('.LeftColumnNavigation__Item_type_downloads');
DesktopObjects.sidebarNavigation.dropHighlighted = new Entity('.drop-target_highlighted');
//TODO добавить остальные разделы

//секция с папками
DesktopObjects.sidebarNavigation.favoriteNavigationItems = new Entity('.navigation__items_favorite');
//секция с индикатором свободного места
DesktopObjects.spaceInfoSection = new Entity('.InfoSpace');
DesktopObjects.spaceInfoSection.infoSpaceButton = new Entity('.InfoSpace__Button');
DesktopObjects.spaceInfoSection.infoSpaceButton.infoSpaceButtonTextWrapper = new Entity('.Button2-Text');
DesktopObjects.spaceInfoSection.infoSpaceText = new Entity('.InfoSpace__Text');

//нужен для того, чтобы скрывать автоматически разворачивающийся инфопопап для вирусных и заблокированных файлов в тачах
TouchObjects.modalCell = new Entity('.Drawer_visible .Drawer-Overlay');

//хлебные крошки в листинге
DesktopObjects.listingCrumbs = new Entity('.listing-head__crumbs');
DesktopObjects.listingCrumbs.headCrumb = new Entity('.crumbs2__head');
DesktopObjects.listingCrumbs.crumbItem = new Entity('.crumbs2__item');
DesktopObjects.listingCrumbs.lastCrumb = new Entity('.crumbs2__item_last');

//блок кнопок с действиями "Загрузить" и "Создать" в сайдбаре
DesktopObjects.sidebarButtons = new Entity('.LeftColumn__Buttons');
DesktopObjects.sidebarButtons.create = new Entity('.create-resource-popup-with-anchor');
DesktopObjects.sidebarButtons.upload = new Entity('.upload-button');
DesktopObjects.sidebarButtons.upload.input = new Entity('input[type="file"]');

//промо установки ПО
DesktopObjects.softwareHeaderPromo = new Entity('.software-header-promo');
DesktopObjects.softwareHeaderProduct = new Entity('.software-header-product');

//кнопка Купить место
DesktopObjects.infoSpaceButton = new Entity('.InfoSpace__Button');
// индикатор свободного места
DesktopObjects.infoSpaceIndicator = new Entity('.InfoSpace__IndicatorWrapper');
DesktopObjects.infoSpaceIndicator.green = new Entity('.indicator-bar_load_normal');
DesktopObjects.infoSpaceIndicator.yellow = new Entity('.indicator-bar_load_medium');
DesktopObjects.infoSpaceIndicator.red = new Entity('.indicator-bar_load_full');

//ссылки навигации по разделам (Последние, Файлы, Фото, ...) над списком файлов
TouchObjects.mobileNavigation = new Entity('.mobile-navigation');
TouchObjects.navigationItemDisk = new Entity('span*=' + navigation.disk.navTitle);
TouchObjects.navigationItemPhoto = new Entity('span*=' + navigation.photo.navTitle);
TouchObjects.navigationItemShared = new Entity('span*=' + navigation.shared.navTitle);
TouchObjects.navigationItemRecent = new Entity('span*=' + navigation.recent.navTitle);
TouchObjects.navigationItemTrash = new Entity('span*=' + navigation.trash.navTitle);
TouchObjects.navigationItemJournal = new Entity('span*=' + navigation.journal.navTitle);
TouchObjects.navigationItemAlbums = new Entity('span*=' + navigation.albums.navTitle);
TouchObjects.navigationItemArchive = new Entity('span*=' + navigation.archive.navTitle);
TouchObjects.navigationItemTuning = new Entity('a*=' + navigation.tuning.navTitleTouch);
TouchObjects.navigationItemMail360 = new Entity('.mobile-navigation__item[href*=mail360]');
//TODO добавить остальные разделы

//блок кнопок управления списком
TouchObjects.touchListingSettings = new Entity('.touch-listing-settings');
TouchObjects.touchListingSettings.plus = new Entity('.touch-listing-settings__add-menu-button');
TouchObjects.touchListingSettings.settings = new Entity('.touch-listing-settings__listing-type-menu-button');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    desktop: bemPageObject.create(DesktopObjects),
    touch: bemPageObject.create(TouchObjects)
};
