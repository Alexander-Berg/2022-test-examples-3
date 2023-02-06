import { mount } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import { IWebphoneReducerState } from '../../../reducers/webphoneReducer';
import { CALL_STATUS, WEBPHONE_STATUS } from '../helpers/webphoneTypes';
import WebphonePanel from './WebphonePanel';

const defaultStoreObject: IWebphoneReducerState = {
    status: WEBPHONE_STATUS.registered,
    callStatus: null,
    callMetaInfo: null,
    call: null,
    action: null,
    hold: null,
    customCallStatusDescription: null,
};

const Component = (store: any) => {
    return mount(<Provider store={store}>
        <WebphonePanel/>
    </Provider>);
};

describe('Webphone panels', () => {
    it('should display RegisterPanel with status: NotRegistered', () => {
        const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
            Webphone: {
                ...defaultStoreObject,
                status: WEBPHONE_STATUS.notRegistered,
            },
        });

        const component = Component(store);
        expect(component).toMatchSnapshot();
    });

    it('should display RegisterPanel with status: Connecting', () => {
        const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
            Webphone: {
                ...defaultStoreObject,
                status: WEBPHONE_STATUS.connecting,
            },
        });

        const component = Component(store);
        expect(component).toMatchSnapshot();
    });

    it('should display CallNotClientPanel with status: Registered', () => {
        const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
            Webphone: {
                ...defaultStoreObject,
                status: WEBPHONE_STATUS.registered,
            },
        });

        const component = Component(store);
        expect(component).toMatchSnapshot();
    });

    it('should display CurrentCallPanel with status: Registered, callStatus: accepted (call a client)', () => {
        const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
            Webphone: {
                ...defaultStoreObject,
                status: WEBPHONE_STATUS.registered,
                callStatus: CALL_STATUS.accepted,
                callMetaInfo: {
                    name: 'Тестовый Тест Тестович',
                    id: '1',
                    phoneNumber: '+1234567890',
                },
            },
        });

        const component = Component(store);
        expect(component).toMatchSnapshot();
    });

    it('should display CurrentCallPanel with status: Registered, callStatus: accepted (call not a client)', () => {
        const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
            Webphone: {
                ...defaultStoreObject,
                status: WEBPHONE_STATUS.registered,
                callStatus: CALL_STATUS.accepted,
                callMetaInfo: {
                    phoneNumber: '+1234567890',
                },
            },
        });

        const component = Component(store);
        expect(component).toMatchSnapshot();
    });

    it('should display CurrentCallPanel with status: Registered, callStatus: Ended (call a client)', () => {
        const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
            Webphone: {
                ...defaultStoreObject,
                status: WEBPHONE_STATUS.registered,
                callStatus: CALL_STATUS.ended,
                callMetaInfo: {
                    name: 'Тестовый Тест Тестович',
                    id: '1',
                    phoneNumber: '+1234567890',
                },
            },
        });

        const component = Component(store);
        expect(component).toMatchSnapshot();
    });
});
