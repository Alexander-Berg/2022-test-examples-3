import React from 'react';
import { shallow } from 'enzyme';

import { Preset } from '../../../../../common/components/Preset/Preset';
import { testFormData } from '../../PreviewStep/testData/testData';
import { FieldWithTextinput } from '../FieldWithTextinput/FieldWithTextinput';
import { Slug } from './Slug';

describe('Slug', () => {
    it('Should render component', () => {
        const handleChange = jest.fn();
        const value = testFormData.general.slug;
        const suitableSlug = { text: 'slug', val: 45 };

        const wrapper = shallow(
            <Slug
                value={value}
                onChange={handleChange}
                suitableSlug={suitableSlug}
            />,
        );

        const fieldWithTextinput = wrapper.find(FieldWithTextinput);
        expect(fieldWithTextinput.prop('fieldName')).toEqual('slug');
        expect(fieldWithTextinput.prop('value')).toEqual(value);
        expect(fieldWithTextinput.prop('onChange')).toEqual(handleChange);

        const example = wrapper.find('.MainStep-Example');
        expect(example).toHaveLength(1);

        const preset = wrapper.find(Preset);
        expect(preset.prop('items')).toEqual([suitableSlug]);
    });

    it('Should not render component without suitable slug', () => {
        const handleChange = jest.fn();

        const wrapper = shallow(
            <Slug
                value={testFormData.general.slug}
                onChange={handleChange}
            />,
        );

        const example = wrapper.find('.MainStep-Example');
        expect(example).toHaveLength(0);

        const preset = wrapper.find(Preset);
        expect(preset.length).toEqual(0);
    });

    it('Should call slug onChange when we select suitable slug', () => {
        const handleChange = jest.fn();

        const newSlug = 'slug2';

        const wrapper = shallow(
            <Slug
                suitableSlug={{ text: 'slug1', val: 'slug1' }}
                value={testFormData.general.slug}
                onChange={handleChange}
            />,
        );

        const preset = wrapper.find(Preset);
        const onSuitableSlugSelect: (val: string) => void = preset.prop('onSelect');

        onSuitableSlugSelect(newSlug);

        expect(handleChange).toHaveBeenCalledWith({ slug: newSlug });
    });
});
