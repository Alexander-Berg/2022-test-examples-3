import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import ErrorComponent from '../../../../../src/components/error';
import Notes from '../../../../../src/components/notes';
import getStore from '../../../../../src/store';

jest.mock('../../../../../src/helpers/metrika', () => ({
    countError: jest.fn()
}));
import { countError } from '../../../../../src/helpers/metrika';

jest.mock('../../../../../src/components/note', () => () => null);
jest.mock('../../../../../src/components/notes-list', () => () => null);
jest.mock('../../../../../src/components/notes-slider', () => () => null);
jest.mock('@ps-int/ufo-rocks/lib/components/notifications', () => () => null);

const SERVER_INTERNAL_ERROR_CODE = 500;

const getState = (errorCode) => ({
    ua: { isSmartphone: false },
    notes: {
        notes: {},
        sliderResourceId: null
    },
    dialogs: {},
    environment: {
        experiments: {
            flags: {}
        }
    },
    errorCode: errorCode || null
});

const getComponent = (store) => (
    <Provider store={store}>
        <Notes />
    </Provider>
);

describe('components/error =>', () => {
    it('should mount when error code exists and call `countError`', () => {
        const wrapper = mount(getComponent(getStore(getState(SERVER_INTERNAL_ERROR_CODE))));

        expect(wrapper.find(ErrorComponent).exists()).toBe(true);
        expect(wrapper.render()).toMatchSnapshot();
        expect(countError).toBeCalled();
        expect(popFnCalls(countError)[0]).toEqual([SERVER_INTERNAL_ERROR_CODE]);
    });

    it('should not mount when error code does not exist', () => {
        const wrapper = mount(getComponent(getStore(getState())));

        expect(wrapper.find(ErrorComponent).exists()).toBe(false);
        expect(countError).not.toBeCalled();
    });
});
