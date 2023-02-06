import { omit } from 'lodash/fp';
import { ADULT } from '@yandex-turbo/applications/beru.ru/constants/cookies';
import * as cookieHelpers from '@yandex-turbo/applications/beru.ru/helpers/cookie';
import { Report } from '../report';
import { Places } from '../types';
import { mockRequest } from '../../../mocks/request';
import * as utils from '../../utils';

describe('Report', () => {
    const id = '1010';
    const additionalOptions = {
        lr: '22',
        clid: '99',
        uuid: '1234',
        wprid: 'string',
        gallery_position: 'string',
        gallery_number: 'string',
    };
    const host = {
        development: 'http://warehouse-report.blue.tst.vs.market.yandex.net:17051/yandsearch',
        testing: 'http://warehouse-report.blue.tst.vs.market.yandex.net:17051/yandsearch',
        production: 'http://turbo-report.blue.vs.market.yandex.net:17051/yandsearch',
    };
    let report: Report;
    let getUrl: ReturnType<typeof jest.spyOn>;
    let request: ReturnType<typeof mockRequest>;

    function makeExpectedParams(
        env: string,
        request: ReturnType<typeof mockRequest>,
        additionalOptions: Record<string, string> = {},
    ) {
        const options = omit(['clid', 'lr', 'priceto', 'pricefrom', 'prune'])(additionalOptions);
        const { lr, priceto, pricefrom, prune } = additionalOptions;

        lr && (options.rids = lr);
        pricefrom && (options.mcpricefrom = pricefrom);
        priceto && (options.mcpriceto = priceto);
        prune && (options['prime-prune-count'] = prune);

        return {
            rgb: 'blue',
            'new-picture-format': '1',
            'pickup-options': 'grouped',
            rids: env === 'production' ? '{region_id}' : '213',
            'show-models-specs': 'full',
            'use-multi-navigation-trees': '1',
            'with-rebuilt-model': '1',
            client: 'turbo_report',
            ip: request.httpXRealIP,
            pof: additionalOptions.clid ? JSON.stringify({
                clid: [additionalOptions.clid],
                mclid: null,
                vid: null,
                distr_type: null,
                opp: null,
            }) : undefined,
            gallery_position: additionalOptions.gallery_position,
            gallery_number: additionalOptions.gallery_number,
            uuid: additionalOptions.uuid,
            wprid: additionalOptions.wprid,
            'rearr-factors': 'pokupki_instead_beru_enabled=1',
            ...options,
        };
    }

    ['development', 'testing', 'production'].forEach(env => {
        describe(`В ${env} окружении`, () => {
            describe('метод getUrl', () => {
                beforeEach(() => {
                    process.env.NODE_ENV = env;

                    request = mockRequest();
                    report = new Report(request);
                    getUrl = jest.spyOn(utils, 'getUrlPath');
                });

                afterEach(() => {
                    getUrl.mockClear();
                });

                describe('place = turbo', () => {
                    let placeExpectedParams:Record<string, string>;

                    beforeEach(() => {
                        placeExpectedParams = {
                            place: Places.SKU_CARD,
                            pp: env === 'production' ? '1631' : '18',
                            'market-sku': id,
                            'show-urls': 'turboBundle',
                            'show-preorder': '1',
                        };
                    });

                    it('должен правильно строить url без дополнительных параметров', () => {
                        const params = makeExpectedParams(env, request, placeExpectedParams);

                        report.getUrl({
                            place: Places.SKU_CARD,
                            mainOptions: {
                                skuId: id,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], params);
                    });

                    it('должен правильно строить url c дополнительными параметрами', () => {
                        const params = makeExpectedParams(env, request, Object.assign(placeExpectedParams, additionalOptions));

                        report.getUrl({
                            place: Places.SKU_CARD,
                            mainOptions: {
                                skuId: id,
                            },
                            additionalOptions: {
                                ...additionalOptions,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], params);
                    });
                });
                describe('place = also_viewed', () => {
                    let placeExpectedParams: Record<string, string>;

                    beforeEach(() => {
                        placeExpectedParams = {
                            place: Places.ALSO_VIEWED,
                            pp: env === 'production' ? '1631' : '18',
                            hyperid: id,
                            'also-viewed-short-format': '1',
                            numdoc: '6',
                        };
                    });

                    it('должен правильно строить url без дополнительных параметров', () => {
                        const params = makeExpectedParams(env, request, placeExpectedParams);

                        report.getUrl({
                            place: Places.ALSO_VIEWED,
                            mainOptions: {
                                modelId: id,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], params);
                    });

                    it('должен правильно строить url c дополнительными параметрами', () => {
                        const params = makeExpectedParams(env, request, Object.assign(placeExpectedParams, additionalOptions));

                        report.getUrl({
                            place: Places.ALSO_VIEWED,
                            mainOptions: {
                                modelId: id,
                            },
                            additionalOptions: {
                                ...additionalOptions,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], params);
                    });
                });

                describe('place = search', () => {
                    const nid = '15';
                    const count = 6;
                    const noSearchResults = 1;
                    let placeGeneralPrams: Record<string, string>;
                    let placeAdditionalParams: Record<string, string>;
                    let parseCookie: ReturnType<typeof jest.spyOn>;

                    beforeEach(() => {
                        placeAdditionalParams = {
                            onstock: '1',
                            page: '1',
                            glfilter: '32344:2323',
                            priceto: '1223',
                            pricefrom: '32',
                            'filter-delivery-perks-eligible': '1',
                            'blue-fast-delivery': '1',
                            deliveryincluded: '1',
                            'filter-discount-only': '1',
                            hid: 'string',
                            prune: '2000',
                        };
                        placeGeneralPrams = {
                            place: Places.SEARCH,
                            pp: env === 'production' ? '4607' : '18',
                            numdoc: String(count),
                            nid,
                            'show-urls': 'turboBundle',
                            'allow-collapsing': '0',
                            onstock: '1',
                            nosearchresults: '1',
                            showVendors: 'all',
                            how: 'dpop',
                            adult: '0',
                        };
                        parseCookie = jest.spyOn(cookieHelpers, 'parseCookie');
                        parseCookie.mockImplementation(() => ({}));
                    });

                    it('должен правильно строить url без дополнительных параметров', () => {
                        const params = makeExpectedParams(env, request, placeGeneralPrams);

                        report.getUrl({
                            place: Places.SEARCH,
                            mainOptions: {
                                nid,
                                count,
                                noSearchResults,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], params);
                    });

                    it('должен правильно строить url с долнительными параметрами', () => {
                        const params = makeExpectedParams(env, request, {
                            ...placeGeneralPrams,
                            ...additionalOptions,
                            ...placeAdditionalParams,
                        });

                        report.getUrl({
                            place: Places.SEARCH,
                            mainOptions: {
                                nid,
                                count,
                                noSearchResults,
                            },
                            additionalOptions: {
                                ...additionalOptions,
                                ...placeAdditionalParams,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], params);
                    });

                    it('если выставленя кука ADULT, то в запрос к репорту должен передаваться параметр adult=1', () => {
                        parseCookie.mockImplementation(() => ({ [ADULT]: 'adult' }));
                        report.getUrl({
                            place: Places.SEARCH,
                            mainOptions: {
                                nid,
                                count,
                                noSearchResults,
                            },
                        });

                        expect(getUrl).toHaveBeenCalledWith(host[env], expect.objectContaining({
                            adult: '1',
                        }));
                    });
                });

                it('должен возвращать ошибку если place не найден', () => {
                    const place = 'UNDEFINED';
                    const cb = () => {
                        report.getUrl({
                            // @ts-ignore
                            place,
                        });
                    };

                    expect(cb).toThrow(`Resource: handler for report place "${place}" is not defined`);
                });
            });
        });
    });
});
