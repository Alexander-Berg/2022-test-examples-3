import _ from 'lodash';
import {mergeSuites, makeSuite} from 'ginny';

import USERS from 'spec/lib/constants/users/users';
import makeUserStory from 'spec/hermione/lib/helpers/userStory';
import buildUrl from 'spec/lib/helpers/buildUrl';
import {combinePermissions, excludePermissions, allPermissions} from 'spec/hermione/lib/helpers/permissions';
import productsState from 'spec/lib/page-mocks/products.json';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import PERMISSIONS from 'app/constants/permissions';

import PRODUCT_KEYS from 'app/constants/products/keys';

import PRODUCT_SERVICE_IDS from 'app/constants/products/serviceIds';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import {METRICS} from 'app/constants/modelsPromotionStatistics';
import {getAllowedPermissions} from 'shared/permissions';

import manyCutoffsState from './manyCutoffs.json';
import managerCutoffState from './managerCutoff.json';
import documentCutoffState from './documentCutoff.json';
import inactiveProductState from './inactiveProduct.json';
import brandZoneTariffsState from './brandZoneTariffs.json';
import marketAnalyticsCategoriesState from './marketAnalyticsCategories.json';
import recommendationsState from './recommendations.json';

const userStory = makeUserStory(ROUTE_NAMES.PRODUCTS);

const FINANCE_PERMISSIONS = [
    PERMISSIONS.modelbids.finance,
    PERMISSIONS.brandzone.finance,
    PERMISSIONS.recommended.finance,
    // Маркет.Аналитики нет, так как у неё другой service_id
    PERMISSIONS.marketingBanners.finance,
    PERMISSIONS.marketingLandings.finance,
    PERMISSIONS.marketingPromo.finance,
    PERMISSIONS.marketingEmail.finance,
    PERMISSIONS.marketingShopInShop.finance,
    PERMISSIONS.marketingProductPlacement.finance,
    PERMISSIONS.marketingLogo.finance,
    PERMISSIONS.marketingTv.finance,
    PERMISSIONS.marketingExternalPlatforms.finance,
];

const differentClientsProductState = {
    list: Object.values(productsState.list).map((product, index) => ({
        ...product,
        clientId: index,
    })),
};

export default makeSuite('Страница Услуги.', {
    story: (() => {
        const suites = USERS.map(user => {
            const vendor = _.get(user.permissions, [PERMISSIONS.recommended.finance, 0], 3301);
            const permissions = getAllowedPermissions(user.permissions, vendor);
            const params = {
                vendor,
                routeParams: {vendor},
                count: 18,
                detailsCount: 15,
            };

            let Transfer;

            if (_.intersection(permissions, FINANCE_PERMISSIONS).length > 1) {
                Transfer = {
                    suite: 'Transfer',
                };
            }

            return makeSuite(`${user.description}.`, {
                story: userStory({
                    user,
                    params,
                    pageObjects: {
                        logo: 'Logo',
                        modal: 'Modal',
                        footer: 'Footer',
                        products: 'Products',
                    },
                    async onSetKadavrState({id}) {
                        switch (id) {
                            // Стейт c неподключёнными услугами контрактного вендора
                            case 'vendor_auto-35':
                            case 'vendor_auto-819':
                            case 'vendor_auto-820':
                            case 'vendor_auto-821':
                            case 'vendor_auto-825':
                            case 'vendor_auto-831':
                            case 'vendor_auto-832':
                            case 'vendor_auto-833':
                            case 'vendor_auto-838':
                            case 'vendor_auto-842':
                            case 'vendor_auto-843':
                            case 'vendor_auto-844':
                            case 'vendor_auto-850':
                            case 'vendor_auto-855':
                            case 'vendor_auto-856':
                            case 'vendor_auto-857':
                            case 'vendor_auto-868':
                            case 'vendor_auto-1110':
                            case 'vendor_auto-1111':
                            case 'vendor_auto-1112':
                            case 'vendor_auto-1116':
                            case 'vendor_auto-1433':
                            case 'vendor_auto-1434':
                            case 'vendor_auto-1435':
                            case 'vendor_auto-1439':
                            case 'vendor_auto-1454':
                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [],
                                        offer: false,
                                    },
                                ]);

                            // Все услуги с предоплатой подключены офертному вендору
                            case 'vendor_auto-818':
                            case 'vendor_auto-822':
                            case 'vendor_auto-826':
                            case 'vendor_auto-827':
                            case 'vendor_auto-830':
                            case 'vendor_auto-834':
                            case 'vendor_auto-837':
                            case 'vendor_auto-839':
                            case 'vendor_auto-841':
                            case 'vendor_auto-845':
                            case 'vendor_auto-849':
                            case 'vendor_auto-851':
                            case 'vendor_auto-854':
                            case 'vendor_auto-858':
                            case 'vendor_auto-867':
                            case 'vendor_auto-1109':
                            case 'vendor_auto-1113':
                            case 'vendor_auto-1117':
                            case 'vendor_auto-1118':
                            case 'vendor_auto-1432':
                            case 'vendor_auto-1436':
                            case 'vendor_auto-1438':
                            case 'vendor_auto-1440':
                                await this.browser.setState('vendorProductsData', productsState);

                                return this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15],
                                        offer: true,
                                    },
                                ]);

                            case 'vendor_auto-31':
                                await this.browser.setState('vendorProductCutoffs', managerCutoffState);
                                return this.browser.setState('vendorProductsData', productsState);

                            case 'vendor_auto-517':
                                await this.browser.setState('vendorProductCutoffs', manyCutoffsState);
                                return this.browser.setState('vendorProductsData', productsState);

                            case 'vendor_auto-518':
                                await this.browser.setState('vendorProductCutoffs', documentCutoffState);
                                return this.browser.setState('vendorProductsData', productsState);

                            case 'vendor_auto-521':
                                await this.browser.setState('vendorProductsData', productsState);

                                return this.browser.setState('vendorCurrentUserInfo', {
                                    // признак, чтобы определить, какой катофф использовать
                                    // при запуске/отключении услуги
                                    isManager: this.params.isManager,
                                });

                            case 'vendor_auto-523':
                                await this.browser.setState('vendorProductCutoffs', {
                                    brandzone: [
                                        {
                                            id: 100500,
                                            type: this.params.isManager ? 'ADMIN' : 'CLIENT',
                                        },
                                    ],
                                });

                                return this.browser.setState('vendorProductsData', {
                                    list: {
                                        brandzone: {
                                            ...inactiveProductState.list.brandzone,
                                            activeCutoffTypes: [this.params.isManager ? 'ADMIN' : 'CLIENT'],
                                        },
                                    },
                                });

                            case 'vendor_auto-529':
                                await this.browser.setState('vendorProductCutoffs', {
                                    brandzone: [
                                        {
                                            id: 100500,
                                            type: 'POSTPONED',
                                            from: 1634216086000,
                                            to: 1674334800000,
                                        },
                                    ],
                                });

                                return this.browser.setState('vendorProductsData', inactiveProductState);

                            // для всех услуг указаны разные clientID
                            case 'vendor_auto-558':
                                return this.browser.setState('vendorProductsData', differentClientsProductState);

                            case 'vendor_auto-879':
                                await this.browser.setState('vendorProductCutoffs', {
                                    analytics: [
                                        {
                                            id: 100500,
                                            type: this.params.isManager ? 'ADMIN' : 'CLIENT',
                                        },
                                    ],
                                });

                                return this.browser.setState('vendorProductsData', {
                                    list: {
                                        analytics: {
                                            ...inactiveProductState.list.analytics,
                                            activeCutoffTypes: [this.params.isManager ? 'ADMIN' : 'CLIENT'],
                                        },
                                    },
                                });

                            // есть неактивная услуга
                            case 'vendor_auto-1216':
                                return this.browser.setState('vendorProductsData', inactiveProductState);

                            case 'vendor_auto-1379':
                                await this.browser.setState('vendorProductCutoffs', {
                                    modelbids: [
                                        {
                                            id: 100500,
                                            type: 'FINANCE',
                                        },
                                        {
                                            id: 100501,
                                            type: this.params.isManager ? 'ADMIN' : 'CLIENT',
                                        },
                                    ],
                                });

                                return this.browser.setState('vendorProductsData', {
                                    list: {
                                        modelbids: {
                                            ...inactiveProductState.list.modelbids,
                                            activeCutoffTypes: [this.params.isManager ? 'ADMIN' : 'CLIENT', 'FINANCE'],
                                        },
                                    },
                                });

                            // Сохранение деталей услуги без изменения
                            case 'vendor_auto-823':
                            case 'vendor_auto-835':
                            case 'vendor_auto-846':
                            case 'vendor_auto-863':
                            case 'vendor_auto-1114':
                            case 'vendor_auto-1430':
                                await this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15],
                                        offer: true,
                                    },
                                ]);

                                await this.browser.setState('marketAnalytics', marketAnalyticsCategoriesState);

                                return this.browser.setState('vendorProductsData', productsState);
                            // Смена тарифа для услуги Бренд-зона
                            case 'vendor_auto-852':
                                await this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15],
                                        offer: true,
                                    },
                                ]);

                                await this.browser.setState('vendorProductsTariffs', brandZoneTariffsState);

                                return this.browser.setState('vendorProductsData', productsState);
                            // Валидация cms ID страницы для услуги Бренд-зона
                            case 'vendor_auto-942':
                                await this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15],
                                        offer: true,
                                    },
                                ]);

                                return this.browser.setState('vendorProductsData', productsState);
                            // Отображение счётчиков с рекомендациями в блоке услуги рекомендованных магазинов
                            case 'vendor_auto-1456':
                                await this.browser.setState('virtualVendor', [
                                    {
                                        vendorId: vendor,
                                        products: [1],
                                        offer: true,
                                    },
                                ]);

                                await this.browser.setState('vendorRecommendedBusinesses', recommendationsState);

                                return this.browser.setState('vendorProductsData', productsState);
                            default:
                                return this.browser.setState('vendorProductsData', productsState);
                        }
                    },
                    suites: {
                        common: [
                            // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                            Transfer,
                            'ProductsGrid',
                            {
                                suite: 'Page/title',
                                params: {
                                    title: 'Услуги',
                                },
                            },
                        ],
                        byPermissions: {
                            [combinePermissions(
                                PERMISSIONS.entries.read,
                                excludePermissions(PERMISSIONS.entries.write),
                            )]: [
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Рекомендованные магазины. Кнопка редактирования.',
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Продвижение товаров. Кнопка редактирования.',
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Бренд-зона. Кнопка редактирования.',
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Аналитика. Кнопка редактирования.',
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Кнопка редактирования.',
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Кнопка редактирования.',
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Кнопка редактирования.',
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Кнопка редактирования.',
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Кнопка редактирования.',
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Кнопка редактирования.',
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Кнопка редактирования.',
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Кнопка редактирования.',
                                    params: {
                                        productName: 'Реклама на ТВ.',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/hiddenEditButton',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. Кнопка редактирования.',
                                    params: {
                                        productName: 'Реклама на внешних площадках.',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                            ],
                            [PERMISSIONS.entries.write]: [
                                /**
                                 * Открытие формы подключения услуги
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Рекомендованные магазины. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-817',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Продвижение товаров. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-829',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Отзывы за баллы. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1431',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Бренд-зона. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-840',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Аналитика. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-853',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/open',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. Кнопка редактирования.',
                                    meta: {
                                        id: 'vendor_auto-1108',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Сохранение формы подключения услуги
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Рекомендованные магазины. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-821',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Продвижение товаров. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-833',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Отзывы за баллы. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1435',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Бренд-зона. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-844',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Аналитика. Подключение услуги в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-857',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/save',
                                    suiteName: 'Маркетинговые услуги. Реклама на внешних площадках. Подключение.',
                                    meta: {
                                        id: 'vendor_auto-1112',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Отмена сохранения формы подключения услуги
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Рекомендованные магазины. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-820',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Продвижение товаров. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-832',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                        canBeActivated: true,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Отзывы за баллы. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1434',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                        canBeActivated: true,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Бренд-зона. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-843',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                        canBeActivated: true,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Аналитика. Подключение в Балансе. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-856',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                        canBeActivated: true,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cancel',
                                    suiteName:
                                        'Маркетинговые услуги. Реклама на внешних площадках. Подключение. Отмена.',
                                    meta: {
                                        id: 'vendor_auto-1111',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Валидация полей формы редактирования услуги
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Рекомендованные магазины. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-818',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Продвижение товаров. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-830',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Отзывы за баллы. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1432',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Бренд-зона. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-841',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Аналитика. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-854',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName:
                                        'Маркетинговые услуги. Участие в промоакциях. Редактирование. ' +
                                        'Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName:
                                        'Маркетинговые услуги. Страница магазина. Редактирование. ' +
                                        'Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName:
                                        'Маркетинговые услуги. Размещение логотипа. Редактирование. ' +
                                        'Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Редактирование. Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/validate',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. Редактирование. ' +
                                        'Валидация полей.',
                                    meta: {
                                        id: 'vendor_auto-1109',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Переход по ссылке на заказ в Балансе из формы редактирования услуги
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName: 'Рекомендованные магазины. Редактирование. Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-822',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName: 'Продвижение товаров. Редактирование. Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-834',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName: 'Отзывы за баллы. Редактирование. Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1436',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName: 'Бренд-зона. Редактирование. Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-845',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName: 'Аналитика. Редактирование. Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-858',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Баннеры. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Лендинг. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Участие в промоакциях. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Рассылки. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Страница магазина. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Брендирование. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. Размещение логотипа. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на ТВ. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/adminBalanceLink',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. Редактирование. ' +
                                        'Переход на заказ в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1113',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Редактирование услуги
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Рекомендованные магазины. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-824',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Продвижение товаров. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-836',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Отзывы за баллы. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1437',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Бренд-зона. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-848',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Аналитика. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-864',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Редактирование.',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Редактирование. ',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/edit',
                                    suiteName: 'Маркетинговые услуги. Реклама на внешних площадках. Редактирование. ',
                                    meta: {
                                        id: 'vendor_auto-1115',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Сохранение деталей услуги без изменений
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Рекомендованные магазины. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-823',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Продвижение товаров. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-835',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Бренд-зона. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-846',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Аналитика. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-863',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Отзывы за баллы. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1430',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Сохранение без изменений.',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Сохранение без изменений. ',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/editWithoutChanges',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. Сохранение без изменений. ',
                                    meta: {
                                        id: 'vendor_auto-1114',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Валидация ID клиента в Балансе
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName: 'Рекомендованные магазины. Подключение. Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-819',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName: 'Продвижение товаров. Подключение. Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-831',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName: 'Отзывы за баллы. Подключение. Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1433',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName: 'Бренд-зона. Подключение. Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-842',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName: 'Аналитика. Подключение услуги в Балансе. Валидация ID клиента.',
                                    meta: {
                                        id: 'vendor_auto-855',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Баннеры. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Лендинг. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Участие в промоакциях. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Рассылки. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Страница магазина. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Брендирование. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Размещение логотипа. Подключение. ' +
                                        'Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Реклама на ТВ. ' +
                                        'Подключение. Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/clientIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. ' +
                                        'Подключение. Валидация ID клиента в Балансе.',
                                    meta: {
                                        id: 'vendor_auto-1110',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Валидация ID контракта
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Рекомендованные магазины. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-825',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Продвижение товаров. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-838',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Отзывы за баллы. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1439',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Бренд-зона. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-850',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Аналитика. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-868',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Участие в промоакциях. Подключение. ' +
                                        'Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Страница магазина. Подключение. ' +
                                        'Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Брендирование. Подключение. ' +
                                        'Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Размещение логотипа. Подключение. ' +
                                        'Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. Реклама на ТВ. ' +
                                        'Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdValidate',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. ' +
                                        'Подключение. Валидация ID контракта.',
                                    meta: {
                                        id: 'vendor_auto-1116',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Изменение типа договора
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName: 'Рекомендованные магазины. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-826',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName: 'Продвижение товаров. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-837',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName: 'Отзывы за баллы. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1438',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName: 'Бренд-зона. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-849',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName: 'Аналитика. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-867',
                                    },
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Баннеры. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Лендинг. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Участие в промоакциях. Редактирование. ' +
                                        'Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Рассылки. Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Страница магазина. Редактирование. ' +
                                        'Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Брендирование. Редактирование. ' +
                                        'Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Размещение логотипа. Редактирование. ' +
                                        'Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. Реклама на ТВ. ' +
                                        'Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractTypeChange',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. ' +
                                        'Редактирование. Изменение типа договора.',
                                    meta: {
                                        id: 'vendor_auto-1117',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                /**
                                 * Перевод на оферту
                                 */
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Рекомендованные магазины. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-827',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Продвижение товаров. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-839',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Отзывы за баллы. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1440',
                                    },
                                    params: {
                                        productName: 'Отзывы за баллы',
                                        productKey: PRODUCT_KEYS.PAID_OPINIONS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Бренд-зона. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-851',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName:
                                        'Маркетинговые услуги. Участие в промоакциях. Редактирование. ' +
                                        'Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName:
                                        'Маркетинговые услуги. Страница магазина. Редактирование. ' +
                                        'Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName:
                                        'Маркетинговые услуги. Брендирование. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName:
                                        'Маркетинговые услуги. Размещение логотипа. Редактирование. ' +
                                        'Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName:
                                        'Маркетинговые услуги. Реклама на ТВ. Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/contractIdChange',
                                    suiteName:
                                        'Маркетинговые услуги. ' +
                                        'Реклама на внешних площадках. ' +
                                        'Редактирование. Перевод на оферту.',
                                    meta: {
                                        id: 'vendor_auto-1118',
                                    },
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/managerForm/cmsIdValidation',
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                            ],
                            [PERMISSIONS.entries.read]: [
                                {
                                    suite: 'ProductsGrid/managerMode',
                                    pageObjects: {
                                        tumbler: 'SwitchLevitan',
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Рекомендованные магазины. Детали услуги.',
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Продвижение товаров. Детали услуги.',
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Бренд-зона. Детали услуги.',
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Аналитика. Детали услуги.',
                                    params: {
                                        productName: 'Яндекс.Маркет Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.ANALYTICS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Баннеры. Детали услуги.',
                                    params: {
                                        productName: 'Баннеры',
                                        productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Лендинг. Детали услуги.',
                                    params: {
                                        productName: 'Лендинг',
                                        productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Участие в промоакциях. Детали услуги.',
                                    params: {
                                        productName: 'Участие в промоакциях',
                                        productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Рассылки. Детали услуги.',
                                    params: {
                                        productName: 'Рассылки',
                                        productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Страница магазина. Детали услуги.',
                                    params: {
                                        productName: 'Страница магазина',
                                        productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Брендирование. Детали услуги.',
                                    params: {
                                        productName: 'Брендирование',
                                        productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Размещение логотипа. Детали услуги.',
                                    params: {
                                        productName: 'Размещение логотипа',
                                        productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Реклама на ТВ. Детали услуги.',
                                    params: {
                                        productName: 'Реклама на ТВ',
                                        productKey: PRODUCT_KEYS.MARKETING.TV,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/details',
                                    suiteName: 'Маркетинговые услуги. Реклама на внешних площадках. Детали услуги.',
                                    params: {
                                        productName: 'Реклама на внешних площадках',
                                        productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                        balanceOrderUrl: buildUrl('external:balance-admin-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/notActivated',
                                    meta: {
                                        id: 'vendor_auto-35',
                                    },
                                    params: {
                                        productName: 'Аналитика',
                                        productKey: PRODUCT_KEYS.ANALYTICS,
                                    },
                                },
                            ],
                            [PERMISSIONS.modelbids.write]: {
                                suite: 'ProductsGrid/product/placement',
                                suiteName: 'Продвижение товаров. Управление размещением услуги.',
                                params: {
                                    productName: 'Продвижение товаров',
                                    productKey: PRODUCT_KEYS.MODEL_BIDS,
                                },
                            },
                            [PERMISSIONS.modelbids.finance]: {
                                suite: 'ProductsGrid/product/invoice',
                                params: {
                                    productName: 'Продвижение товаров',
                                    productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    settingsUrl: buildUrl(ROUTE_NAMES.SETTINGS, {
                                        vendor,
                                    }),
                                },
                            },
                            [PERMISSIONS.modelbids.read]: [
                                {
                                    suite: 'ProductsGrid/product',
                                    suiteName: 'Продвижение товаров.',
                                    meta: {
                                        issue: 'VNDFRONT-1828',
                                        id: 'vendor_auto-312',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        titleUrl: buildUrl(ROUTE_NAMES.MODELS_PROMOTION, {vendor}),
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                        spendingUrl: buildUrl(ROUTE_NAMES.MODELS_PROMOTION_STATISTICS, {
                                            vendor,
                                            metrics: METRICS.CHARGES,
                                        }),
                                        invoiceUrl: buildUrl('external:balance-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/currencyViewFilter',
                                    meta: {
                                        issue: 'VNDFRONT-4149',
                                        id: 'vendor_auto-1381',
                                    },
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/cutoffList',
                                    params: {
                                        productName: 'Продвижение товаров',
                                        productKey: PRODUCT_KEYS.MODEL_BIDS,
                                        cutoffList: [
                                            'Отключено производителем. Запустите услугу.',
                                            'Недостаточно средств — пополните счёт.',
                                        ],
                                    },
                                },
                            ],
                            [PERMISSIONS.brandzone.read]: [
                                {
                                    suite: 'ProductsGrid/product',
                                    suiteName: 'Бренд-зона.',
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                        invoiceUrl: buildUrl('external:balance-order', {
                                            serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                        }),
                                    },
                                },
                                {
                                    suite: 'Link',
                                    suiteName: 'Бренд-зона. Ссылка "Отправить заявку на подключение"',
                                    meta: {
                                        id: 'vendor_auto-1173',
                                        issue: 'VNDFRONT-3345',
                                        feature: 'Управление услугами и пользователями',
                                    },
                                    params: {
                                        caption: 'Отправить заявку на подключение',
                                        url: buildUrl('external:help:brand-create'),
                                        target: '_blank',
                                        external: true,
                                        exist: true,
                                    },
                                    pageObjects: {
                                        product() {
                                            return this.createPageObject(
                                                'Product',
                                                // @ts-expect-error(TS2551) найдено в рамках VNDFRONT-4580
                                                this.products,
                                                // @ts-expect-error(TS2551) найдено в рамках VNDFRONT-4580
                                                this.products.getItemByProductKey(PRODUCT_KEYS.BRAND_ZONE),
                                            );
                                        },
                                        link() {
                                            return this.createPageObject(
                                                'Link',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.product,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.product.createBrandLink,
                                            );
                                        },
                                    },
                                    hooks: {
                                        async beforeEach() {
                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.allure.runStep('Ожидаем появления списка услуг', () =>
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                this.products.waitForExist(),
                                            );

                                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                            await this.allure.runStep(
                                                'Ожидаем появления блока услуги "Бренд-зона"',
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                () => this.product.waitForExist(),
                                            );
                                        },
                                    },
                                },
                            ],
                            [PERMISSIONS.brandzone.finance]: {
                                suite: 'ProductsGrid/product/brandzone',
                                params: {
                                    productName: 'Бренд-зона',
                                    productKey: PRODUCT_KEYS.BRAND_ZONE,
                                },
                            },
                            [PERMISSIONS.analytics.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Аналитика.',
                                params: {
                                    productName: 'Яндекс.Маркет Аналитика',
                                    productKey: PRODUCT_KEYS.ANALYTICS,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.ANALYTICS,
                                    }),
                                },
                            },
                            [PERMISSIONS.recommended.read]: [
                                {
                                    suite: 'ProductsGrid/product/notActivated',
                                    suiteName: 'Рекомендованные магазины. Отображение неподключённой услуги.',
                                    meta: {
                                        id: 'vendor_auto-1454',
                                        issue: 'VNDFRONT-4348',
                                    },
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/recommended',
                                    params: {
                                        productName: 'Рекомендованные магазины',
                                        productKey: PRODUCT_KEYS.RECOMMENDED,
                                    },
                                },
                            ],
                            [allPermissions(PERMISSIONS.analytics.write, PERMISSIONS.entries.write)]: {
                                suite: 'ProductsGrid/product/postponedLaunch',
                                suiteName: 'Услуги. Аналитика. Изменение даты запуска менеджером.',
                                meta: {
                                    id: 'vendor_auto-879',
                                    issue: 'VNDFRONT-4149',
                                },
                                params: {
                                    productName: 'Яндекс.Маркет Аналитика',
                                    productKey: PRODUCT_KEYS.ANALYTICS,
                                },
                            },
                            [PERMISSIONS.marketingBanners.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Баннеры.',
                                params: {
                                    productName: 'Баннеры',
                                    productKey: PRODUCT_KEYS.MARKETING.BANNERS,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingLandings.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Лендинг.',
                                params: {
                                    productName: 'Лендинг',
                                    productKey: PRODUCT_KEYS.MARKETING.LANDINGS,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingPromo.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Участие в промоакциях.',
                                params: {
                                    productName: 'Участие в промоакциях',
                                    productKey: PRODUCT_KEYS.MARKETING.PROMO,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingEmail.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Рассылки.',
                                params: {
                                    productName: 'Рассылки',
                                    productKey: PRODUCT_KEYS.MARKETING.EMAIL,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingShopInShop.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Страница магазина.',
                                params: {
                                    productName: 'Страница магазина',
                                    productKey: PRODUCT_KEYS.MARKETING.SHOP_IN_SHOP,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingProductPlacement.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Брендирование.',
                                params: {
                                    productName: 'Брендирование',
                                    productKey: PRODUCT_KEYS.MARKETING.PRODUCT_PLACEMENT,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingLogo.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Размещение логотипа.',
                                params: {
                                    productName: 'Размещение логотипа',
                                    productKey: PRODUCT_KEYS.MARKETING.LOGO,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingTv.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Реклама на ТВ.',
                                params: {
                                    productName: 'Реклама на ТВ',
                                    productKey: PRODUCT_KEYS.MARKETING.TV,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [PERMISSIONS.marketingExternalPlatforms.read]: {
                                suite: 'ProductsGrid/product',
                                suiteName: 'Маркетинговые услуги. Реклама на внешних площадках.',
                                params: {
                                    productName: 'Реклама на внешних площадках',
                                    productKey: PRODUCT_KEYS.MARKETING.EXTERNAL_PLATFORMS,
                                    invoiceUrl: buildUrl('external:balance-order', {
                                        serviceId: PRODUCT_SERVICE_IDS.VENDORS,
                                    }),
                                },
                            },
                            [allPermissions(PERMISSIONS.entries.write, PERMISSIONS.brandzone.write)]: [
                                {
                                    suite: 'ProductsGrid/product/managerForm/tariffSelect',
                                    suiteName: 'Бренд-зона. Редактирование услуги. Смена тарифа.',
                                    meta: {
                                        id: 'vendor_auto-852',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                        currentTariffName: 'Продажи',
                                        nextTariffName: 'Продажи + Реклама',
                                    },
                                },
                                {
                                    suite: 'ProductsGrid/product/postponedLaunch',
                                    suiteName: 'Управление услугой Бренд-зона. Выставление даты запуска менеджером.',
                                    meta: {
                                        id: 'vendor_auto-523',
                                        issue: 'VNDFRONT-4149',
                                    },
                                    params: {
                                        productName: 'Бренд-зона',
                                        productKey: PRODUCT_KEYS.BRAND_ZONE,
                                    },
                                },
                            ],
                        },
                    },
                }),
            });
        });

        return mergeSuites(...suites);
    })(),
});
