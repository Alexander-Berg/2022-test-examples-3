/* eslint-disable import/extensions, import/no-unresolved, @typescript-eslint/explicit-module-boundary-types */

import {ServerResponse} from 'http';

// @ts-ignore
import _ from 'lodash';
// @ts-ignore
import ResourceApi from '@yandex-market/mandrel/decorators/ResourceApi';
// @ts-ignore
import CSPWidget from '@yandex-market/mandrel/widgets/ContentSecurityPolicy';
// @ts-ignore
import {Page} from '@yandex-market/stout';
// @ts-ignore
import type StoutUser from '@yandex-market/mandrel/lib/User/StoutUser';
import type {
    Context,
    StoutPage,
    StoutRequest,
    // @ts-ignore
} from '@yandex-market/mandrel/context';
// @ts-ignore
import {createContext as createContextOrigin} from '@yandex-market/mandrel/context';
import deepmerge from 'deepmerge';

import {
    createRequestFake,
    createResponseFake,
    createRouteStub,
    createUserFake,
    createDefaultRouteConfig,
    Route,
    // @ts-ignore
} from '../../../platform/mandrel/stoutTestHelpers';

export type InitRouteArg = {
    name: string;
    data: {
        pageData: any;
    };
};

export type InitUserArg = {
    region?: any;
    UID?: string;
    yandexuid?: string;
    browser?: any;
    settings?: {[key: string]: any};
    isNeedToConvertCurrency?: boolean;
    hasExtendedPermissions?: boolean;
    isRobot?: boolean;
    isAuth?: boolean;
    isJsAvailable?: boolean;
    defaultEmail?: string;
    isYandexEmployee?: boolean;
    isBetaTester?: boolean;
    dbFields?: string;
};

export type InitRequestArg = {
    data?: any;
    params?: any;
    ip?: string;
    port?: string;
    remoteIp?: string;
    remotePort?: string;
    method?: string;
    headers?: any;
    cookie?: any;
    body?: {[key: string]: any};
    url?: string;
    abt?: any;
};

export type InitContextArg = {
    route?: InitRouteArg;
    user?: InitUserArg;
    request?: InitRequestArg;
    page?: any;
};

function MyPage(this: any) {
    this.push(
        this.widget(CSPWidget, {
            isStrict: true,
        }).onResolve((csp: any) => {
            this.request.setData(
                'nonce',
                csp['w-content-security-policy'].nonce,
            );
        }),
    );
}

export function createPage(
    pageParams: any,
    params: {
        user: StoutUser;
        request: StoutRequest;
        response: ServerResponse;
        route: Route;
    },
): StoutPage {
    // eslint-disable-next-line no-param-reassign
    pageParams = deepmerge({}, pageParams ?? {});
    const {user, request, response, route} = params;
    const resourceConfig = (config: any) => ({
        config: {
            contextId: 'fake-context-id',
            requestId: 'fake-request-id',
            pageId: 'fake-page',
            isTest: true,
        },
        requestIdModule: {subReqId: 1},
        ...config,
    });
    const page = Page.create(MyPage)
        .decorate(ResourceApi)
        .make([request, response, route], [request]);
    // @ts-ignore
    page.contextId = 'fake-context-id';
    // @ts-ignore
    page.user = user;
    // Предотвращение очистки страницы
    page.destroy = jest.fn();

    delete pageParams.push;

    _.merge(page, {resourceConfig}, pageParams);

    // @ts-ignore
    return page;
}

export function createRoute(config?: InitRouteArg): Route {
    // eslint-disable-next-line no-param-reassign
    config = deepmerge({}, config ?? createDefaultRouteConfig());
    // eslint-disable-next-line no-void
    return createRouteStub(config);
}

export function createUser(params?: InitUserArg): StoutUser {
    // eslint-disable-next-line no-param-reassign
    params = deepmerge({}, params ?? {});
    const user = createUserFake(params);
    // @ts-ignore
    user.tld = user.tld || 'ru';
    // @ts-ignore
    user.region.info = {
        data: [
            {
                latitude: 0,
                longitude: 0,
                zoom: 0,
            },
        ],
    };
    // todo перенести в хелпе
    // @ts-ignore
    user.region.geobase = {
        __countriesById: {
            // Moscow - Russia
            213: 225,
            // Ruan - France
            105065: 124,
        },
        __regionByIpMap: {
            '10.10.10.10': 213,
            '11.11.11.11': 105065,
        },
        getRegionByIp(this: any, ip: string) {
            return Promise.resolve({
                id: this.__regionByIpMap[ip] || 213,
            });
        },
        getCountryInfo(id: number) {
            // @ts-ignore
            return Promise.resolve({id: this.__countriesById[id]});
        },
        // @ts-ignore
        getInfo(id: number) {
            return Promise.resolve({
                id,
                name: 'Тествиль',
                country: 'Тестляндия',
                linguistics: {
                    preposition: 'в',
                    prepositional: 'Тествилле',
                },
                data: {},
            });
        },
    };

    return user;
}

export function createRequest(
    user: StoutUser,
    params?: InitRequestArg,
): StoutRequest {
    // eslint-disable-next-line no-param-reassign
    params = deepmerge({}, params ?? {});
    const request = createRequestFake(params);
    request.setData('user', user);
    request.headers['x-market-req-id'] =
        request.headers['x-market-req-id'] || 'fake-req-id';
    request.data.domainZone = request.data.domainZone || 'ru';

    // @ts-ignore
    return request;
}

export function createResponse(): ServerResponse {
    // @ts-ignore
    return createResponseFake();
}

export function createContext(params?: InitContextArg): Context {
    if (params) {
        // eslint-disable-next-line no-param-reassign
        params = deepmerge({}, params || {});
    }

    const route = createRoute(params?.route);
    const user = createUser(params?.user);
    const request = createRequest(user, params?.request);
    const response = createResponse();
    const page = createPage(params?.page, {user, request, response, route});

    return createContextOrigin(page);
}
