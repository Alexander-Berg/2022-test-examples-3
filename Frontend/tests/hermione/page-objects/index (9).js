const pageObject = require('@yandex-int/bem-page-object');

module.exports = {
    loadPageObject(platform) {
        return pageObject.create(require(`./${platform}`));
    },
};
