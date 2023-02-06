import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { ChoiceTags } from './Choice_type_tags';

describe('Choice_type_tags', () => {
    it('Should render simple component', () => {
        const name = 'test name';

        const wrapper = mount(
            <>
                {ChoiceTags({ id: '12', name })}
            </>,
        );

        expect(wrapper.find('.Choice_type_Tags').text()).toEqual(name);
    });
});
