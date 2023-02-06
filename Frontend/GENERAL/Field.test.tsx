import React from 'react';
import { shallow } from 'enzyme';

import { Field } from './Field';

describe('Field', () => {
    const handledValue = 'testHandledValue';

    const wrapper = shallow(
        <Field fieldId="englishDescription" handledValue={handledValue} />,
    );

    it('Should render field label', () => {
        expect(wrapper.find('.PreviewStep-Label_type_field')).toHaveLength(1);
        expect(wrapper.find('.PreviewStep-Label_type_field').prop('children')).toEqual('i18n:english-description');
    });

    it('Should render handled value', () => {
        expect(wrapper.find('.PreviewStep-Value_type_field')).toHaveLength(1);
        expect(wrapper.find('.PreviewStep-Value_type_field').prop('children')).toEqual(handledValue);
    });
});
