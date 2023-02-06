import React from 'react';

import { mount } from 'enzyme';
import { Item } from '@yandex-int/tools-components/ToolsSuggest';
import { Owner } from './Owner';
import { testFormData } from '../../PreviewStep/testData/testData';
import { IMainStepData } from '../../../ServiceCreation.types';

interface PeopleSuggestProps {
    onChosenChange: ((chosen: Item[]) => void)
}

jest.mock('./PeopleSuggest', () => ({
    PeopleSuggest: ({ onChosenChange }: PeopleSuggestProps) => {
        const handleChange = ({ target: { value } }: React.ChangeEvent<HTMLInputElement>) => {
            onChosenChange([{ id: value }]);
        };

        return (
            <input className="mockPeopleSuggest" onChange={handleChange} />
        );
    },
}));

describe('Owner', () => {
    const handleChange = jest.fn();
    const stepData: IMainStepData = testFormData.general;

    it('Should render component', () => {
        const wrapper = mount(
            <Owner
                value={stepData.owner}
                onChange={handleChange}
            />,
        );

        const textinput = wrapper.find('.MainStep-Input_field_owner');

        const labelText = wrapper.find('.FieldLabel_field_owner .FieldLabel-Label').text();
        expect(labelText).toEqual('i18n:owner');

        const textInputValue = textinput.prop('chosen');
        expect(textInputValue).toEqual([testFormData.general.owner]);

        const textinputPlaceholder = textinput.prop('placeholder');
        expect(textinputPlaceholder).toEqual('i18n:owner-placeholder');

        wrapper.unmount();
    });

    it('Schould check handleChange', () => {
        const wrapper = mount(
            <Owner
                value={stepData.owner}
                onChange={handleChange}
            />,
        );

        wrapper.find('.mockPeopleSuggest').simulate('change', { target: { value: 'testUser2' } });

        expect(handleChange).toHaveBeenCalledWith({ owner: { id: 'testUser2' } });
        wrapper.unmount();
    });
});
