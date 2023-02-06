import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { ChoiceStaff } from './Choice_type_staff';

describe('Choice_type_staff', () => {
    it('Should render simple component', () => {
        const props = {
            id: 'testlogin',
            login: 'testlogin',
            title: 'Test Title',
            disabled: true,
        };
        const wrapper = mount(
            <>
                {ChoiceStaff(props)}
            </>,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
