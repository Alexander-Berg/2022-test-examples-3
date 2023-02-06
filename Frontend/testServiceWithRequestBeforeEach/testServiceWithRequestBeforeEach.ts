import * as sinon from 'sinon';

import { SpecRequest } from 'libs/Request/SpecRequest';

import { Store } from 'schema/state/Store';

import { createReduxStore } from 'chan/createReduxStore/createReduxStore';
import { Router } from 'chan/createRouter/Router';

export interface TestServiceWithRequestCallbackOptions {
    di: DiContainer & {
        set(key: string, value: any): void;
    };
    redux: Store;
    request: SpecRequest;
}

export type TestServiceWithRequestCallback = (options: TestServiceWithRequestCallbackOptions) => Promise<any>;

export function testServiceWithRequestBeforeEach(callback: TestServiceWithRequestCallback) {
    beforeEach(async() => {
        let redux: Store;
        let request: SpecRequest;
        let di: TestServiceWithRequestCallbackOptions['di'];
        let router: Router;
        let diHash: Dict = {};

        request = new SpecRequest({});

        di = function(key) {
            if (typeof key === 'object') {
                return Object.keys(key).reduce((memo, key) => {
                    memo[key] = di(key);

                    return memo;
                }, {});
            }
            if (key === 'request') {
                return request;
            }

            if (key === 'redux') {
                return redux;
            }

            if (key === 'router') {
                if (!router) {
                    router = new Router({}, async() => {});

                    router.navigate = sinon.spy();
                }

                return router;
            }

            if (diHash[key]) {
                return diHash[key];
            }

            throw new Error('Not supported DI key: ' + key);
        } as any;

        di.set = function(key, value) {
            diHash[key] = value;
        };

        redux = createReduxStore({} as any, di);

        await callback({ request, redux, di });
    });
}
