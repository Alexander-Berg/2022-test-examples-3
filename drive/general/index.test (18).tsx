import { mount } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import { IWebphoneReducerState } from '../../../../reducers/webphoneReducer';
import { WEBPHONE_STATUS } from '../../helpers/webphoneTypes';
import Call2 from './Call2';
import EndCall from './EndCall';
import ForwardCall from './ForwardCall';
import HoldCall from './HoldCall';
import MuteCall from './MuteCall';
import WebphoneLogsDisplay from './WebphoneLogsDisplay';

const defaultStoreObject: IWebphoneReducerState = {
    status: WEBPHONE_STATUS.registered,
    callStatus: null,
    callMetaInfo: null,
    call: null,
    action: null,
    hold: null,
    customCallStatusDescription: null,
    mute: null,
};

const store = configureMockStore<{ Webphone: IWebphoneReducerState }>([thunk])({
    Webphone: defaultStoreObject,
});

describe('Webphone buttons', () => {
    it('Call button - call a client', () => {
        const component = mount(<Provider store={store}>
            <Call2 userInfo={{ name: 'Test', id: '1' }}
                   client={''}/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });

    it('Call button - call not a client', () => {
        const component = mount(<Provider store={store}>
            <Call2 client={''}/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });

    it('Forward call popup', () => {
        const component = mount(<Provider store={store}>
            <ForwardCall/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });

    it('Call logs button', () => {
        const component = mount(<Provider store={store}>
            <WebphoneLogsDisplay/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });

    it('End call button', () => {
        const component = mount(<Provider store={store}>
            <EndCall/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });

    it('Hold call button', () => {
        const component = mount(<Provider store={store}>
            <HoldCall/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });

    it('Mute call button', () => {
        const component = mount(<Provider store={store}>
            <MuteCall/>
        </Provider>);
        expect(component).toMatchSnapshot();
    });
});
