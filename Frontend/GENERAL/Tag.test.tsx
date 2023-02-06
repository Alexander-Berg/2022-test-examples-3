import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { Tag } from './Tag';

describe('Chosen_type_tags', () => {
    const name = 'test name';
    const color = '#FFFFFF';

    const props = { id: '12', name, color };

    it('Should render simple component', () => {
        const wrapper = mount(
            <Tag {...props} />,
        );

        expect(wrapper.find('.Tag').text()).toEqual(name);
        expect(wrapper.find('.Tag').prop('style')).toEqual({ backgroundColor: color });
    });

    it('Should render component with className', () => {
        const wrapper = mount(
            <Tag {...props} className="AdditionalClassname" />,
        );

        expect(wrapper.find('.Tag.AdditionalClassname').text()).toEqual(name);
        expect(wrapper.find('.Tag.AdditionalClassname').prop('style')).toEqual({ backgroundColor: color });
    });
});
