import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { ChoiceParentServices } from './Choice_type_parentServices';
import { ServiceState } from '../../../Service/Service';

describe('Choice_type_parentServices', () => {
    const name = { ru: 'тестовое название', en: 'test name' };
    const status: ServiceState = 'develop';

    const props = { id: 12, name, status, disabled: false };

    it('Should render simple component', () => {
        const wrapper = mount(
            <>
                {ChoiceParentServices(props)}
            </>,
        );

        expect(wrapper.find('.Service.Choice_type_ParentServices')).toHaveLength(1);

        expect(wrapper.find('.Choice_disabled')).toHaveLength(0);
    });

    it('Should render disabled component', () => {
        const wrapper = mount(
            <>
                {ChoiceParentServices({ ...props, disabled: true })}
            </>,
        );

        expect(wrapper.find('.Service.Choice_disabled')).toHaveLength(1);
    });
});
