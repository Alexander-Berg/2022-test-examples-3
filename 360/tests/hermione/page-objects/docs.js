const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

CommonObjects.docsPage = new Entity('.root_page_docs');
CommonObjects.docsPage.spin = new Entity('.Docs__Spin-Wrap');
CommonObjects.docsPage.innerWrapper = new Entity('.root__content-inner_page_docs');
CommonObjects.docsPage.title = new Entity('.section-header__title[title=":title:"]');
CommonObjects.docsPage.titleWrapper = new Entity('.section-header__title-wrapper');

CommonObjects.docsListing = new Entity('.Docs__Listing');
CommonObjects.docsListing.preview = new Entity('.resource-image');
CommonObjects.docsListing.item = new Entity('.listing-item');
CommonObjects.docsListing.iconsItem = new Entity('.listing-item_theme_tile.listing-item_size_m');
CommonObjects.docsListing.tileItem = new Entity('.listing-item_theme_tile.listing-item_size_l');
CommonObjects.docsListing.listItem = new Entity('.listing-item_theme_row');

CommonObjects.docsStub = new Entity('.Docs-Stub');
CommonObjects.docsStub.docx = new Entity('.Docs-Stub__Option-Icon_docx');
CommonObjects.docsStub.xlsx = new Entity('.Docs-Stub__Option-Icon_xlsx');
CommonObjects.docsStub.pptx = new Entity('.Docs-Stub__Option-Icon_pptx');
CommonObjects.docsStub.open = new Entity('.Open-From-Disk__Button');
CommonObjects.docsStub.upload = new Entity('.upload-button__attach');

CommonObjects.documentTitleDialog = new Entity('.Document-Title-Dialog');
CommonObjects.documentTitleDialog.nameInput = new Entity('.Textinput-Control');
CommonObjects.documentTitleDialog.submitButton = new Entity('.confirmation-dialog__button_submit');

CommonObjects.openFromDiskDialog = new Entity('.Open-From-Disk__Confirm-Dialog');
CommonObjects.openFromDiskDialog.acceptButton = new Entity('.confirmation-dialog__button_submit');

CommonObjects.emptyFilterStub = new Entity('.Docs__Empty-Filter');

CommonObjects.resourceActionsButton = new Entity('.ResourceActionsButton');
CommonObjects.rootContentContainer = new Entity('.root__content-container');

CommonObjects.listingItems = new Entity('.listing__items');
CommonObjects.scansListingItem = new Entity('//span[text() = \':titleText\']');

CommonObjects.scansGoToDiskFile = new Entity('.resources-actions-popup__action_type_go-to-file');
CommonObjects.scansDeleteResource = new Entity('.resources-actions-popup__action_type_delete');
CommonObjects.scansSliderMoreButton = new Entity('.groupable-buttons__more-button');
CommonObjects.promoTooltipHide = new Entity('.MessageBox-Close');
CommonObjects.scansStub = new Entity('.listing-scans-stub');

DesktopObjects.docsSidebar = new Entity('.LeftColumn');
DesktopObjects.docsSidebar.docxSection = new Entity('.LeftColumnNavigation__Item_type_docx');
DesktopObjects.docsSidebar.xlsxSection = new Entity('.LeftColumnNavigation__Item_type_xlsx');
DesktopObjects.docsSidebar.pptxSection = new Entity('.LeftColumnNavigation__Item_type_pptx');
DesktopObjects.docsSidebar.viewSection = new Entity('.LeftColumnNavigation__Item_type_view');
DesktopObjects.docsSidebar.scansSection = new Entity('.LeftColumnNavigation__Item_type_scans');
DesktopObjects.docsSidebar.createButton = new Entity('.Docs-Create-Dropdown__Button');

DesktopObjects.docsSidebarCreatePopup = new Entity('.Docs-Create-Dropdown__Popup');
DesktopObjects.docsSidebarCreatePopup.open = new Entity('.Docs-Create-Dropdown__Type_open');
DesktopObjects.docsSidebarCreatePopup.xlsx = new Entity('.Docs-Create-Dropdown__Type_xlsx');
DesktopObjects.docsSidebarCreatePopup.upload = new Entity('.upload-button__attach');

DesktopObjects.changeListingTypeButton = new Entity('.Docs-Change-Listing-Type__Button');
DesktopObjects.changeListingTypePopup = new Entity('.Docs-Change-Listing-Type__Popup');
DesktopObjects.changeListingTypeMenu = new Entity('.Docs-Change-Listing-Type__Menu');
DesktopObjects.changeListingTypeMenu.checkedItem = new Entity('.Menu-Item_checked');
DesktopObjects.changeListingTypeMenu.icons = new Entity('.Docs-Change-Listing-Type__Type_icons');
DesktopObjects.changeListingTypeMenu.tile = new Entity('.Docs-Change-Listing-Type__Type_tile');
DesktopObjects.changeListingTypeMenu.list = new Entity('.Docs-Change-Listing-Type__Type_list');

DesktopObjects.sortPopup = new Entity('.Change-Sort-Type__Popup');
DesktopObjects.sortPopup.titleSortType = new Entity('.Change-Sort-Type__Sort-Type_name');
DesktopObjects.sortPopup.sizeSortType = new Entity('.Change-Sort-Type__Sort-Type_size');
DesktopObjects.sortPopup.lastViewSortType = new Entity('.Change-Sort-Type__Sort-Type_date_last_view');
DesktopObjects.sortPopup.ascSortOrder = new Entity('.Change-Sort-Type__Sort-Order_1');
DesktopObjects.sortPopup.descSortOrder = new Entity('.Change-Sort-Type__Sort-Order_0');

DesktopObjects.filterButton = new Entity('.Docs-Header-Filter-Button');
DesktopObjects.filterPopup = new Entity('.Docs-Header-Filter-Button__Popup');
DesktopObjects.filterPopup.all = new Entity('.Docs-Header-Filter-Button__Type_all');
DesktopObjects.filterPopup.my = new Entity('.Docs-Header-Filter-Button__Type_my');
DesktopObjects.filterPopup.other = new Entity('.Docs-Header-Filter-Button__Type_other');

DesktopObjects.editorShareButton = new Entity('.Button2.share-button');

DesktopObjects.docsToolbar = new Entity('.Docs-Header__Controls-Wrapper');
DesktopObjects.docsToolbar.openFromDisk = new Entity('.Open-From-Disk__Button');
DesktopObjects.docsToolbar.upload = new Entity('.Docs-Upload-Button__Button');
DesktopObjects.docsToolbar.upload.input = new Entity('.upload-button__attach');

TouchObjects.navigation = new Entity('.mobile-navigation');
TouchObjects.docxSection = new Entity('//a[text() = \'Документы\']');
TouchObjects.xlsxSection = new Entity('//a[text() = \'Таблицы\']');
TouchObjects.pptxSection = new Entity('//a[text() = \'Презентации\']');
TouchObjects.scansSection = new Entity('//a[text() = \'Сканы\']');

TouchObjects.touchListingSettingsButton = new Entity('.Docs-Touch-Listing-Settings__Button');
TouchObjects.touchListingSettings = new Entity('.Docs-Touch-Listing-Settings');
TouchObjects.touchListingSettings.icons = new Entity('.Docs-Touch-Listing-Settings__Type_icons');
TouchObjects.touchListingSettings.tile = new Entity('.Docs-Touch-Listing-Settings__Type_tile');
TouchObjects.touchListingSettings.list = new Entity('.Docs-Touch-Listing-Settings__Type_list');
TouchObjects.touchListingSettings.titleSortType = new Entity('.Radiobox-Control[value="name"]');
TouchObjects.touchListingSettings.sizeSortType = new Entity('.Radiobox-Control[value="size"]');
TouchObjects.touchListingSettings.lastViewSortType = new Entity('.Radiobox-Control[value="date_last_view"]');
TouchObjects.touchListingSettings.ascSortOrder = new Entity('.Radiobox-Control[value="1"]');
TouchObjects.touchListingSettings.descSortOrder = new Entity('.Radiobox-Control[value="0"]');
TouchObjects.touchListingSettings.myFilter = new Entity('.Radiobox-Control[value="MY"]');
TouchObjects.touchListingSettings.otherFilter = new Entity('.Radiobox-Control[value="OTHER"]');

TouchObjects.editorShareButton = new Entity('.Button2.share-button-mobile');

TouchObjects.docsCreateButton = new Entity('.Docs-Create-Dropdown__Button');
TouchObjects.docsCreateDrawer = new Entity('.Docs-Create-Dropdown__Drawer');
TouchObjects.docsCreateDrawer.open = new Entity('.Docs-Create-Dropdown__Type_open');
TouchObjects.docsCreateDrawer.upload = new Entity('.Docs-Upload-Button__Button');
TouchObjects.docsCreateDrawer.upload.input = new Entity('.upload-button__attach');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    touch: bemPageObject.create(TouchObjects),
    desktop: bemPageObject.create(DesktopObjects)
};
