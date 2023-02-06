const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};
const DesktopObjects = {};
const TouchObjects = {};

CommonObjects.searchForm = new Entity('.client-search-input');
CommonObjects.searchForm.input = new Entity('.Textinput-Control');
CommonObjects.searchForm.submitButton = new Entity('.Textinput-Icon_side_right');

CommonObjects.searchContent = new Entity('.client-search__content');
CommonObjects.searchResult = new Entity('.client-search__result');
CommonObjects.searchResultItems = new Entity('.client-search-result__items');
CommonObjects.searchResultItems.item = new Entity('.client-search-result__item');
CommonObjects.searchResultItems.itemFile = new Entity('.client-search-result__item_type_files');

DesktopObjects.buttonX = new Entity('.client-search__close');
DesktopObjects.searchBackground = new Entity('.client-search__background');

TouchObjects.buttonX = new Entity('.Textinput-Icon_side_left');

module.exports = {
    common: bemPageObject.create(CommonObjects),
    desktop: bemPageObject.create(DesktopObjects),
    touch: bemPageObject.create(TouchObjects)
};
