import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import ProductCardLinks, {INFO_LINKS} from '@self/platform/widgets/parts/ProductCardLinks/__pageObject';
import ProductCardLinksCompact, {INFO_LINKS_COMPACT} from '@self/platform/widgets/parts/ProductCardLinksCompact/__pageObject';
import SkuCheckInUrlSuite from './checkPageUrl';


/**
 * Тесты на проверку сохранения sku при переходе.
 */
export default makeSuite('Табы.', {
    story: mergeSuites(
        makeSuite('Отзывы', {
            story: prepareSuite(SkuCheckInUrlSuite, {
                meta: {
                    id: 'm-touch-3445',
                    issue: 'MARKETFRONT-21578',
                },
                params: {
                    infoLink: INFO_LINKS.REVIEW,
                },
                pageObjects: {
                    productCardLinks() {
                        return this.createPageObject(ProductCardLinks);
                    },
                },
            }),
        }),
        makeSuite('Характеристики', {
            story: prepareSuite(SkuCheckInUrlSuite, {
                meta: {
                    id: 'm-touch-3446',
                    issue: 'MARKETFRONT-21578',
                },
                params: {
                    infoLink: INFO_LINKS.SPECS,
                },
                pageObjects: {
                    productCardLinks() {
                        return this.createPageObject(ProductCardLinks);
                    },
                },
            }),
        }),
        makeSuite('Вопросы', {
            story: prepareSuite(SkuCheckInUrlSuite, {
                meta: {
                    id: 'm-touch-3447',
                    issue: 'MARKETFRONT-21578',
                },
                params: {
                    infoLink: INFO_LINKS.QUESTIONS,
                },
                pageObjects: {
                    productCardLinks() {
                        return this.createPageObject(ProductCardLinks);
                    },
                },
            }),
        }),
        makeSuite('Отзывы. Компактный вид.', {
            story: prepareSuite(SkuCheckInUrlSuite, {
                meta: {
                    id: 'm-touch-3445',
                    issue: 'MARKETFRONT-21578',
                },
                params: {
                    infoLink: INFO_LINKS_COMPACT.REVIEW,
                },
                pageObjects: {
                    productCardLinks() {
                        return this.createPageObject(ProductCardLinksCompact);
                    },
                },
            }),
        }),
        makeSuite('Характеристики. Компактный вид.', {
            story: prepareSuite(SkuCheckInUrlSuite, {
                meta: {
                    id: 'm-touch-3446',
                    issue: 'MARKETFRONT-21578',
                },
                params: {
                    infoLink: INFO_LINKS_COMPACT.SPECS,
                },
                pageObjects: {
                    productCardLinks() {
                        return this.createPageObject(ProductCardLinksCompact);
                    },
                },
            }),
        }),
        makeSuite('Вопросы. Компактный вид.', {
            story: prepareSuite(SkuCheckInUrlSuite, {
                meta: {
                    id: 'm-touch-3447',
                    issue: 'MARKETFRONT-21578',
                },
                params: {
                    infoLink: INFO_LINKS_COMPACT.QUESTIONS,
                },
                pageObjects: {
                    productCardLinks() {
                        return this.createPageObject(ProductCardLinksCompact);
                    },
                },
            }),
        })
    ),
});
