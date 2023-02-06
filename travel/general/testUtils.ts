import Lang from '../../interfaces/Lang';
import IStore from '../../interfaces/IStore';
import IStateFlags from '../../interfaces/state/flags/IStateFlags';
import IState from '../../interfaces/state/IState';
import Dispatch from '../../interfaces/actions/Dispatch';
import Req from '../../interfaces/router/Req';
import Res from '../../interfaces/router/Res';
import IRouteMiddlewareParams from '../../interfaces/router/IRouteMiddlewareParams';
import IApi from '../../interfaces/api/IApi';
import GetParameters from '../../interfaces/lib/url/GetParameters';

import noop from '../../lib/noop';

export function getStore(
    state?: Record<string, any>,
    dispatch?: Dispatch,
): IStore {
    dispatch = dispatch || noop;

    return {
        dispatch,
        getState: () =>
            ({
                language: Lang.ru,
                flags: {} as IStateFlags,
                ...(state || {}),
            } as unknown as IState),
    } as unknown as IStore;
}

interface IGetReq {
    params?: Partial<Record<string, string>>;
    query?: GetParameters;
    originalUrl?: string;
}

export function getReq({
    params = {},
    query = {},
    originalUrl = '',
}: IGetReq): Req {
    return {
        params,
        query,
        originalUrl,
    } as Req;
}

interface IGetRes {
    redirect?: Res['redirect'];
    render?: Res['render'];
    set?: (header: string, value: string) => Res;
    get?: (header: string) => string;
}

export function getRes({
    redirect = noop,
    render = () => Promise.resolve(),
    set,
    get,
}: IGetRes): Res {
    return {
        redirect,
        render,
        set:
            set ||
            function (this: Res) {
                return this;
            },
        get: get || (() => ''),
    } as unknown as Res;
}

type Next = IRouteMiddlewareParams['next'];

function defaultNext(err?: any): void {
    if (err) {
        throw err;
    }
}

export function getNext(next: Next = defaultNext): Next {
    return next;
}

export function getApi(methods: IApi): IApi {
    return {
        exec: noop,
        ...methods,
    } as IApi;
}
