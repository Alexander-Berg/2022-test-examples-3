import { URL } from 'url';
import { fetchSkuCard, fetchAlsoViewed, fetchSearch, fetchAccessories } from '..';
import * as utils from '../../resources/utils';
import { mockRequest } from '../../mocks/request';
import { Report } from '../../resources/report/report';
import { Places } from '../../resources/report/types';
import { Pages } from '../../router/routes';

describe('Метод:', () => {
    // в тесте у всех фетчеров будет одна и таже страница
    const pageId = Pages.LIST;
    let reportGetUrl: ReturnType<typeof jest.spyOn>;
    let filterQueryParams: ReturnType<typeof jest.spyOn>;

    beforeEach(() => {
        reportGetUrl = jest.spyOn(Report.prototype, 'getUrl');
        filterQueryParams = jest.spyOn(utils, 'filterQueryParams');
    });

    afterEach(() => {
        filterQueryParams.mockClear();
        reportGetUrl.mockClear();
    });

    describe('fetchSkuCard', () => {
        const skuId = '10101';

        it('должен правильно формировать объект запроса без дополнительных параметров в url', () => {
            const url = 'http://some.com/path/to';

            // перед вызовом мокаем возвращаемое значение
            reportGetUrl.mockReturnValue(url);

            const receivedUrl = fetchSkuCard({
                req: mockRequest(),
                url: new URL(url),
                pageId,
                data: {
                    skuId,
                },
            });

            expect(reportGetUrl).toBeCalledWith({
                place: Places.SKU_CARD,
                mainOptions: {
                    skuId,
                },
            });
            expect(filterQueryParams).not.toHaveBeenCalled();
            expect(receivedUrl).toEqual({
                uri: url,
            });
        });

        it('должен правильно формировать объект запроса c дополнительными параметрами в url', () => {
            const url = 'https://path/to/somth' +
                '?gallery_number=1&gallery_position=2&lr=3&clid=4&uuid=5&wprid=6&notValid=7';
            const urlObj = new URL(url);

            reportGetUrl.mockReturnValue(url);

            fetchSkuCard({
                req: mockRequest({ url }),
                url: urlObj,
                pageId,
                data: {
                    skuId,
                },
            });

            expect(filterQueryParams).toHaveBeenCalledWith(urlObj, [
                'gallery_number',
                'gallery_position',
                'lr',
                'clid',
                'uuid',
                'wprid',
            ]);
        });

        it('должен кидать исключение если отсутствует "data"', () => {
            const url = 'http://some.com/path/to';

            expect(() => {
                return fetchSkuCard({
                    req: mockRequest(),
                    url: new URL(url),
                    pageId,
                });
            }).toThrow('skuCard: route data is not passed');
        });
    });
    describe('fetchAlsoViewed', () => {
        const modelId = '10101';

        it('должен правильно формировать объект запроса без дополнительных параметров в url', () => {
            const url = 'http://some.com/path/to';

            // перед вызовом мокаем возвращаемое значение
            reportGetUrl.mockReturnValue(url);

            const receivedUrl = fetchAlsoViewed({
                req: mockRequest(),
                url: new URL(url),
                pageId,
                data: {
                    modelId,
                },
            });

            expect(reportGetUrl).toBeCalledWith({
                place: Places.ALSO_VIEWED,
                mainOptions: {
                    modelId,
                },
            });
            expect(filterQueryParams).not.toHaveBeenCalled();
            expect(receivedUrl).toEqual({
                uri: url,
            });
        });

        it('должен правильно формировать объект запроса c дополнительными параметрами в url', () => {
            const url = 'https://path/to/somth' +
                '?gallery_number=1&gallery_position=2&lr=3&clid=4&uuid=5&wprid=6&notValid=7';
            const urlObj = new URL(url);

            reportGetUrl.mockReturnValue(url);

            fetchAlsoViewed({
                req: mockRequest({ url }),
                url: urlObj,
                pageId,
                data: {
                    modelId,
                },
            });

            expect(filterQueryParams).toHaveBeenCalledWith(urlObj, [
                'gallery_number',
                'gallery_position',
                'lr',
                'clid',
                'uuid',
                'wprid',
            ]);
        });

        it('должен кидать исключение если отсутствует "data"', () => {
            const url = 'http://some.com/path/to';

            expect(() => {
                return fetchAlsoViewed({
                    req: mockRequest(),
                    url: new URL(url),
                    pageId,
                });
            }).toThrow('alsoViewed: route data is not passed');
        });
    });
    describe('fetchSearch', () => {
        const nid = '10';
        let url: string;

        beforeEach(() => {
            url = 'http://some.com/path/to';
        });

        it('должен правильно формировать объект запроса без дополнительных параметров в url', () => {
            // перед вызовом мокаем возвращаемое значение
            reportGetUrl.mockReturnValue(url);

            const receivedUrl = fetchSearch({
                req: mockRequest(),
                url: new URL(url),
                pageId,
                data: {
                    nid,
                },
            });

            expect(reportGetUrl).toBeCalledWith({
                place: Places.SEARCH,
                mainOptions: {
                    nid,
                    count: 6,
                    noSearchResults: 0,
                },
            });
            expect(filterQueryParams).not.toHaveBeenCalled();
            expect(receivedUrl).toEqual({
                uri: url,
            });
        });

        it('для страницы фильтров параметр noSearchResults должен быть равен 1', () => {
            reportGetUrl.mockReturnValue(url);
            fetchSearch({
                req: mockRequest(),
                url: new URL(url),
                pageId: Pages.FILTERS,
                data: {
                    nid,
                },
            });

            expect(reportGetUrl).toBeCalledWith(expect.objectContaining({
                mainOptions: expect.objectContaining({
                    noSearchResults: 1,
                }),
            }));
        });

        it('должен правильно формировать объект запроса c дополнительными параметрами в url', () => {
            url += '?page=3&notValid=7';

            const urlObj = new URL(url);

            reportGetUrl.mockReturnValue(url);

            fetchSearch({
                req: mockRequest({ url }),
                url: urlObj,
                pageId,
                data: {
                    nid,
                },
            });

            expect(reportGetUrl).toBeCalledWith({
                place: Places.SEARCH,
                mainOptions: {
                    nid,
                    count: 6,
                    noSearchResults: 0,
                },
                additionalOptions: {
                    page: '3',
                },
            });
            expect(filterQueryParams).toHaveBeenCalledWith(urlObj, [
                'gallery_number',
                'gallery_position',
                'lr',
                'clid',
                'uuid',
                'wprid',
                'how',
                'glfilter',
                'priceto',
                'pricefrom',
                'filter-delivery-perks-eligible',
                'blue-fast-delivery',
                'deliveryincluded',
                'filter-discount-only',
                'page',
                'hid',
                'prune',
            ]);
        });

        it('парамер count должен быть равен 10, если присутствует query параметр prune', () => {
            url += '?prune=2000';

            const urlObj = new URL(url);

            reportGetUrl.mockReturnValue(url);

            fetchSearch({
                req: mockRequest({ url }),
                url: urlObj,
                pageId,
                data: {
                    nid,
                },
            });

            expect(reportGetUrl).toBeCalledWith(expect.objectContaining({
                mainOptions: expect.objectContaining({
                    count: 10,
                }),
            }));
        });

        it('должен кидать исключение если отсутствует "data"', () => {
            const url = 'http://some.com/path/to';

            expect(() => {
                fetchSearch({
                    req: mockRequest(),
                    pageId,
                    url: new URL(url),
                });
            }).toThrow(`fetchSearch: route data is not passed. url=${url}`);
        });
    });
    describe('fetchAccessories', () => {
        const modelId = '10101';

        it('должен правильно формировать объект запроса без дополнительных параметров в url', () => {
            const url = 'http://some.com/path/to';

            // перед вызовом мокаем возвращаемое значение
            reportGetUrl.mockReturnValue(url);

            const receivedUrl = fetchAlsoViewed({
                req: mockRequest(),
                url: new URL(url),
                pageId,
                data: {
                    modelId,
                },
            });

            expect(reportGetUrl).toBeCalledWith({
                place: Places.ALSO_VIEWED,
                mainOptions: {
                    modelId,
                },
            });
            expect(filterQueryParams).not.toHaveBeenCalled();
            expect(receivedUrl).toEqual({
                uri: url,
            });
        });

        it('должен правильно формировать объект запроса c дополнительными параметрами в url', () => {
            const url = 'https://path/to/somth' +
                '?gallery_number=1&gallery_position=2&lr=3&clid=4&uuid=5&wprid=6&notValid=7';
            const urlObj = new URL(url);

            reportGetUrl.mockReturnValue(url);

            fetchAccessories({
                req: mockRequest({ url }),
                url: urlObj,
                pageId,
                data: {
                    modelId,
                },
            });

            expect(filterQueryParams).toHaveBeenCalledWith(urlObj, [
                'gallery_number',
                'gallery_position',
                'lr',
                'clid',
                'uuid',
                'wprid',
            ]);
        });

        it('должен кидать исключение если отсутствует "data"', () => {
            const url = 'http://some.com/path/to';

            expect(() => {
                return fetchAccessories({
                    req: mockRequest(),
                    url: new URL(url),
                    pageId,
                });
            }).toThrow('accessories: route data is not passed');
        });
    });
});
