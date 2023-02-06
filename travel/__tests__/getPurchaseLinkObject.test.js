import {YBUS} from '../tariffSources';
import {SEARCH_SEGMENT_MEDIUM} from '../../url/ybusUrl';

import {TransportType} from '../../transportType';
import HttpMethod from '../../../interfaces/HttpMethod';

import getPurchaseLinkObject from '../getPurchaseLinkObject';

jest.mock('../../url/applyUtm', () => jest.fn(url => url));

const deepUrl = 'https://rasp.yandex.ru/deepUrl';
const orderUrl = 'https://rasp.yandex.ru/orderUrl';
const partnerOrderRequestUrl = 'https://rasp.yandex.ru/partnerOrderRequest';

const busSegment = {
    transport: {
        code: TransportType.bus,
    },
};
const yandexBusSegment = {
    transport: {
        code: TransportType.bus,
    },
    source: YBUS,
};
const planeSegment = {
    transport: {
        code: TransportType.plane,
    },
};
const partnerOrderRequestGet = {
    url: partnerOrderRequestUrl,
    httpMethod: HttpMethod.get,
    params: {},
};
const partnerOrderRequestGetWithParameters = {
    url: partnerOrderRequestUrl,
    httpMethod: HttpMethod.get,
    params: {
        test: '1',
    },
};
const partnerOrderRequestPost = {
    url: partnerOrderRequestUrl,
    httpMethod: HttpMethod.post,
    params: {
        test: '1',
    },
};

describe('getPurchaseLinkObject', () => {
    it('Ссылка на покупку автобусов', () => {
        const mainParameters = {
            segment: yandexBusSegment,
            tariffClass: {
                parsedUrl: {
                    pathname: 'https://yandex.ru/bus',
                    query: {},
                },
            },
            isMobile: false,
        };

        expect(
            getPurchaseLinkObject({
                ...mainParameters,
                medium: SEARCH_SEGMENT_MEDIUM,
            }),
        ).toEqual({
            href: 'https://yandex.ru/bus?utm_medium=search_segment',
            method: HttpMethod.get,
            POSTParams: {},
        });

        expect(getPurchaseLinkObject(mainParameters)).toEqual({
            href: 'https://yandex.ru/bus',
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, нет orderUrl', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {deepUrl},
                isMobile: false,
            }),
        ).toEqual({
            href: deepUrl,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: нет deepUrl, есть orderUrl', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {orderUrl},
                isMobile: false,
            }),
        ).toEqual({
            href: orderUrl,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                },
                isMobile: false,
            }),
        ).toEqual({
            href: deepUrl,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с HttpMethod.get-запросом. Мобильная версия. Автобус', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestGet,
                },
                isMobile: true,
            }),
        ).toEqual({
            href: partnerOrderRequestUrl,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с POST-запросом. Мобильная версия. Автобус', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestPost,
                },
                isMobile: true,
            }),
        ).toEqual({
            href: partnerOrderRequestUrl,
            method: HttpMethod.post,
            POSTParams: {
                test: '1',
            },
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с HttpMethod.get-запросом и параметрами. Мобильная версия. Автобус', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestGetWithParameters,
                },
                isMobile: true,
            }),
        ).toEqual({
            href: `${partnerOrderRequestUrl}?test=1`,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с POST-запросом. Мобильная версия. Самолет', () => {
        expect(
            getPurchaseLinkObject({
                segment: planeSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestPost,
                },
                isMobile: true,
            }),
        ).toEqual({
            href: deepUrl,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с HttpMethod.get-запросом. Десктопная версия. Автобус', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestGet,
                },
                isMobile: false,
            }),
        ).toEqual({
            href: partnerOrderRequestUrl,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с POST-запросом. Десктопная версия. Автобус', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestPost,
                },
                isMobile: false,
            }),
        ).toEqual({
            href: partnerOrderRequestUrl,
            method: HttpMethod.post,
            POSTParams: {
                test: '1',
            },
        });
    });

    it('Ссылка на покупку: есть deepUrl, есть orderUrl, есть partnerOrderRequest с HttpMethod.get-запросом и параметрами. Десктопная версия. Автобус', () => {
        expect(
            getPurchaseLinkObject({
                segment: busSegment,
                tariffClass: {
                    deepUrl,
                    orderUrl,
                    partnerOrderRequest: partnerOrderRequestGetWithParameters,
                },
                isMobile: false,
            }),
        ).toEqual({
            href: `${partnerOrderRequestUrl}?test=1`,
            method: HttpMethod.get,
            POSTParams: {},
        });
    });
});
