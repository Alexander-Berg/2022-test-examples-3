import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';

// suites
import SubscriptionVerifySuite from '@self/platform/spec/hermione/test-suites/blocks/SubscriptionVerify';
import SubscribeAgainSuite from '@self/platform/spec/hermione/test-suites/blocks/SubscriptionVerify/subscribeAgain';
import UnsubscribeReasonsSuite from '@self/platform/spec/hermione/test-suites/blocks/UnsubscribeReasons';
// page-objects
import SubscriptionVerify from '@self/platform/spec/page-objects/SubscriptionVerify';
import UnsubscribeReasons from '@self/platform/spec/page-objects/widgets/content/UnsubscribeReasons';

import verificationConfig from './config.mock.json';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница подтверждения отписки', {
    environment: 'kadavr',
    feature: 'Подтверждение отписки',
    story: mergeSuites(
        {
            'Сообщение об успешной отписке': prepareSuite(SubscriptionVerifySuite, {
                hooks: {
                    async beforeEach() {
                        const {product} = verificationConfig;
                        const productState = createProduct(product, product.id);

                        await this.browser.setState('marketUtils.data.verification',
                            verificationConfig.successWithProduct);

                        await this.browser.setState('marketUtils.data.unsubscribeReasons', null);
                        await this.browser.setState('report', productState);

                        await this.browser.yaOpenPage('market:unsubscribe', {
                            action: 'actionString',
                            sk: 'skString',
                        });
                    },
                },
                meta: {
                    id: 'm-touch-2691',
                    issue: 'MOBMARKET-11609',
                },
                pageObjects: {
                    subscriptionVerify() {
                        return this.createPageObject(SubscriptionVerify);
                    },
                },
                params: {
                    productId: verificationConfig.product.id,
                    slug: verificationConfig.product.slug,
                },
            }),
            'Кнопка обратной подписки': {
                'отписка от рассылки на модель': prepareSuite(SubscribeAgainSuite, {
                    meta: {
                        id: 'm-touch-2784',
                        issue: 'MOBMARKET-12171',
                    },
                    hooks: {
                        async beforeEach() {
                            const {product} = verificationConfig;
                            const productState = createProduct(product, product.id);

                            await this.browser.setState('marketUtils.data.verification',
                                verificationConfig.successWithProduct);

                            await this.browser.setState('marketUtils.data.unsubscribeReasons', null);
                            await this.browser.setState('report', productState);

                            await this.browser.yaOpenPage('market:unsubscribe', {
                                action: 'actionString',
                                sk: 'skString',
                            });
                        },
                    },
                    pageObjects: {
                        unsubscribeReasons() {
                            return this.createPageObject(UnsubscribeReasons);
                        },
                        subscriptionVerify() {
                            return this.createPageObject(SubscriptionVerify);
                        },
                    },
                }),
                'отписка от акций и скидок': prepareSuite(SubscribeAgainSuite, {
                    meta: {
                        id: 'm-touch-2783',
                        issue: 'MOBMARKET-12171',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('marketUtils.data.verification',
                                verificationConfig.success);

                            await this.browser.setState('marketUtils.data.unsubscribeReasons', null);

                            await this.browser.yaOpenPage('market:unsubscribe', {
                                action: 'actionString',
                                sk: 'skString',
                            });
                        },
                    },
                    pageObjects: {
                        unsubscribeReasons() {
                            return this.createPageObject(UnsubscribeReasons);
                        },
                        subscriptionVerify() {
                            return this.createPageObject(SubscriptionVerify);
                        },
                    },
                }),
            },
            'Причины отписки': {
                'отписка от рассылки на модель': prepareSuite(UnsubscribeReasonsSuite, {
                    meta: {
                        id: 'm-touch-2786',
                        issue: 'MOBMARKET-12171',
                    },
                    hooks: {
                        async beforeEach() {
                            const {product} = verificationConfig;
                            const productState = createProduct(product, product.id);

                            await this.browser.setState('marketUtils.data.verification',
                                verificationConfig.successWithProduct);

                            await this.browser.setState('marketUtils.data.unsubscribeReasons', null);
                            await this.browser.setState('report', productState);

                            await this.browser.yaOpenPage('market:unsubscribe', {
                                action: 'actionString',
                                sk: 'skString',
                            });
                        },
                    },
                    pageObjects: {
                        unsubscribeReasons() {
                            return this.createPageObject(UnsubscribeReasons);
                        },
                    },
                }),
                'отписка от акций и скидок': prepareSuite(UnsubscribeReasonsSuite, {
                    meta: {
                        id: 'm-touch-2785',
                        issue: 'MOBMARKET-12171',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('marketUtils.data.verification',
                                verificationConfig.success);

                            await this.browser.setState('marketUtils.data.unsubscribeReasons', null);

                            await this.browser.yaOpenPage('market:unsubscribe', {
                                action: 'actionString',
                                sk: 'skString',
                            });
                        },
                    },
                    pageObjects: {
                        unsubscribeReasons() {
                            return this.createPageObject(UnsubscribeReasons);
                        },
                    },
                }),
            },
        }
    ),
});
