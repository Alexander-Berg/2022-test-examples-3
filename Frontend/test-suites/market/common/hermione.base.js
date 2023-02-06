module.exports = function(_, platform) {
    const mock = require(`../${platform}/mocks/base.json`);
    const searchCategoryMock = require(`../${platform}/mocks/search-category.json`);
    const historyMock = require(`../${platform}/mocks/history.json`);
    const text = 'iphone';

    it('visible', function() {
        return this.browser
            .yaMockSuggest(text, mock)
            .click('.mini-suggest__input')
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content');
    });

    it('history', function() {
        return this.browser
            .yaMockSuggest('', historyMock)
            .click('.mini-suggest__input')
            .waitForVisible('.mini-suggest__popup-content')
            .assertView('history', '.mini-suggest__item_personal_yes');
    });

    it('category', function() {
        return this.browser
            .yaMockSuggest(text, mock)
            .click('.mini-suggest__input')
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content')
            .assertView('category', '.mini-suggest__item_subtype_ecom-category');
    });

    it('search category', function() {
        return this.browser
            .yaMockSuggest(text, searchCategoryMock)
            .click('.mini-suggest__input')
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content')
            .assertView('search category', '.mini-suggest__item_subtype_ecom-category');
    });

    it('model', function() {
        return this.browser
            .yaMockSuggest(text, mock)
            .click('.mini-suggest__input')
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content')
            .assertView('model', '.mini-suggest__item_subtype_ecom-item');
    });

    it('brand', function() {
        return this.browser
            .yaMockSuggest(text, mock)
            .click('.mini-suggest__input')
            .keys(text)
            .waitForVisible('.mini-suggest__popup-content')
            .assertView('brand', '.mini-suggest__item_subtype_ecom-brand');
    });
};
