import { createAction } from 'redux-actions';
import { dispatchSerial } from 'tools-access-react-redux/src/serial';

import { requestJson } from '../../abc/react/redux/request-json';
import {
    SERVICES_UPDATE,
    SERVICES_SET_LOADING,
    updateService,
    updateServiceOptions,
} from './Services.redux';
import handleActions from './Services.redux';

describe('services', () => {
    it('should create JSON_REQUEST action in update', () => {
        const serviceId = 42;

        const actual = updateService(serviceId);

        const expected = dispatchSerial([
            createAction(SERVICES_SET_LOADING)(true),
            requestJson({
                pathname: `/back-proxy/api/frontend/services/${serviceId}/`,
                __forReducer: { serviceId },
            }, SERVICES_UPDATE),
            createAction(SERVICES_SET_LOADING)(false),
        ]);

        expect(actual).toEqual(expected);
    });

    it('should create JSON_REQUEST action in update options', () => {
        const serviceId = 42;

        const actual = updateServiceOptions(serviceId);

        const expected = dispatchSerial([
            createAction(SERVICES_SET_LOADING)(true),
            requestJson({
                pathname: `/back-proxy/api/frontend/services/${serviceId}/`,
                method: 'OPTIONS',
                __forReducer: { serviceId },
            }, SERVICES_UPDATE),
            createAction(SERVICES_SET_LOADING)(false),
        ]);

        expect(actual).toEqual(expected);
    });

    it('Should handle SERVICES_SET_LOADING action', () => {
        const actual = handleActions({}, {
            type: SERVICES_SET_LOADING,
            payload: true,
        });

        const expected = { loading: true };

        expect(actual).toEqual(expected);
    });

    it('Should handle SERVICES_UPDATE action', () => {
        const initialStore = {
            loading: false,
            error: null,
            data: {},
        };

        const actual = handleActions(initialStore, {
            type: SERVICES_UPDATE,
            payload: {
                options: { some: 'data' },
                permissions: ['some data'],
            },
            error: false,
            meta: { from: { payload: { __forReducer: { serviceId: 42 } } } },
        });

        const expected = {
            data: {
                '42': {
                    options: { some: 'data' },
                    permissions: ['some data'],
                },
            },
            error: null,
            loading: false,
        };

        expect(actual).toEqual(expected);
    });

    it('Should handle SERVICES_UPDATE error', () => {
        const initialStore = {
            loading: false,
            error: null,
            data: {},
        };

        const actual = handleActions(initialStore, {
            type: SERVICES_UPDATE,
            payload: { message: 'error' },
            error: true,
            meta: { from: { payload: { __forReducer: { serviceId: 42 } } } },
        });

        const expected = {
            loading: false,
            error: { message: 'error' },
            data: {},
        };

        expect(actual).toEqual(expected);
    });
});
