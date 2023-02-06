/**
 * @jest-environment jsdom
 */

import React from 'react';
import {mount} from 'enzyme';

import Paginator from '..';

const mountWithProps = props => mount(<Paginator {...props} />);
const changeHandler = jest.fn();

describe('Paginator', () => {
    beforeEach(() => {
        changeHandler.mockClear();
    });

    it('increments page', () => {
        const wrapper = mountWithProps({
            limit: 10,
            offset: 0,
            hasMore: true,
            onChange: changeHandler,
        });

        const nextButton = wrapper.find('button[title="Следующая"]');

        expect(nextButton).toHaveLength(1);
        nextButton.simulate('click');

        expect(changeHandler).toBeCalledTimes(1);
        expect(changeHandler).toBeCalledWith(10);
    });

    it('decrements page', () => {
        const wrapper = mountWithProps({
            limit: 10,
            offset: 50,
            hasMore: true,
            onChange: changeHandler,
        });

        const prevButton = wrapper.find('button[title="Предыдущая"]');

        expect(prevButton).toHaveLength(1);
        prevButton.simulate('click');

        expect(changeHandler).toBeCalledTimes(1);
        expect(changeHandler).toBeCalledWith(40);
    });

    it('moves to the first page', () => {
        const wrapper = mountWithProps({
            limit: 10,
            offset: 30,
            hasMore: true,
            onChange: changeHandler,
        });

        const prevButton = wrapper.find('button[title="Первая"]');

        expect(prevButton).toHaveLength(1);
        prevButton.simulate('click');

        expect(changeHandler).toBeCalledTimes(1);
        expect(changeHandler).toBeCalledWith(0);
    });

    it('changes page by text field', () => {
        const wrapper = mountWithProps({
            limit: 10,
            offset: 30,
            hasMore: true,
            onChange: changeHandler,
        });

        const input = wrapper.find('input');

        expect(input.prop('value')).toBe('4');

        input.simulate('change', {target: {value: '22'}});
        input.simulate('blur');

        expect(changeHandler).toBeCalledTimes(1);
        expect(changeHandler).toBeCalledWith(210);
    });
});
