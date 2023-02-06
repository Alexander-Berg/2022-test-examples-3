import {makeCase, makeSuite, mergeSuites} from 'ginny';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {getCrushedEndpointSettingsCookie} from '@self/root/src/utils/resource/utils';
import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';
import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';
import FullCard from '@self/platform/components/PageCardTitle/FullCard/__pageObject';
import {offer, offerId} from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-offer/fixtures/offerWithoutModel';
import OfferPage from '@self/platform/widgets/pages/OfferPage/__pageObject';
import ReviewsError from '@self/root/src/components/ReviewsError/__pageObject';
const offerDegradationSuite = makeSuite('Деградация.', {
    environment: 'kadavr',
    story: mergeSuites(

        makeSuite('Отказ сервиса pers-static.', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: getCrushedEndpointSettingsCookie(BACKENDS_NAME.PERS_STATIC),
                },
            },
            story: mergeSuites(
                makeSuite('Таб с описанием.', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.setPageObjects({
                                    productTabs: () => this.createPageObject(ProductTabs),
                                    productCardTitle: () => this.createPageObject(FullCard),
                                });

                                await this.browser.setState('report', offer);
                                await this.browser.yaOpenPage('market:offer', {offerId});
                            },
                        },
                        {
                            'Оглавление карточки товара должно отображаться.': makeCase({
                                async test() {
                                    await this.productCardTitle.root.isVisible()
                                        .should
                                        .eventually
                                        .to
                                        .be
                                        .equal(true, 'Оглавление карточки товара должно отображаться.');
                                },
                            }),
                            'Табы должны отображаться.': makeCase({
                                async test() {
                                    await this.productTabs.root.isVisible()
                                        .should
                                        .eventually
                                        .to
                                        .be
                                        .equal(true, 'Табы карточки товара должны быть видны.');
                                },
                            }),
                        }
                    ),
                }),
                makeSuite('Таб с отзывами', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.setPageObjects({
                                    degradationView: () => this.createPageObject(ReviewsError),
                                    offerPage: () => this.createPageObject(OfferPage),
                                    productTabs: () => this.createPageObject(ProductTabs),
                                    productCardTitle: () => this.createPageObject(FullCard),
                                });

                                await this.browser.setState('report', offer);
                                await this.browser.yaOpenPage('market:offer-reviews', {offerId});
                            },
                        },
                        {
                            'На табе с отзывами должна показываться плейсхолдер': makeCase({
                                async test() {
                                    await this.degradationView.rootElem.isVisible()
                                        .should
                                        .eventually
                                        .to
                                        .be
                                        .equal(true, 'Блок "Не удалось загрузить отзывы" должен отображаться.');
                                },
                            }),
                        }
                    ),
                })
            ),
        })
    ),
});

export default offerDegradationSuite;
