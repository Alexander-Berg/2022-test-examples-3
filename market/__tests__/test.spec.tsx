/**
 * @jest-environment jsdom
 */

import React from 'react';
import {mount, shallow} from 'enzyme';

import SearchInput from '..';

describe('SearchInput', () => {
    it('render correctly', () => {
        const wrapped = () => shallow(<SearchInput onClick={() => {}} />);

        expect(wrapped).not.toThrowError();
    });

    it('click without error', () => {
        const mockFn = jest.fn();
        const wrapped = mount(<SearchInput onClick={mockFn} />);

        wrapped.find('button').simulate('click');
        expect(mockFn).toBeCalled();
    });

    it('call onclick after enter keypress', () => {
        const mockFn = jest.fn();
        const wrapped = mount(<SearchInput onClick={mockFn} />);

        wrapped.find('input').simulate('keyup', {keyCode: 13});
        expect(mockFn).toBeCalled();
    });

    it('change input value', () => {
        const mockFn = jest.fn(x => x);
        const wrapped = mount(<SearchInput defaultValue="hello" onClick={mockFn} />);

        wrapped
            .find('button')
            .last()
            .simulate('click');

        expect(mockFn.mock.results[0].value).toEqual('hello');
    });
});
