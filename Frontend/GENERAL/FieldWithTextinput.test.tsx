import React from 'react';

import { mount } from 'enzyme';
import kebabCase from 'lodash/kebabCase';
import { FieldWithTextinput } from './FieldWithTextinput';

describe('FieldWithTextinput', () => {
    const handleChange = jest.fn();
    const handleCommit = jest.fn();
    const fieldName = 'name';
    const fieldValue = 'testValue';

    const wrapper = mount(
        <FieldWithTextinput
            fieldName={fieldName}
            value={fieldValue}
            onChange={handleChange}
            onBlur={handleCommit}
        />,
    );

    afterAll(() => {
        wrapper.unmount();
    });

    const textinput = wrapper.find('.MainStep-Input_textinput.MainStep-Input_field_name input');

    it('Should render component', () => {
        const labelText = wrapper.find('.FieldLabel_field_name .FieldLabel-Label').text();
        expect(labelText).toEqual(`i18n:${kebabCase(fieldName)}`);

        const textInputValue = textinput.prop('value');
        expect(textInputValue).toEqual(fieldValue);

        const textinputPlaceholder = textinput.prop('placeholder');
        expect(textinputPlaceholder).toEqual(`i18n:${kebabCase(fieldName)}-placeholder`);
    });

    it('Should call onChange and onCommit', () => {
        textinput.simulate('change', { target: { value: 'testValue2' } });
        textinput.simulate('blur');

        expect(handleChange).toHaveBeenCalledWith({ name: 'testValue2' });
        expect(handleCommit).toHaveBeenCalled();
    });
});
