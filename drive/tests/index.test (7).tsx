import { mount, render } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import { initRules, initState } from '../../../reducers/adminUserReducer';
import { fastTags2, requestSettingsData } from '../../../reducers/tests/adminUSerReducer.mock';
import { buttonLocationDetails } from '../../../utils/sendLogs/eventTypes/buttonDetails';
import { comReducers } from '../../App/store';
import { FastTagsView } from '../component';
import FastTagsViewConnected from '../index';
import { data, dataClientInfo } from './mock';

const mockStore = configureMockStore([thunk]);

const addTag = jest.fn();

describe('FastTagsView store', () => {
    const globalState: any = {
        AdminUser: initState,
    };
    let store: any;
    let wrapper: any;

    beforeEach(() => {
        const state = comReducers(globalState, initRules(requestSettingsData, null));
        store = mockStore(state);
        wrapper = mount(<Provider store={store}><FastTagsViewConnected/></Provider>);
    });

    it('check Prop', () => {
        const fastTags = wrapper.find(FastTagsView).prop('fastTags');
        expect(fastTags).toEqual(fastTags2);
    });
});

describe('FastTagsView', () => {
    it('tags for client', () => {
        const wrapper = render(
            <FastTagsView fastTags={data}
                          addTag={addTag}
                          place={buttonLocationDetails.CLIENT_CARD}/>,
        );
        expect(wrapper).toMatchSnapshot();
    });

    it('tags for car', () => {
        const wrapper = render(
            <FastTagsView fastTags={data}
                          addTag={addTag}
                          place={buttonLocationDetails.CAR_CARD}/>,
        );
        expect(wrapper).toMatchSnapshot();
    });

    it('tags for client info', () => {
        const wrapper = render(
            <FastTagsView fastTags={dataClientInfo}
                          addTag={addTag}
                          place={buttonLocationDetails.CLIENT_INFO}/>,
        );
        expect(wrapper).toMatchSnapshot();
    });

    it('empty', () => {
        const wrapper = render(
            <FastTagsView fastTags={null}
                          addTag={addTag}
                          place={buttonLocationDetails.CAR_CARD}/>,
        );
        expect(wrapper).toMatchSnapshot();
    });
});
