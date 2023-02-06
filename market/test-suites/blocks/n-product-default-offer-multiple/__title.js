import {makeSuite, makeCase} from 'ginny';
import {createProduct, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

/**
 * Тесты на элемент n-product-default-offer-multiple__title
 * @param {PageObject.ProductDefaultOfferMultiple} defaultOffer
 */
export default makeSuite('В заголовке дефолтного офера.', {
    feature: 'Регион',
    environment: 'kadavr',
    story: {
        beforeEach() {
            const phone = {
                'id': '1831859610',
                'titles': {
                    'raw': 'Смартфон Xiaomi Redmi 5 Plus 3/32GB',
                    'highlighted': [
                        {
                            'value': 'Смартфон Xiaomi Redmi 5 Plus 3/32GB',
                        },
                    ],
                },
                'slug': 'smartfon-xiaomi-redmi-5-plus-3-32gb',
                'categories': [
                    {
                        'entity': 'category',
                        'id': 91491,
                        'name': 'Мобильные телефоны',
                        'fullName': 'Мобильные телефоны',
                        'type': 'guru',
                        'slug': 'mobilnye-telefony',
                        'isLeaf': true,
                    },
                ],
                'navnodes': [
                    {
                        'entity': 'navnode',
                        'id': 54726,
                        'name': 'Мобильные телефоны',
                        'slug': 'mobilnye-telefony',
                        'fullName': 'Мобильные телефоны',
                        'isLeaf': true,
                        'rootNavnode': {},
                    },
                ],
            };
            const product = createProduct(phone, phone.id);
            const offer = createOffer({
                orderMinCost: {
                    value: 5500,
                    currency: 'RUR',
                },
                benefit: {
                    type: this.params.benefit,
                    isPrimary: true,
                },
                shop: {
                    id: 1,
                    name: 'shop',
                    slug: 'shop',
                    logo: 'shop-logo',
                },
                urls: {
                    encrypted: '/redir/test',
                    decrypted: '/redir/test',
                    geo: '/redir/test',
                    offercard: '/redir/test',
                },
                vendor: {
                    id: 1,
                    webpageRecommendedShops: '/some-url/',
                    name: 'vendor',
                    logo: {
                        url: 'logo-url',
                    },
                },
                cpc: 'YVwBN9ETvPXGDZSiKF2l7yI7ewwo3VPSxwc_i4zikHLQonAxhPCtDEi6sGg0m78tNJMVpses-WvglfZYpW1ZqaxAkk' +
        '6Jkk9okufOs3YBoznAc40Hlkj9wdfSS5fsdZswCS8u1xJXkmg,',
            }, 42);
            const reportState = mergeReportState([
                product,
                offer,
                {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                },
            ]);

            return this.browser.setState('report', reportState)
                .then(() => this.browser.yaOpenPage('market:product', {productId: phone.id, slug: phone.slug}));
        },

        'При выборе региона': {
            'Присутствует соответствующая надпись': makeCase({
                test() {
                    return this.defaultOfferTitle.getText()
                        .should.eventually.equal(this.params.title, 'Заголовок содержит нужный текст');
                },
            }),
        },
    },
});
