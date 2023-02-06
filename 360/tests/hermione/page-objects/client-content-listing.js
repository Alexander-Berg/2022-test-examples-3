const bemPageObject = require('bem-page-object');

const Entity = bemPageObject.Entity;
const CommonObjects = {};
const TouchObjects = {};
const DesktopObjects = {};

//список файлов
CommonObjects.listing = new Entity('.root__content_page_listing');
CommonObjects.listingThemeRow = new Entity('.listing_theme_row');
CommonObjects.listingThemeTile = new Entity('.listing_theme_tile');
CommonObjects.listing.inner = new Entity('.root__content-inner_page_listing');
CommonObjects.listing.item = new Entity('.listing-item');
CommonObjects.listing.item_selected = new Entity('.listing-item_selected');
CommonObjects.listing.itemDpopHighlighed = new Entity('.listing__drop-target_active');
CommonObjects.listing.itemDpopHighlighed.title = new Entity('.listing-item__title span');
CommonObjects.listing.item.title = new Entity('.listing-item__title span');
CommonObjects.listing.item.preview = new Entity('.resource-image');
CommonObjects.listing.item.time = new Entity('.listing-item__column_time');
CommonObjects.listing.item.date = new Entity('.listing-item__column_date');
CommonObjects.listing.firstFile = new Entity('.listing-item:first-child');
CommonObjects.listing.head = new Entity('.listing-head');
CommonObjects.listing.head.header = new Entity('h1');
CommonObjects.listing.head.actionButton = new Entity('.listing-heading__button');
CommonObjects.listing.head.title = new Entity('.listing-heading__title[title=":title"]');
CommonObjects.listing.headSearch = new Entity('.listing-head_search');

CommonObjects.listingBody = new Entity('.listing');
CommonObjects.listingBody.items = new Entity('.listing-item:not(.listing-item_theme_tile-empty)');
CommonObjects.listingBody.itemsWithoutTrash = new Entity(
    '.listing-item:not(.listing-item_theme_tile-empty):not(.js-prevent-drag)'
);
CommonObjects.listingBody.items.title = new Entity('.listing-item__title .clamped-text');
CommonObjects.listingBody.items.icon = new Entity('.file-icon');
CommonObjects.listingBody.items.trashIcon = new Entity('.file-icon_dir_trash');
CommonObjects.listingBody.items.trashIconFull = new Entity('.file-icon_dir_trash-full');
CommonObjects.listing_body_items_trashIconClass = new Entity('file-icon_dir_trash');
CommonObjects.listingBody.items.publicIconButton = new Entity('.listing-item__field_public-link');
CommonObjects.listing.invitesToFolders = new Entity('.listing-invites-to-folders');
CommonObjects.listing.invitesToFolders.buttons = new Entity('.listing-invites-to-folders__buttons');
CommonObjects.listing.invitesToFolders.buttons.accept = new Entity('.Button2_view_default');
CommonObjects.listing.searchStub = new Entity('.listing-search-stub');

CommonObjects.invitesToFolderCertainAcceptButtonXpath =
    // eslint-disable-next-line max-len
    new Entity('//*[contains(concat(" ", normalize-space(@class), " "), " listing-invites-to-folders__item ")]//div[contains(@title, ":titleText")]/..//*[contains(concat(" ", normalize-space(@class), " "), " Button2_view_link ")]');
CommonObjects.listingSpinner = new Entity('.load-portions__spin');

//весь листинг, включая название папки, в которой находимся
CommonObjects.clientListing = new Entity('.client-listing');

//кнопка сортировки листинга
DesktopObjects.listingSortButton = new Entity('.listing-head__listing-settings .listing-sort');

//настройка типа листинга
DesktopObjects.listingType = new Entity('.ListingTypeSelect');
DesktopObjects.listingType.popup = new Entity('.Select2-Popup');
DesktopObjects.listingType.tile = new Entity('.Menu-Item[value="tile"]');
DesktopObjects.listingType.icons = new Entity('.Menu-Item[value="icons"]');
DesktopObjects.listingType.list = new Entity('.Menu-Item[value="list"]');
DesktopObjects.listingType.wow = new Entity('.Menu-Item[value="wow"]');

CommonObjects.listingBody.items.trashFullIcon = new Entity('.file-icon_dir_trash-full');

CommonObjects.listing.createSharedFolderButton = new Entity('.client-listing__create-shared-folder-button');

CommonObjects.listingBodyItemsInfoXpath = new Entity(
    // эквивалент '.listing-item__info span:contains(:titleText)', который wdio не поддерживает
    // concat с пробелами необходим, чтобы был выбран элемент с полным именем класса
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " listing-item__info ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"]'
);
CommonObjects.listingBodySelectedItemsInfoXpath = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(@class, "listing-item_selected")]//*[contains(concat(" ", normalize-space(@class), " "), " listing-item__info ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"]'
);
CommonObjects.listingBodyHighlightedItemsInfoXpath = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(@class, "listing-item_highlighted")]//*[contains(concat(" ", normalize-space(@class), " "), " listing-item__info ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"]'
);

// xpath для иконки файла (требуется для drag-and-drop)
CommonObjects.listingBodyItemsIconXpath = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " listing-item__info ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"] / ancestor::div[contains(concat(" ", normalize-space(@class), " "), " listing-item ")]//div[contains(concat(" ", normalize-space(@class), " "), " listing-item__icon-wrapper ")]'
);
// xpath для элемента файла с селктором .listing-item
CommonObjects.listingBodyItemsXpath = new Entity(
    // eslint-disable-next-line max-len
    '//*[contains(concat(" ", normalize-space(@class), " "), " listing-item__info ")]//span[translate(text(), "\n", "") = ":titleText" or @title = ":titleText"] / ancestor::div[contains(concat(" ", normalize-space(@class), " "), " listing-item ")]'
);

//кнопка очистки корзины в разделе Корзина
CommonObjects.listing.cleanTrash = new Entity('.client-listing__clean-trash-button');

//листинг в разделе Истоиия
CommonObjects.journalListing = new Entity('.root__content_page_journal');
CommonObjects.journalListing.header = new Entity('h1');
CommonObjects.journalListing.calendarDropdown = new Entity('.calendar-dropdown__textinput');
CommonObjects.journalListing.calendarDropdown.input = new Entity('input.Textinput-Control');
CommonObjects.journalListing.fileLink = new Entity('a.journal-group-item__name');
CommonObjects.journalListing.previewImageLink = new Entity('a.journal-group-item_preview-image');
CommonObjects.journalListing.previewVideoLink = new Entity('.journal-group-item_preview-video');
CommonObjects.journalListing.stub = new Entity('.journal-empty');
CommonObjects.journalListing.group = new Entity('.journal-group');
CommonObjects.journalListing.group.container = new Entity('.journal-group__container');
CommonObjects.journalListing.group.container.user = new Entity('.user');

DesktopObjects.journalFilter = CommonObjects.journalListing.descendant(new Entity('.journal-filter__filters'));
DesktopObjects.journalFilter.foldersFilter = new Entity('.journal-filter__item_folders');
DesktopObjects.journalFilter.calendarFilter = new Entity('.journal-filter__item_calendar');
DesktopObjects.journalFilter.eventsFilter = new Entity('.journal-filter__item_events');
DesktopObjects.journalFilter.platformFilter = new Entity('.journal-filter__item_platform');

CommonObjects.albumListing = new Entity('.listing-album');
CommonObjects.albumListing.item = new Entity('.item-album');

// иконка пуличной ссылки на файле в листинге
TouchObjects.listingItemPublicLinkIcon = CommonObjects.listingBody.items.descendant(
    new Entity('.listing-item__fields .listing-item__field_public-link .public-icon')
);
TouchObjects.listingItemRowPublicLinkIcon = CommonObjects.listingBody.items.descendant(
    new Entity('.listing-item__right .listing-item__column_public-link .public-icon')
);
DesktopObjects.listingItemPublicLinkIcon = CommonObjects.listingBody.items.descendant(
    new Entity('.share-link-button__hover-dropdown')
);

// активный элемент, отображающийся под курсором, при drag and drop'е
DesktopObjects.listingDraggingElement_active =
    new Entity('.listing__dragging-element.listing__dragging-element_active');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    touch: bemPageObject.create(TouchObjects),
    desktop: bemPageObject.create(DesktopObjects)
};
