import { shallow } from 'enzyme';
import React from 'react';

import Checkbox from './index';

describe('Checkbox', () => {
    it('should be checked when value is true', () => {
        const wrapper = shallow(<Checkbox checked={true}
                                          onChange={() => {
                                          }}/>);
        expect(wrapper.find('input').props().checked).toEqual(true);
    });

    it('should be disabled when value is true', () => {
        const wrapper = shallow(<Checkbox checked={false}
                                          onChange={() => {
                                          }}
                                          disabled={true}/>);
        expect(wrapper.find('input').props().disabled).toEqual(true);
    });
});
