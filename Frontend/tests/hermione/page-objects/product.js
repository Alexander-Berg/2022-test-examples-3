const assert = require('assert');

const basePage = require('./base');

/**
 * Страница товара
 * Содержит элементы, которые есть на странице товара
 */

module.exports = {
    ...basePage,

    /**
     * Селекторы страницы продукта
     */
    gallery: '[class*="ProductInfo_wrapper"] .Gallery',
    gallerySlider: '[class*="ProductInfo_wrapper"] .Gallery .Slider-Scroll',
    galleryItem: '[class*="ProductInfo_wrapper"] .Gallery .Slider-Scroll .SliderItem',
    galleryImageLoaded: '[class*="ProductInfo_wrapper"] .GalleryItem-ImageContainer .Image_loaded',
    galleryPoint: '[class*="ProductInfo_wrapper"] .GalleryPoints-Point',
    galleryPointActive(pointPosition) {
        return `[class*="ProductInfo_wrapper"] .GalleryPoints-Point:nth-of-type(${pointPosition}).GalleryPoints-Point_active`;
    },

    productScreen: '[class*="ProductScreen_page"]',
    productHeader: '[class*="ProductScreen_header"] + div',
    productHeaderFixed: '[class*="ProductScreen_scrolled"] [class*="ProductScreen_header"]',
    productProductNoImage: '[class*="ProductInfo_no-image"]',
    productInfoGallery: '[class*="ProductInfo_gallery"]:not(.Gallery)',
    productInfoTitle: '[class*="ProductInfo_title"]',
    description: '[class*="ProductDescription"][class*="TextExpand_container"]',
    descriptionCollapsed: '[class*="TextExpand_collapsed"]',
    descriptionExpand: '[class*="TextExpand_label"]',

    /**
     * Поиск селектора страницы
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchProductPage(bro, selectionPos) {
        const selectors = await bro.findElements(this.productScreen);

        const selector = selectors[selectionPos];

        return {
            screen: selector,
            screenHeader: `${selector} [class*="ProductScreen_header"]`
        };
    },

    /**
     * Селекторы блока с размерами товара
     */
    sizesCarousel: '[class*="ConfigurableAttribute_carousel"]',

    /**
     * Поиск селектора кнопки выбора размера
     *
     * @param {Object} bro
     * @param {Number} selectorPos
     * @returns {Promise}
     */
    async searchSizesLabel(bro, selectorPos) {
        const sizesLabels = `${this.sizesCarousel} [class*="ConfigurableAttribute_label"]`;
        const selectors = await bro.findElements(sizesLabels);
        const selector = selectors[selectorPos];

        return {
            selector,
            labelActive: `${selector}[class*="PropertyLabel_active"]`,
        };
    },

    /**
     * Осуществляет поиск селектора кнопки выбор размера
     * Нажимает на выбранную кнопку
     * Проверяет, что кнопка стала активной
     *
     * @param {Object} bro
     * @param {Number} selectorPos
     * @returns {Promise}
     */
    async selectAndCheckSizeLabelActive(bro, selectorPos) {
        let sizeLabel = await this.searchSizesLabel(bro, selectorPos);
        await bro.click(sizeLabel.selector);
        await bro.waitForVisible(sizeLabel.labelActive, 5000);
    },

    /**
     * Осуществляет поиск селектора кнопки выбор размера
     * Нажимает на выбранную кнопку
     * Проверяет, что кнопка стала неактивной
     *
     * @param {Object} bro
     * @param {Number} selectorPos
     * @returns {Promise}
     */
    async selectAndCheckSizeLabelDefault(bro, selectorPos) {
        let sizeLabel = await this.searchSizesLabel(bro, selectorPos);
        await bro.click(sizeLabel.selector);
        await bro.waitForVisible(sizeLabel.labelActive, 5000, true);
    },

    /**
     * Ожидает отображение страницы и заголовка страницы
     * Проверяет, что урл текущей страницы соответствует урлу страницы,
     * с которой был осуществлен переход
     *
     * @param {Object} bro
     * @param {String} urlProduct
     * @returns {Promise}
     */
    async checkBackProductPage(bro, urlProduct) {
        await bro.waitForVisible(this.productScreen, 5000);
        await bro.waitForVisible(this.productInfoTitle, 5000);

        const urlProductCurrent = await bro.getCurrentUrl();
        assert.strictEqual(urlProduct, urlProductCurrent, `Должен отображаться урл товара "${urlProduct}", а отображается "${urlProductCurrent}"`);
    },

    /**
     * @param {String} id
     * @param {Object} [query]
     * @returns {String}
     */
    getUrl(id, query) {
        return this.makeUrl(`/product/${id}`, query);
    }
};
