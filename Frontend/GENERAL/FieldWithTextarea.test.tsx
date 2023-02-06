import React from 'react';
import { shallow } from 'enzyme';

import kebabCase from 'lodash/kebabCase';
import { FieldWithTextarea } from './FieldWithTextarea';

describe('FieldWithTextinput', () => {
    const handleChange = jest.fn();
    const handleCommit = jest.fn();
    const fieldName = 'description';
    const fieldValue = 'testValue';

    const wrapper = shallow(
        <FieldWithTextarea
            fieldName={fieldName}
            value={fieldValue}
            onChange={handleChange}
            onBlur={handleCommit}
        />,
    );

    const textarea = wrapper.find('.DescriptionStep-Input_field_description');

    it('Should render component', () => {
        const labelText = wrapper.find('.DescriptionStep-Label_field_description').text();
        expect(labelText).toEqual(`i18n:${kebabCase(fieldName)}`);

        const textInputValue = textarea.prop('value');
        expect(textInputValue).toEqual(fieldValue);
    });

    it('Should call handleChange after input changing', () => {
        textarea.simulate('change', { target: { value: 'testValue2' } });

        expect(handleChange).toHaveBeenCalledWith({ description: 'testValue2' });
    });

    it('Should call onCommit after input blur', () => {
        textarea.simulate('blur');

        expect(handleCommit).toHaveBeenCalled();
    });

    it('Should pass description hint and undefined state to Textarea without error', () => {
        expect(textarea.prop('hint')).toEqual('i18n:description-wiki');
        expect(textarea.prop('state')).toEqual(undefined);
    });

    describe('With error', () => {
        const errorText = 'Bad error';

        const wrapper = shallow(
            <FieldWithTextarea
                fieldName={fieldName}
                value={fieldValue}
                onChange={handleChange}
                onBlur={handleCommit}

                error={errorText}
            />,
        );

        const textarea = wrapper.find('.DescriptionStep-Input_field_description');

        it('Should pass error state to Textarea with error', () => {
            expect(textarea.prop('state')).toEqual('error');
        });

        it('Should pass hint with error text to Textarea with error', () => {
            expect(textarea.prop('hint')).toEqual(errorText);
        });
    });
});
