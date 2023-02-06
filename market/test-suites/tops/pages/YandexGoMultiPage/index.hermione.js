import {makeSuite, prepareSuite, mergeSuites} from '@yandex-market/ginny';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// configs
import {profiles} from '@self/platform/spec/hermione2/configs/profiles';
// suites
import YandexGoMultiPageSuite from '@self/platform/spec/hermione2/test-suites/blocks/YandexGoMultiPage';
// mocks
import yandexGoEntrypointCmsMarkup from '@self/root/src/spec/hermione/kadavr-mock/tarantino/yandexGoEntrypoint';
import yandexGoSearchCmsMarkup from '@self/root/src/spec/hermione/kadavr-mock/tarantino/yandexGoSearch';
import yandexGoSearchCmsConfig from '@self/root/src/spec/hermione/kadavr-mock/tarantino/yandexGoSearch/config';
import navigationTree from '@self/root/src/spec/hermione/kadavr-mock/cataloger/expressNavigationTree';
import {createProductWithCPADefaultOffer} from '@self/platform/spec/hermione2/fixtures/product';
// page-objects
import YandexGoCatalog from '@self/root/src/widgets/content/YandexGoCatalog/__pageObject';
import Snippet from '@self/root/src/components/Snippet/__pageObject';
import YandexGoCartButtonPopup from '@self/root/src/widgets/content/YandexGoCartButtonPopup/components/View/__pageObject';
// constants
import {PAGE_IDS_YANDEX_GO} from '@self/root/src/constants/pageIds';
import {YANDEX_GO_EATS_KIT_TEST_COOKIE} from '@self/root/src/constants/eatsKit';

const GPS = '37.541773,55.749461';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('E2E тест Yandex Go.', {
    defaultParams: {
        goAddress: {
            lat: 55.74777933879411,
            lon: 37.54372856423937,
            fullAddress: 'Россия, Москва, Москва Сити, Эволюция – Т1',
            uri: 'ytpp://МоскваСити/moscow_city_point_t1',
            title: 'ytpp://МоскваСити/moscow_city_point_t1',
            entrance: '4',
            doorcode: '',
            floor: '3',
            office: '3',
            comment: 'Аккуратнее, пожалуйста',
            source: 'MODAL_REQUEST',
        },
    },
    environment: 'kadavr',
    issue: 'MARKETFRONT-55392',
    story: mergeSuites(
        {
            async beforeEach() {
                const testUser = profiles['pan-topinambur'];

                const currentUser = createUser({
                    id: testUser.uid,
                    uid: {
                        value: testUser.uid,
                    },
                    login: testUser.login,
                    display_name: {
                        name: 'Willy Wonka',
                        public_name: 'Willy W.',
                        avatar: {
                            default: '61207/462703116-1544492602',
                            empty: false,
                        },
                    },
                    dbfields: {
                        'userinfo.firstname.uid': 'Willy',
                        'userinfo.lastname.uid': 'Wonka',
                    },
                    public_id: testUser.publicId,
                });

                await this.browser.setState('schema', {
                    users: [currentUser],
                });

                await this.browser.yaLogin(
                    testUser.login,
                    testUser.password
                );

                await this.browser.yaSetCookie({name: YANDEX_GO_EATS_KIT_TEST_COOKIE, value: '1', path: '/'});
            },
        },
        prepareSuite(YandexGoMultiPageSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [
                            yandexGoEntrypointCmsMarkup,
                            yandexGoSearchCmsMarkup,
                            yandexGoSearchCmsConfig,
                        ]
                    );

                    await this.browser.setState(
                        'Cataloger.tree', navigationTree
                    );

                    const category = {
                        entity: 'category',
                        id: 91491,
                        name: 'Мобильные телефоны',
                        fullName: 'Мобильные телефоны',
                        slug: 'mobilnye-telefony',
                        type: 'guru',
                        isLeaf: true,
                    };

                    const categories = [
                        category,
                    ];

                    const navnodes = {
                        navnodes: [{
                            entity: 'navnode',
                            category: category,
                            id: 82914,
                            slug: 'trubki',
                        }],
                    };

                    await this.browser.setState('report', createProductWithCPADefaultOffer(
                        {
                            product: {
                                categories,
                                navnodes,
                            },
                            offer: {
                                categories,
                                navnodes,
                                shop: {
                                    feed: {id: 12345},
                                },
                            },
                        }
                    ));

                    await this.browser.yaOpenPage(PAGE_IDS_YANDEX_GO.ENTRYPOINT, {
                        gps: GPS,
                        entrypoint: 1,
                        lr: 213,
                    });
                },
            },
            pageObjects: {
                yandexGoCatalog() {
                    return this.browser.createPageObject(YandexGoCatalog);
                },
                snippet() {
                    return this.browser.createPageObject(Snippet);
                },
                yandexGoCartButtonPopup() {
                    return this.browser.createPageObject(YandexGoCartButtonPopup);
                },
            },
        })
    ),
});
