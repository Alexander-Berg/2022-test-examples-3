import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {createOffer, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createReportProductStateWithPicture} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {getVersusSlugByProductSlugs} from '@self/project/src/entities/versus/helpers';
// suites
import PopupComplainProductSuite from '@self/platform/spec/hermione/test-suites/blocks/PopupComplain/product';
import AdultWarningDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/default';
import AdultWarningAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/accept';
import AdultWarningDeclineSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/decline';
import HeadBannerProductAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/productAbsence';
import ProductSpecsDescriptionShortTextSuite
    from '@self/platform/spec/hermione/test-suites/blocks/n-w-product-specs-description/shortText';
import SeoVersusComparisonsSuite from '@self/platform/spec/hermione/test-suites/blocks/SeoVersusComparisons';
// page-objects
import SeoVersusComparisons from '@self/platform/spec/page-objects/widgets/content/SeoVersusComparisons';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';

import ProductSpecsDescription from '@self/platform/spec/page-objects/n-w-product-specs-description';
import offerMock from '@self/platform/spec/hermione/fixtures/offer/';
import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';
import {phone, phoneShortDescription} from '@self/platform/spec/hermione/fixtures/product/product';
import ComplainProductButton from '@self/platform/spec/page-objects/components/ComplainProductButton';
import PopupComplainForm from '@self/platform/spec/page-objects/components/ComplainPopup';

import seo from './seo';
import defaultOffer from './defaultOffer';

const DEFAULT_VERSUS_ID = 1111;
const DEFAULT_VERSUS_MAIN_PRODUCT_ID = 14206682;
const DEFAULT_HID = 91491;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница карточки модели, характеристики.', {
    story: mergeSuites(
        makeSuite('Диалог подтверждения возраста. Adult контент.', {
            environment: 'kadavr',
            feature: 'Диалог подтверждения возраста',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            adultConfirmationPopup() {
                                return this.createPageObject(AdultConfirmationPopup);
                            },
                        });

                        const productId = 1722193751;
                        const state = mergeState([
                            stateProductWithDO(1722193751, {
                                type: 'model',
                                titles: {raw: 'Смартфон Samsung Galaxy S8'},
                            }, {
                                ...offerMock,
                            }),
                            {
                                data: {
                                    search: {
                                        adult: true,
                                    },
                                },
                            },
                        ]);

                        await this.browser.setState('report', state);
                        await this.browser.yaOpenPage('market:product-spec', {
                            productId,
                            slug: 'some-slug',
                        });
                    },
                },
                prepareSuite(AdultWarningDefaultSuite, {
                    meta: {
                        issue: 'MARKETVERSTKA-24997',
                        id: 'marketfront-869',
                    },
                }),
                prepareSuite(AdultWarningAcceptSuite, {
                    meta: {
                        issue: 'MARKETVERSTKA-25001',
                        id: 'marketfront-873',
                    },
                }),
                prepareSuite(AdultWarningDeclineSuite, {
                    meta: {
                        issue: 'MARKETVERSTKA-25006',
                        id: 'marketfront-877',
                    },
                })
            ),
        }),

        prepareSuite(HeadBannerProductAbsenceSuite, {
            meta: {
                id: 'marketfront-3387',
                issue: 'MARKETVERSTKA-33961',
            },
            params: {
                pageId: 'market:product-spec',
            },
        }),
        prepareSuite(ProductSpecsDescriptionShortTextSuite, {
            params: {
                expectedTitle: 'Описание',
            },
            hooks: {
                async beforeEach() {
                    const offer = createOffer(phoneShortDescription, phoneShortDescription.id);
                    const product = createProduct(phoneShortDescription, phoneShortDescription.id);
                    const state = mergeState([offer, product]);
                    await this.browser.setState('report', state);
                    await this.browser.yaOpenPage('market:product-spec', {
                        productId: phoneShortDescription.id,
                        slug: phoneShortDescription.slug,
                    });
                },
            },
            pageObjects: {
                productSpecsDescrition() {
                    return this.createPageObject(ProductSpecsDescription);
                },
            },
        }),
        mergeSuites(
            {
                async beforeEach() {
                    const mainProductSample = {
                        ...phone,
                        id: DEFAULT_VERSUS_MAIN_PRODUCT_ID,
                    };
                    const otherProductSample = {
                        id: 14206684,
                        titles: {
                            raw: 'Смартфон IPhone 100500',
                        },
                        slug: 'iphone-100500-plus',
                    };
                    const versusSample = {
                        entity: 'versus',
                        id: DEFAULT_VERSUS_ID,
                        hid: DEFAULT_HID,
                        products: [
                            {entity: 'product', id: mainProductSample.id},
                            {entity: 'product', id: otherProductSample.id},
                        ],
                    };

                    const mainProduct = createReportProductStateWithPicture(mainProductSample);
                    const productToCompare = createProduct(otherProductSample, otherProductSample.id);

                    const state = mergeState([mainProduct, productToCompare]);
                    const schema = {versus: [versusSample]};

                    this.params = {
                        ...this.params,
                        expectedId: DEFAULT_VERSUS_ID,
                        expectedSlug: getVersusSlugByProductSlugs(mainProductSample.slug, otherProductSample.slug),
                    };

                    await Promise.all([
                        await this.browser.setState('schema', schema),
                        await this.browser.setState('report', state),
                    ]);

                    await this.browser.yaOpenPage('market:product-spec', {
                        productId: mainProductSample.id,
                        slug: mainProductSample.slug,
                    });
                },
            },
            prepareSuite(SeoVersusComparisonsSuite, {
                pageObjects: {
                    seoVersusComparisons() {
                        return this.createPageObject(SeoVersusComparisons);
                    },
                },
            }),
            makeSuite('Форма обратной связи в характеристиках', {
                environment: 'kadavr',
                story: prepareSuite(PopupComplainProductSuite, {
                    pageObjects: {
                        popup() {
                            return this.createPageObject(ComplainProductButton);
                        },
                        popupForm() {
                            return this.createPageObject(PopupComplainForm);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            const offer = createOffer(phoneShortDescription, phoneShortDescription.id);
                            const product = createProduct(phoneShortDescription, phoneShortDescription.id);
                            const state = mergeState([offer, product]);
                            await this.browser.setState('report', state);
                            await this.browser.yaOpenPage('market:product-spec', {
                                productId: phoneShortDescription.id,
                                slug: phoneShortDescription.slug,
                            });
                        },
                    },
                }),
            })
        ),
        seo,
        defaultOffer
    ),
});
