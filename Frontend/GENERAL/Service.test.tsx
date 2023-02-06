import React from 'react'; // eslint-disable-line no-unused-vars, @typescript-eslint/no-unused-vars
import { mount } from 'enzyme';
import { Service, ServiceState } from './Service';
import { IMultiLangString } from '../../types/IMultiLangString';

const { BEM_LANG } = process.env;

describe('Service', () => {
    const name = { ru: 'тестовое название', en: 'test name' };
    const status: ServiceState = 'develop';

    const props = { id: 12, name, status };

    it('Should render simple component', () => {
        const wrapper = mount(
            <Service {...props} />,
        );

        expect(wrapper.find('.Service').text()).toEqual(name?.[BEM_LANG as keyof IMultiLangString]);
        expect(wrapper.find(`.ServiceStateIcon_type_${status}`)).toHaveLength(1);
    });

    it('Should render component with className', () => {
        const wrapper = mount(
            <Service {...{ ...props, className: 'AdditionalClassname' }} />,
        );

        expect(wrapper.find('.Service').text()).toEqual(name?.[BEM_LANG as keyof IMultiLangString]);
        expect(wrapper.find(`.ServiceStateIcon_type_${status}`)).toHaveLength(1);
    });
});
