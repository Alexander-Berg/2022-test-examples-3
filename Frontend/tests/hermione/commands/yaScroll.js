// Скролит к нужному элементу (.scroll не работает на touch-phone-android)
function scrollToElement(browser, selector) {
    return browser.execute(function(selector) {
        document.querySelector(selector).scrollIntoView();
    }, selector);
}

// Скролит страницу на указанное кол-во пикселей
function scrollPixels(browser, number) {
    return browser
        .execute(function(number) {
            window.scrollTo(0, number);
        }, number);
}

module.exports = function(value) {
    return (typeof value === 'string') ?
        scrollToElement(this, value) :
        scrollPixels(this, value);
};
