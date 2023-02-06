import { mount } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import { AnyAction, Store } from 'redux';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import { LSSettingItems } from '../../../types';
import { initRoles, initRules, initState } from '../../reducers/adminUserReducer';
import { initStateMock, LocalizationReducerState } from '../../reducers/localizationReducer';
import { requestRolesData, requestSettingsData } from '../../reducers/tests/adminUSerReducer.mock';
import LS from '../../utils/localStorage/localStorage';
import { comReducers } from '../App/store';
import LeftMenu from './index';
import style from './index.css';

const emptyLangState: LocalizationReducerState = {
    currentLang: new LS().get(LSSettingItems.lang),
    localizations: null,
};

const buildMockStore = (emptyLang :LocalizationReducerState | null = null): Store<any, AnyAction> => {
    const mockStore = configureMockStore([thunk]);
    const globalState: any = {
        AdminUser: initState,
        Lang: emptyLang || initStateMock,
    };

    let state = comReducers(globalState, initRoles(requestRolesData, null));
    state = comReducers(state, initRules(requestSettingsData, null));

    return mockStore(state);
};

describe('LeftMenu', () => {

    it('Should render with active=false', () => {
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={false}
                          toggleSideBar={jest.fn()}/>
            </Provider>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Should render with active=true', () => {
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={true}
                          toggleSideBar={jest.fn()}/>
            </Provider>,
        );

        expect(component.mount()).toMatchSnapshot();
    });

    it('Should render with empty localization object', () => {
        const component = mount(
            <Provider store={buildMockStore(emptyLangState)}>
                <LeftMenu active={true}
                          toggleSideBar={jest.fn()}/>
            </Provider>,
        );

        expect(component.mount()).toMatchSnapshot();
    });

    it('Should render with empty active props', () => {
        let emptyActive: any;
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={emptyActive}
                          toggleSideBar={jest.fn()}/>
            </Provider>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Should render with empty toggleSideBar props', () => {
        let emptyToggle: any;
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={false}
                          toggleSideBar={emptyToggle}/>
            </Provider>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Should render with non valid toggleSideBar props', () => {
        const emptyToggle: any = 'string';
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={false}
                          toggleSideBar={emptyToggle}/>
            </Provider>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Should render with non valid toggleSideBar props', () => {
        const emptyToggle: any = 'string';
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={false}
                          toggleSideBar={emptyToggle}/>
            </Provider>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Should close menu after clicking on Link', () => {
        const mockCallBack = jest.fn();

        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={true}
                          toggleSideBar={mockCallBack}/>
            </Provider>,
        );

        expect(mockCallBack).toHaveBeenCalledTimes(0);
        component.find(`.${style['link']}`).at(0).simulate('click');
        expect(mockCallBack).toHaveBeenCalledTimes(1);
    });

    it('Shouldn\'t call e.preventDefault on Link click', () => {
        const mockCallBack = jest.fn();

        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={true}
                          toggleSideBar={mockCallBack}/>
            </Provider>,
        );

        const mEvent = { preventDefault: jest.fn() };
        component.find(`.${style['link']}`).at(0).simulate('click', mEvent);
        expect(mEvent.preventDefault).not.toBeCalled();
    });

    it('Should filter', () => {
        const mockCallBack = jest.fn();
        const component = mount(
            <Provider store={buildMockStore()}>
                <LeftMenu active={true}
                          toggleSideBar={mockCallBack}/>
            </Provider>,
        );

        const input = component.find('input');
        const valueHome = { target: { value: 'Home' } };
        input.simulate('change', valueHome);

        expect(component.find(`.${style['link']}`).length).toEqual(1);
    });
});
