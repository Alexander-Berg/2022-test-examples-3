import React from 'react';

import { shallow, mount } from 'enzyme';
import { Item } from '@yandex-int/tools-components/ToolsSuggest';
import { Parent } from './Parent';
import { testFormData } from '../../PreviewStep/testData/testData';
import { ParentServicesSuggest } from './ParentServicesSuggest';
import { Preset } from '../../../../../common/components/Preset/Preset';

interface PeopleSuggestProps {
    onChosenChange: ((chosen: Item[]) => void)
}

jest.mock('./ParentServicesSuggest', () => ({
    ParentServicesSuggest: ({ onChosenChange }: PeopleSuggestProps) => {
        const handleChange = ({ target: { value } }: React.ChangeEvent<HTMLTextAreaElement>) => {
            onChosenChange([{ id: value }]);
        };

        return (
            <textarea className="mockParentServicesSuggest" onChange={handleChange} />
        );
    },
}));

const suitableParents = [{ text: 'text', val: 'val' }];
const externalParentId = '383';

describe('Parent', () => {
    const onChange = jest.fn();
    const setExternalParentId = jest.fn();
    const value = testFormData.general.parent;

    const props = {
        value,
        onChange,
        suitableParents,
        externalParentId,
        setExternalParentId,
    };

    it('Should render component', () => {
        const wrapper = mount(
            <Parent
                {...props}
            />,
        );

        const textarea = wrapper.find('.MainStep-Input_field_parent');

        const labelText = wrapper.find('.FieldLabel_field_parent .FieldLabel-Label').text();
        expect(labelText).toEqual('i18n:parent');

        const textInputValue = textarea.prop('chosen');
        expect(textInputValue).toEqual([testFormData.general.parent]);

        const textinputPlaceholder = textarea.prop('placeholder');
        expect(textinputPlaceholder).toEqual('i18n:parent-placeholder');

        wrapper.unmount();
    });

    it('Should check handleChange', () => {
        const wrapper = mount(
            <Parent
                {...props}
            />,
        );

        wrapper.find('.mockParentServicesSuggest').simulate('change', { target: { value: 'testService' } });

        expect(onChange).toHaveBeenCalledWith({ parent: { id: 'testService' } });
        wrapper.unmount();
    });

    describe('Children prop', () => {
        const wrapper = shallow(
            <Parent
                {...props}
            />,
        );

        it('Should check ParentServicesSuggest prop', () => {
            const parentSuggest = wrapper.find(ParentServicesSuggest);
            expect(parentSuggest.prop('selectionIds')).toEqual([externalParentId]);
        });

        it('Should call setExternalParentId', () => {
            const preset = wrapper.find(Preset);

            const onSelectProp: (val: number) => void = preset.prop('onSelect');
            onSelectProp(123);

            expect(setExternalParentId).toHaveBeenCalledWith('123');
        });
    });
});
