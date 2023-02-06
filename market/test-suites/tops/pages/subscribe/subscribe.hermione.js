import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';

// suites
import SubscriptionVerifySuite from '@self/platform/spec/hermione/test-suites/blocks/SubscriptionVerify';
import FatalErrorSuite from '@self/platform/spec/hermione/test-suites/blocks/components/FatalError';
import FatalErrorLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/components/FatalError/link';
// page-objects
import SubscriptionVerify from '@self/platform/spec/page-objects/SubscriptionVerify';
import FatalError from '@self/platform/spec/page-objects/components/FatalError';

import verificationConfig from './config.mock.json';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница подтверждения подписки', {
    environment: 'kadavr',
    feature: 'Подтверждение подписки',
    story: mergeSuites(
        {
            'Сообщение об успешной подписке.': prepareSuite(SubscriptionVerifySuite, {
                hooks: {
                    async beforeEach() {
                        const productState = createProduct(verificationConfig.product, verificationConfig.product.id);

                        await this.browser.setState('marketUtils.data.verification', verificationConfig.success);
                        await this.browser.setState('report', productState);

                        await this.browser.yaOpenPage('market:subscribe', {
                            action: 'actionString',
                            sk: 'skString',
                        });
                    },
                },
                meta: {
                    id: 'm-touch-2693',
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
        },
        {
            'Сообщение об ошибке.': mergeSuites(
                prepareSuite(FatalErrorSuite, {
                    hooks: {
                        async beforeEach() {
                            return this.browser.allure.runStep('Открываем страницу без параметров', () =>
                                this.browser.yaOpenPage('market:subscribe')
                            );
                        },
                    },
                    meta: {
                        id: 'm-touch-2701',
                        issue: 'MOBMARKET-11609',
                    },
                    pageObjects: {
                        fatalError() {
                            return this.createPageObject(FatalError);
                        },
                    },
                }),
                prepareSuite(FatalErrorLinkSuite, {
                    hooks: {
                        async beforeEach() {
                            return this.browser.allure.runStep('Открываем страницу без параметров', () =>
                                this.browser.yaOpenPage('market:subscribe')
                            );
                        },
                    },
                    meta: {
                        id: 'm-touch-2702',
                        issue: 'MOBMARKET-11609',
                    },
                    pageObjects: {
                        fatalError() {
                            return this.createPageObject(FatalError);
                        },
                    },
                })
            ),
        }
    ),
});
