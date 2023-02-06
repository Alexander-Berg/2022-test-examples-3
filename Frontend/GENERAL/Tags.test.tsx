import React from 'react';

import { mount } from 'enzyme';
import { Item } from '@yandex-int/tools-components/ToolsSuggest';
import { Tags } from './Tags';
import { IMainStepData } from '../../../ServiceCreation.types';

interface TagsSuggestProps {
    onChosenChange: ((chosen: Item[]) => void)
}

const initialTags = [{ id: 11, name: 'testTag11', color: '#FFFFFF' }];

jest.mock('./TagsSuggest', () => ({
    TagsSuggest: ({ onChosenChange }: TagsSuggestProps) => {
        const handleChange = ({ target: { value } }: React.ChangeEvent<HTMLInputElement>) => {
            const newTags = [
                ...initialTags,
                { id: value, name: `testTag${value}`, color: '#FFFFFF' },
            ];

            onChosenChange(newTags);
        };

        return (
            <input className="mockTagsSuggest" onChange={handleChange} />
        );
    },
}));

describe('Tags', () => {
    const handleChange = jest.fn();
    const stepData: IMainStepData = {
        name: '',
        englishName: '',
        slug: '',
        tags: [{ id: 11, name: 'AAA', color: '#FFFFFF' }],
    };

    it('Should render component', () => {
        const wrapper = mount(
            <Tags
                value={stepData.tags}
                onChange={handleChange}
            />,
        );

        const textinput = wrapper.find('.MainStep-Input_field_tags');

        const labelText = wrapper.find('.FieldLabel_field_tag .FieldLabel-Label').text();
        expect(labelText).toEqual('i18n:tags');

        const textInputValue = textinput.prop('chosen');
        expect(textInputValue).toEqual([{ id: 11, name: 'AAA', color: '#FFFFFF' }]);

        const textinputPlaceholder = textinput.prop('placeholder');
        expect(textinputPlaceholder).toEqual('i18n:tags-placeholder');

        wrapper.unmount();
    });

    it('Should check handleChange', () => {
        const wrapper = mount(
            <Tags
                value={stepData.tags}
                onChange={handleChange}
            />,
        );

        wrapper.find('.mockTagsSuggest').simulate('change', { target: { value: '12' } });

        expect(handleChange).toHaveBeenCalledWith({ tags: [...initialTags, { id: '12', name: 'testTag12', color: '#FFFFFF' }] });

        wrapper.unmount();
    });
});
