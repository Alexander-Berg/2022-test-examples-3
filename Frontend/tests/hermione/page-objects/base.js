const URL = require('url');

/**
 * Содержит элементы, которые есть на нескольких страницах магазина
 */

module.exports = {
    /**
     * Селектор кнопки "Назад", расположенной в шапке страниц
     *
     * @param {String} pageSelector
     * @returns {String}
     */
    backButton(pageSelector) {
        return `${pageSelector}  [class*="BackwardButton_button"]`;
    },

    /**
     * Селектор карусели товара
     */
    productCarousel: '[class*="ProductsCarousel_carousel"]',
    productCarouselFirstCard: '[class*="ProductsCarousel_carousel"] [class*="ProductsCarousel_wrapper"]:first-child',
    productCardTitle: '[class*="ProductsCarousel_carousel"] [class*="ProductsCarousel_wrapper"]:first-child .ProductCard-Title',
    productCardSkeleton: '.ProductCardSkeleton',

    /**
     * Осуществляет поиск селекторов карусели товара
     *
     * @param {Object} bro
     * @param {Number} carouselPosition
     * @returns {String}
     */
    async searchProductCarousel(bro, carouselPosition) {
        const productsCarousel = '[class*="ProductsCarousel_content"]';
        const selectors = await bro.findElements(productsCarousel);

        return {
            selector: selectors[carouselPosition],
        };
    },

    /**
     * Осуществляет поиск селекторов карточки товара в карусели товаров
     *
     * @param {Object} bro
     * @param {Number} carouselPosition
     * @param {Number} cardPosition
     * @returns {String}
     */
    async searchProductCarouselCard(bro, carouselPosition, cardPosition) {
        const carousel = await this.searchProductCarousel(bro, carouselPosition);
        const carouselProductsCard = `${carousel.selector} [class*="ProductsCarousel_wrapper"]`;
        const selectors = await bro.findElements(carouselProductsCard);
        const card = selectors[cardPosition];

        return {
            card,
            cardTitle: `${card} .ProductCard-Title`,
            cardUrl: `${card} a`,
            image: `${card} .ProductCard-Image`
        };
    },

    /**
     * Селекторы блока "Рекомендуем"
     */
    productRecommended: '[class*="ProductScreen_recommended"]',

    /**
     * Поиск селектора карточки товара в блоке "Рекомендуем"
     *
     * @param {Object} bro
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchProductRecommendedCard(bro, selectionPos) {
        const productRecommendedCard = `${this.productRecommended} .ProductCard`;
        const selectors = await bro.findElements(productRecommendedCard);
        const selector = selectors[selectionPos];

        return {
            selector,
            title: `${selector} .ProductCard-Title`,
            url: `${selector} a`,
        };
    },

    /**
     * Поиск скелетона карточки товара в переданном блоке
     *
     * @param {Object} bro
     * @param {String} selectorBlock
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchProductCardSkeleton(bro, selectorBlock, selectionPos) {
        const productCardSkeleton = `${selectorBlock} ${this.productCardSkeleton}`;
        const selectors = await bro.findElements(productCardSkeleton);

        return {
            selector: selectors[selectionPos],
        };
    },

    /**
     * Поиск селектора карточки товара
     *
     * @param {Object} bro
     * @param {String} selectorBlock
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchProductCard(bro, selectorBlock, selectionPos) {
        const productRecommendedCard = `${selectorBlock} .ProductCard`;
        const selectors = await bro.findElements(productRecommendedCard);

        return {
            selector: `${selectors[selectionPos]}`,
            title: `${selectors[selectionPos]} .ProductCard-Title`,
            url: `${selectors[selectionPos]} a`,
        };
    },

    /**
     * Нажимает на кнопку "Назад"
     * расположенной в шапке переданной страницы
     *
     * @param {Object} bro
     * @param {String} selectorPage
     * @returns {Promise}
     */
    async clickBackButton(bro, selectorPage) {
        const backButton = await this.backButton(selectorPage);
        await bro.leftClick(backButton);
    },

    /**
     * Осуществляет поиск скелетона карточки товара
     * Ожидает исчезновения скелетона найденной карточки товара со страницы
     *
     * @param {Object} bro
     * @param {String} selectorBlock
     * @param {Number} selectionPos
     * @returns {Promise}
     */
    async searchAndWaitProductCardSkeleton(bro, selectorBlock, selectionPos) {
        const productCardSkeletonVisible = await bro.isVisible(this.productCardSkeleton);

        if (productCardSkeletonVisible) {
            let productSkeleton = await this.searchProductCardSkeleton(bro, selectorBlock, selectionPos);
            await bro.waitForVisible(productSkeleton.selector, 5000, true);
        }
    },

    /**
     * @param {String} path
     * @param {Object} [query]
     * @returns {String}
     */
    makeUrl(path, query) {
        return URL.format({
            pathname: path,
            query
        });
    },
};
