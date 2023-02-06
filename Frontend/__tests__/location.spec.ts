import { setLocation } from '../history';
import * as isBeautifulUrlLib from '../isBeautifulUrl';
import {
    LocationLegacy,
    makeLocation,
    LocationBeautiful,
    parseDocumentId,
    ECatalogPageTypes,
    ECartPageTypes,
    EPoductPageTypes,
    getExtraQueryParams,
    EXTRA_QUERY_KEYS,
    EListingPageTypes,
} from '../location';

describe('location', () => {
    describe('LocationLegacy', () => {
        const currentLocation = {
            pathname: '/turbo',
            search: '?text=mirm.ru%2Fyandexturbocatalog%2F',
            hash: '',
            state: {},
        };

        beforeEach(() => setLocation(currentLocation));
        const locationCreator = new LocationLegacy();

        it('buildProductLocation() генерирует корректный location для продукта', () => {
            expect(locationCreator.buildProductLocation({
                // @ts-ignore
                product: {
                    id: '69802',
                    href: '/turbo?text=https%3A%2F%2Fmirm.ru%2Fcatalog%2Fproducts%2Fnightsun_gc005',
                },
            })).toEqual({
                pathname: '/turbo',
                search: '?text=https%3A%2F%2Fmirm.ru%2Fcatalog%2Fproducts%2Fnightsun_gc005&product_id=69802',
                hash: '',
                state: {},
            });
        });

        it('buildCartLocation() генерирует корректный location для корзины', () => {
            expect(locationCreator.buildCartLocation({
                shopId: 'mirm.ru',
                state: {
                    transition: 'none',
                },
            })).toEqual({
                pathname: '/turbo',
                search: '?text=mirm.ru%2Fyandexturbocart%2F',
                hash: '',
                state: {
                    transition: 'none',
                },
            });
        });

        it('buildCatalogLocation() генерирует корректный location', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
            })).toEqual({
                pathname: '/turbo',
                search: '?text=mirm.ru%2Fyandexturbocatalog%2F',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location c pageType', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                pageType: ECatalogPageTypes.filter,
            })).toEqual({
                pathname: '/turbo',
                search: '?text=mirm.ru%2Fyandexturbocatalog%2F&page_type=filter',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с queryParams', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                queryParams: {
                    about: '1',
                },
            })).toEqual({
                pathname: '/turbo',
                search: '?text=mirm.ru%2Fyandexturbocatalog%2F&about=1',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с category_id', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                category: {
                    id: '12345',
                    name: 'name',
                },
            })).toEqual({
                pathname: '/turbo',
                search: '?text=mirm.ru%2Fyandexturbocatalog%2F&category_id=12345',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с query filters sort_by', () => {
            const currentLocation = {
                pathname: '/turbo',
                search: '?text=super01.ru%2Fyandexturbocatalog%2F&query=Детский&filters=p1%3A128&text=super01.ru%2Fyandexturbocatalog%2F&sort_by=price',
                hash: '',
                state: {},
            };
            setLocation(currentLocation);

            expect(locationCreator.buildCatalogLocation({
                shopId: 'super01.ru',
                category: {
                    id: '12345',
                    name: 'name',
                },
            })).toEqual({
                pathname: '/turbo',
                search: '?text=super01.ru%2Fyandexturbocatalog%2F&category_id=12345&query=%D0%94%D0%B5%D1%82%D1%81%D0%BA%D0%B8%D0%B9&filters=p1%3A128&sort_by=price',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location не добавляя доп параметров', () => {
            const currentLocation = {
                pathname: '/turbo',
                search: '?text=super01.ru%2Fyandexturbocatalog%2F&query=Детский&filters=p1%3A128&text=super01.ru%2Fyandexturbocatalog%2F&sort_by=price',
                hash: '',
                state: {},
            };
            setLocation(currentLocation);

            expect(locationCreator.buildCatalogLocation({
                shopId: 'super01.ru',
                category: {
                    id: '12345',
                    name: 'name',
                },
                strict: true,
            })).toEqual({
                pathname: '/turbo',
                search: '?text=super01.ru%2Fyandexturbocatalog%2F&category_id=12345',
                hash: '',
                state: {},
            });
        });
    });

    describe('LocationBeautiful', () => {
        const currentLocation = {
            pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
            search: '',
            hash: '',
            state: {},
        };
        const locationCreator = new LocationBeautiful();

        beforeEach(() => setLocation(currentLocation));

        it('buildProductLocation() генерирует корректный location для продукта', () => {
            expect(locationCreator.buildProductLocation({
                // @ts-ignore
                product: {
                    id: '69802',
                    href: 'https://yandex.ru/turbo/mirm.ru/s/catalog/products/nightsun_gc005',
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/s/catalog/products/nightsun_gc005',
                search: '?product_id=69802',
                hash: '',
                state: {},
            });
        });

        it('buildProductLocation() генерирует корректный location для продукта с параметрами паблишера', () => {
            const pcgi = encodeURIComponent('foo=bar&baz=1');
            expect(locationCreator.buildProductLocation({
                // @ts-ignore
                product: {
                    id: '69802',
                    href: `https://yandex.ru/turbo/mirm.ru/s/catalog/products/nightsun_gc005?pcgi=${pcgi}`,
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/s/catalog/products/nightsun_gc005',
                search: `?product_id=69802&pcgi=${pcgi}`,
                hash: '',
                state: {},
            });
        });

        it('buildProductLocation() генерирует корректный location для оплаты в один клик', () => {
            expect(locationCreator.buildProductLocation({
                // @ts-ignore
                product: {
                    id: '69802',
                    href: 'https://yandex.ru/turbo/mirm.ru/s/catalog/products/nightsun_gc005',
                },
                pageType: EPoductPageTypes.payment,
            })).toEqual({
                pathname: '/turbo/mirm.ru/s/catalog/products/nightsun_gc005',
                search: '?product_id=69802&page_type=payment',
                hash: '',
                state: {},
            });
        });

        it('buildCartLocation() генерирует корректный location для корзины', () => {
            expect(locationCreator.buildCartLocation({
                shopId: 'mirm.ru',
                state: {
                    transition: 'none',
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocart/',
                search: '',
                hash: '',
                state: {
                    transition: 'none',
                },
            });
        });

        it('buildCartLocation() генерирует корректный location для оформления заказа в корзине', () => {
            expect(locationCreator.buildCartLocation({
                shopId: 'mirm.ru',
                pageType: ECartPageTypes.payment,
                state: {
                    transition: 'none',
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocart/',
                search: '?page_type=payment',
                hash: '',
                state: {
                    transition: 'none',
                },
            });
        });

        it('buildCatalogLocation() генерирует корректный location', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
                search: '',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с queryParams', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                queryParams: {
                    about: '1',
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
                search: '?about=1',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с category_id', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                category: {
                    id: '12345',
                    name: 'name',
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
                search: '?category_id=12345',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с page_type', () => {
            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                pageType: ECatalogPageTypes.aboutDetail,
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/about_detail/',
                search: '',
                hash: '',
                state: {},
            });
        });

        it('buildListingLocation() генерирует корректный location для листингов', () => {
            expect(locationCreator.buildListingLocation({
                shopId: 'mirm.ru',
                pathname: '/turbo/mirm.ru/listinghttps/catalog/path',
                pageType: EListingPageTypes.filter,
            })).toEqual({
                pathname: '/turbo/mirm.ru/listinghttps/catalog/path',
                search: '?page_type=filter',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location с query filters sort_by', () => {
            const currentLocation = {
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog',
                search: '?query=Детский&filters=p1%3A128&text=super01.ru%2Fyandexturbocatalog%2F&sort_by=price',
                hash: '',
                state: {},
            };
            setLocation(currentLocation);

            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                category: {
                    id: '12345',
                    name: 'name',
                },
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
                search: '?category_id=12345&query=%D0%94%D0%B5%D1%82%D1%81%D0%BA%D0%B8%D0%B9&filters=p1%3A128&sort_by=price',
                hash: '',
                state: {},
            });
        });

        it('buildCatalogLocation() генерирует корректный location не добавляя доп параметров', () => {
            const currentLocation = {
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog',
                search: '?query=Детский&filters=p1%3A128&text=super01.ru%2Fyandexturbocatalog%2F&sort_by=price',
                hash: '',
                state: {},
            };
            setLocation(currentLocation);

            expect(locationCreator.buildCatalogLocation({
                shopId: 'mirm.ru',
                category: {
                    id: '12345',
                    name: 'name',
                },
                strict: true,
            })).toEqual({
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
                search: '?category_id=12345',
                hash: '',
                state: {},
            });
        });
    });

    describe('makeLocation', () => {
        let isBeautifulUrlSpy: jest.SpyInstance;

        beforeAll(() => {
            isBeautifulUrlSpy = jest.spyOn(isBeautifulUrlLib, 'isBeautifulUrl');
        });
        afterEach(() => {
            jest.clearAllMocks();
        });
        afterAll(() => {
            jest.restoreAllMocks();
        });

        it('Возвращает экземпляр LocationLegacy, если формат текущего урла старый', () => {
            const currentLocation = {
                pathname: '/turbo',
                search: '?text=mirm.ru%2Fyandexturbocatalog%2F',
                hash: '',
                state: {},
            };
            setLocation(currentLocation);
            isBeautifulUrlSpy.mockReturnValue(false);

            const locationCreator = makeLocation();

            expect(locationCreator).toBeInstanceOf(LocationLegacy);
        });

        it('Возвращает экземпляр LocationBeautiful, если формат текущего урла новый', () => {
            const currentLocation = {
                pathname: '/turbo/mirm.ru/n/yandexturbocatalog/',
                search: '',
                hash: '',
                state: {},
            };
            setLocation(currentLocation);
            isBeautifulUrlSpy.mockReturnValue(true);

            const locationCreator = makeLocation(currentLocation);

            expect(locationCreator).toBeInstanceOf(LocationBeautiful);
        });
    });

    describe('parseDocumentId', () => {
        it('Возвращает идентификатор документа из text', () => {
            expect(parseDocumentId('/turbo?text=example.com')).toEqual('example.com');
        });

        it('Возвращает идентификатор документа из пути', () => {
            expect(parseDocumentId('/turbo/mirm.ru/n/product')).toEqual('/turbo/mirm.ru/n/product');
        });

        it('Работает идентично для абсолютных ссылок', () => {
            expect(parseDocumentId('https://yandex.ru/turbo/mirm.ru/n/product')).toEqual('/turbo/mirm.ru/n/product');
            expect(parseDocumentId('https://yandex.ru/turbo?text=https://example/com')).toEqual('https://example/com');
        });

        it('Возвращает идентификатор документа с параметрами паблишера', () => {
            const encodedPublisherCGI = encodeURIComponent('foo=bar&baz=1');
            expect(
                parseDocumentId(`/turbo/mirm.ru/n/product/?cgi=123&pcgi=${encodedPublisherCGI}`)
            ).toEqual(`/turbo/mirm.ru/n/product/?pcgi=${encodedPublisherCGI}`);
        });
    });

    describe('getExtraQueryParams', () => {
        EXTRA_QUERY_KEYS.forEach(key => {
            it(`Добавляет параметр ${key}`, () => {
                const value = 'test-value';
                const qs = new URLSearchParams({ [key]: value });

                const currentLocation = {
                    pathname: '/turbo',
                    search: qs.toString(),
                    hash: '',
                    state: {},
                };

                setLocation(currentLocation);

                expect(getExtraQueryParams()).toEqual({ [key]: [value] });
            });
        });

        it('Добавляет все допустимые параметры при их наличии', () => {
            const keys = {};
            EXTRA_QUERY_KEYS.forEach((key, i) => {
                keys[key] = [String(i)];
            });
            const qs = new URLSearchParams(keys);

            const currentLocation = {
                pathname: '/turbo',
                search: qs.toString(),
                hash: '',
                state: {},
            };

            setLocation(currentLocation);

            expect(getExtraQueryParams()).toEqual(keys);
        });

        if (Array.from(EXTRA_QUERY_KEYS).length > 0) {
            it('Не добавляет лишние параметры', () => {
                const keys = {
                    exp_flags: ['test-value'],
                };
                const evil = {
                    evilKey: 'evil-value',
                };
                // Конструктор ругается на передачу Record<string, string[]>,
                // хотя умеет с такими значениями работать.
                // @ts-ignore
                const qs = new URLSearchParams({ ...keys, ...evil });

                const currentLocation = {
                    pathname: '/turbo',
                    search: qs.toString(),
                    hash: '',
                    state: {},
                };

                setLocation(currentLocation);

                expect(
                    getExtraQueryParams()
                ).toEqual(keys);
            });
        }
    });
});
