const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;
const PageObjects = {};

PageObjects.app = new Entity('#app');

// кнопка "Войти" на морде Диска
PageObjects.authLoginLink = new Entity('.header__login-link');

PageObjects.login = new Entity('.header-auth__login');
PageObjects.login.loginField = new Entity('input[name="login"]');
PageObjects.login.passwordField = new Entity('input[name="password"]');
PageObjects.login.submit = new Entity('button[type="submit"]');

PageObjects.loginMobileOpen = new Entity('.b-face-mobile__label[for="flag-auth-on"]');
PageObjects.loginMobile = new Entity('.js-form-auth');
PageObjects.loginMobile.loginField = new Entity('input[name="login"]');
PageObjects.loginMobile.passwordField = new Entity('input[name="passwd"]');
PageObjects.loginMobile.submit = new Entity('button[type="submit"]');

PageObjects.welcomeScreen = new Entity('.welcome-screen-mobile');

PageObjects.header = new Entity('.header');
PageObjects.header.headerRightSide = new Entity('.header__side-right');
PageObjects.header.headerRightSide.yaPlus = new Entity('.ufo-yaplus');
PageObjects.user = new Entity('.header__user');

PageObjects.psHeader = new Entity('.PSHeader');
PageObjects.psHeader.logoYa = new Entity('.PSHeaderLogo360-Ya');
PageObjects.psHeader.logo360 = new Entity('.PSHeaderLogo360-360');
PageObjects.psHeader.mail = new Entity('.PSHeaderIcon_Mail');
PageObjects.psHeader.disk = new Entity('.PSHeaderIcon_Disk');
PageObjects.psHeader.telemost = new Entity('.PSHeaderIcon_Telemost');
PageObjects.psHeader.calendar = new Entity('.PSHeaderIcon_Calendar');
PageObjects.psHeader.documents = new Entity('.PSHeaderIcon_Documents');
PageObjects.psHeader.more = new Entity('.PSHeader-ServiceList-More');
PageObjects.psHeader.tabMore = new Entity('.PSHeaderIcon_More');
PageObjects.psHeader.calendarDay = new Entity('.PSHeaderIcon-CalendarDay');
PageObjects.psHeader.proBanner = new Entity('.PSHeader-Pro');
PageObjects.psHeader.loginButton = new Entity('.PSHeader-NoLoginButton');
PageObjects.psHeader.legoUser = new Entity('.legouser');
PageObjects.psHeader.legoUser.inner = new Entity('.legouser');
PageObjects.psHeader.legoUser.userPic = new Entity('.user-pic');
PageObjects.psHeader.legoUser.ticker = new Entity('.user-account__ticker');
PageObjects.psHeader.legoUser.popup = new Entity('.legouser__popup');
PageObjects.psHeader.legoUser.popup.inner = new Entity('.legouser__menu');
PageObjects.psHeader.legoUser.popup.changeUser =
    new Entity('.legouser__accounts .legouser__account:not(.user-account_template_yes)');
PageObjects.psHeader.legoUser.popup.goToPassport = new Entity('.legouser__menu-item_action_passport');
PageObjects.psHeader.legoUser.popup.unreadCounter = new Entity('.legouser__menu-counter');
PageObjects.psHeader.legoUser.popup.switchEditor = new Entity(' *[class$="editor"]');

PageObjects.psHeader.suggest = new Entity('.client-suggest');
PageObjects.psHeader.suggest.input = new Entity('.Textinput-Control');
PageObjects.psHeader.suggest.submitButton = new Entity('.search-input__form-button');
PageObjects.psHeader.suggest.close = new Entity('.search-input__narrow-close-button');
PageObjects.psHeader.suggest.items = new Entity('.search-result__items');
PageObjects.psHeader.suggest.items.item = new Entity('.search-result__item');
PageObjects.psHeader.suggest.items.fileItem = new Entity('.search-result__item_type_files');
PageObjects.psHeader.suggest.items.folderItem = new Entity('.search-result__item_type_folders');
PageObjects.psHeaderSuggestPopup = new Entity('.search__popup');

PageObjects.psHeaderMorePopup = new Entity('.PSHeader-MorePopup');
PageObjects.psHeaderMorePopup.pro = new Entity('.PSHeaderIcon_Pro');
PageObjects.psHeaderMorePopup.calendar = new Entity('.PSHeaderIcon_Calendar');
PageObjects.psHeaderMorePopup.notes = new Entity('.PSHeaderIcon_Notes');
PageObjects.psHeaderMorePopup.contacts = new Entity('.PSHeaderIcon_Contact');
PageObjects.psHeaderMorePopup.messenger = new Entity('.PSHeaderIcon_Messenger');
PageObjects.psHeaderMorePopup.adminka = new Entity('.PSHeaderIcon_Admin');
PageObjects.psHeaderMorePopup.allServices = new Entity('.qa-PSHeader-MorePopup-all-services');
PageObjects.psHeaderMorePopup.calendarDay = new Entity('.PSHeaderIcon-CalendarDay');

PageObjects.headerLinkNotes = new Entity('.header-link-notes');
PageObjects.headerLinkNotes.unviewed = new Entity('.inline-services-menu__unviewed');

PageObjects.oldBrowsersStub = new Entity('.nb-update-browser');

PageObjects.promoBanner = new Entity('.promo-banner');
PageObjects.promoBanner.closeButton = new Entity('.promo-banner__close');
PageObjects.mobilePromoBanner = new Entity('.mobile-promo-banner');
PageObjects.appPromoBanner = new Entity('.app-promo-banner');
PageObjects.appPromoBanner.closeButton = new Entity('.app-promo-banner__close');
PageObjects.appPromoBanner.installButton = new Entity('.app-promo-banner__install-button');
PageObjects.b2cBanner = new Entity('.b2c-1-banner');
PageObjects.docsPromoBanner = new Entity('.docs-banner');

PageObjects.overdraftBlock = new Entity('.overdraft-block');
PageObjects.overdraftBlock.closeButton = new Entity('.overdraft-block__close-button');
PageObjects.overdraftBlock.tuningButton = new Entity('.overdraft-block__button');
PageObjects.overdraftBlock.helpLink = new Entity('.overdraft-block__help-link');
PageObjects.overdraftBlock.messageWrapper = new Entity('.overdraft-block__message-wrapper');
PageObjects.overdraftBlock.addSpaceButton = new Entity('.overdraft-block__button-add-space');

PageObjects.overdraftContent = new Entity('.overdraft-content');
PageObjects.overdraftContent.closeButton = new Entity('.overdraft-content__close');
PageObjects.overdraftContent.addSpaceButton = new Entity('.overdraft-content__button-add-space');
PageObjects.overdraftContent.cleanButton = new Entity('.overdraft-content__button-clean');
PageObjects.overdraftContent.aboutLink = new Entity('.overdraft-content__about');
PageObjects.overdraftContent.title = new Entity('.overdraft-content__title');
PageObjects.overdraftContent.text = new Entity('.overdraft-content__text');

PageObjects.stub = new Entity('.listing-stub');
PageObjects.stub.background = new Entity('.listing-stub__background');
PageObjects.stub.descriptionHeader = new Entity('.listing-stub__desc h1');
PageObjects.stub.uploadButton = new Entity('.upload-button__attach');
PageObjects.stub.actionButton = new Entity('.listing-stub__action-button');
PageObjects.stubDownloads = new Entity('.listing-stub_downloads');

PageObjects.root = new Entity('.root');
PageObjects.root.content = new Entity('.root__content');
PageObjects.root.content.bottomAd = new Entity('.root__bottom-ad');
PageObjects.root.content.bottomAd.frame = new Entity('.direct__iframe');
PageObjects.scansInSidebar = new Entity('.LeftColumnNavigation__Item_type_scans');

PageObjects.photoItem = new Entity('.with-checkbox-selectable-item');
PageObjects.selectedPhotoItem = new Entity('.with-checkbox-selectable-item_selected');
PageObjects.photoItemByName = new Entity('.with-checkbox-selectable-item[title=":title"]');

module.exports = bemPageObject.create(PageObjects);
