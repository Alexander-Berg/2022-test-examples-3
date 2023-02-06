import { mount } from 'enzyme';
import * as React from 'react';

import ButtonWithChoicesPopup from './index';

describe('ButtonWithChoicesPopup', () => {
    it('should render properly', function () {
        const wrapper = mount(
            <ButtonWithChoicesPopup id={'id-1'}
                                    options={[
                                        { label: 'test', value: '1', isDefault: true },
                                        { label: 'test2', value: '2' },
                                    ]}
                                    onSelect={jest.fn()}/>,
        );

        expect(wrapper).toMatchSnapshot();

        wrapper.find('button').simulate('click');
        expect(wrapper).toMatchSnapshot();
    });

    it('should save choice after reopen', function () {
        const wrapper = mount(
            <ButtonWithChoicesPopup id={'id-1'}
                                    options={[
                                        { label: 'test', value: '1', isDefault: true },
                                        { label: 'test2', value: '2' },
                                    ]}
                                    onSelect={jest.fn()}/>,
        );

        wrapper.find('button').simulate('click');
        wrapper.find('input[value="2"]').simulate('change', { target: { value: '2' } });
        wrapper.find('button').simulate('click');
        wrapper.find('button').simulate('click');
        expect(wrapper.find('input[value="2"]').props().checked).toEqual(true);
    });
});
