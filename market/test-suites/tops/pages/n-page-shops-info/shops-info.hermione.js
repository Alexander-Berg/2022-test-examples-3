import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createShopInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';

// suites
import ShopJurInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-jur-info';
// page-objects
import ShopJurInfo from '@self/platform/spec/page-objects/n-w-shop-jur-info';

import {shopOOO, shopOAO, shopZAO} from './mocks';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница инфо о магазине.', {
    feature: 'Инфо о магазине',
    story: mergeSuites(
        makeSuite('Юридическое.', {
            feature: 'Юридическое.',
            story: mergeSuites(
                makeSuite('Юридическое лицо ООО.', {
                    id: 'marketfront-855',
                    issue: 'MARKETVERSTKA-24986',
                    story: prepareSuite(ShopJurInfoSuite, {
                        pageObjects: {
                            shopJurInfo() {
                                return this.createPageObject(ShopJurInfo);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                this.params = {juridicalAddress: shopOOO.juridicalAddress};
                                const shopInfoMock = createShopInfo(shopOOO, shopOOO.id);

                                await this.browser.setState('ShopInfo.collections', shopInfoMock);

                                return this.browser.yaOpenPage('market:shop-info', {shopIds: shopOOO.id});
                            },
                        },
                    }),
                }),
                makeSuite('Юридическое лицо ОАО.', {
                    id: 'marketfront-856',
                    issue: 'MARKETVERSTKA-24987',
                    story: prepareSuite(ShopJurInfoSuite, {
                        pageObjects: {
                            shopJurInfo() {
                                return this.createPageObject(ShopJurInfo);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                this.params = {juridicalAddress: shopOAO.juridicalAddress};
                                const shopInfoMock = createShopInfo(shopOAO, shopOAO.id);

                                await this.browser.setState('ShopInfo.collections', shopInfoMock);

                                return this.browser.yaOpenPage('market:shop-info', {shopIds: shopOAO.id});
                            },
                        },
                    }),
                }),
                makeSuite('Юридическое лицо ЗАО.', {
                    id: 'marketfront-857',
                    issue: 'MARKETVERSTKA-24988',
                    story: prepareSuite(ShopJurInfoSuite, {
                        pageObjects: {
                            shopJurInfo() {
                                return this.createPageObject(ShopJurInfo);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                this.params = {juridicalAddress: shopZAO.juridicalAddress};
                                const shopInfoMock = createShopInfo(shopZAO, shopZAO.id);

                                await this.browser.setState('ShopInfo.collections', shopInfoMock);

                                return this.browser.yaOpenPage('market:shop-info', {shopIds: shopZAO.id});
                            },
                        },
                    }),
                })
            ),
        })
    ),
});
