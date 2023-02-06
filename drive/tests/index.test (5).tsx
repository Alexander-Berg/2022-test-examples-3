import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { Provider } from 'react-redux';
import { AnyAction, Store } from 'redux';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';

import { deepCopy } from '../../../../utils/utils';
import { _CHATS } from '../../types';
import { BaseMenu } from '../BaseMenu';
import { LinesList } from '../component';
import { ARCHIVE_MENU, LINES_LIST_DATA, MENU_DATA } from './MockedData';

const buildMockStore = (): Store<any, AnyAction> => {
    const mockStore = configureMockStore([thunk]);
    const globalState: any = {
        Chats: {
            my_chats_are_loading: false,
            all_chats_are_loading: false,
            new_chats_are_loading: false,
        },
    };

    return mockStore(globalState);
};

describe('Base Menu', () => {
    it('correct render', () => {
        const wrapper = shallow(<BaseMenu {...MENU_DATA} />);
        expect(wrapper).toMatchSnapshot();
    });

    it('check menu click', () => {
        const DATA_COPY = deepCopy(MENU_DATA);
        DATA_COPY.onMenuClick = jest.fn();
        const wrapper = shallow(<BaseMenu {...DATA_COPY} />);
        const MENU_ID = _CHATS.MY;
        const menu = wrapper.find(`#${MENU_ID}`);
        menu.simulate('click');
        expect(DATA_COPY.onMenuClick).toBeCalledWith(`${MENU_ID}`, null);
        expect(wrapper).toMatchSnapshot();
    });
});

describe('Lines list', () => {
    it('correct lines list render', () => {
        const wrapper = mount(<Provider store={buildMockStore()}>
            <LinesList {...LINES_LIST_DATA}/>
        </Provider>);
        expect(wrapper).toMatchSnapshot();
    });

    it('has additional menu', () => {
        const DATA_COPY = deepCopy(LINES_LIST_DATA);
        DATA_COPY.additionalMenus = [ARCHIVE_MENU];

        const wrapper = mount(<Provider store={buildMockStore()}>
            <LinesList {...DATA_COPY} />
        </Provider>);
        expect(wrapper).toMatchSnapshot();
    });

    it('click update btn', () => {
        const DATA_COPY = deepCopy(LINES_LIST_DATA);
        DATA_COPY.additionalControls[0].onClick = jest.fn();
        const wrapper = mount(<Provider store={buildMockStore()}>
            <LinesList {...DATA_COPY}/>
        </Provider>);
        const btn = wrapper.find('.update_button');
        btn.at(0).simulate('click');
        expect(DATA_COPY.additionalControls[0].onClick).toBeCalledTimes(1);
    });
});
