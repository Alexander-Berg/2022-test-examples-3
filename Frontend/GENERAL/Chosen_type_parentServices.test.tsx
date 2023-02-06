import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { ChosenParentServices } from './Chosen_type_parentServices';

describe('Chosen_type_parentServices', () => {
    it('Should render simple component', () => {
        const name = { ru: 'тестовое название', en: 'test name' };
        const status = 'develop';

        const props = { id: 12, name, status };

        const wrapper = mount(
            <>
                {ChosenParentServices(props)}
            </>,
        );

        expect(wrapper.find('.Service.Chosen_type_ParentServices')).toHaveLength(1);
    });
});
