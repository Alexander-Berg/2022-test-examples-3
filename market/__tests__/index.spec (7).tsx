/**
 * @jest-environment jsdom
 */

import React from 'react';
import {shallow, mount} from 'enzyme';
import {act} from 'react-dom/test-utils';

import Autocomplete from '..';

describe('Autocomplete', () => {
    it('just render', () => {
        const render = () => shallow(<Autocomplete getItems={() => Promise.resolve([])} />);

        expect(render).not.toThrowError();
    });

    it('uncontrolled usage', () => {
        const wrapper = mount(<Autocomplete value="Test" getItems={() => Promise.resolve(['Test'])} />);

        wrapper.setProps({value: 'TestTest'});

        wrapper.update();

        expect(wrapper.find('input').prop('value')).toEqual('TestTest');
    });

    it('should call getItems with correctly argument', () => {
        const waitForComponentToPaint = async wrapper => {
            await act(async () => {
                await new Promise(resolve => setTimeout(resolve, 0));
                await wrapper.update();
            });
        };

        const changeHandler = jest.fn();
        const getItems = jest.fn(() => Promise.resolve([{label: 'Test'}]));

        const wrapper = mount(
            <Autocomplete itemToValue={item => item.label} getItems={getItems} onSelect={changeHandler} />
        );

        wrapper.find('input').simulate('focus');

        wrapper.find('input').simulate('change', {
            target: {
                value: 'Te',
            },
        });

        waitForComponentToPaint(wrapper);

        expect(getItems).toHaveBeenCalledWith('Te');
    });
});
