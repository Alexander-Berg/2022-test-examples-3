import React from 'react';
import { mount, shallow } from 'enzyme';
import { StepPreview } from './StepPreview';
import { FieldProps } from '../Field/Field';
import { ServiceProps } from '~/src/common/components/Service/Service';
import { ITag } from '~/src/common/components/Tag/Tag';
import { testFormData } from '../testData/testData';

jest.mock('@yandex-int/tools-components/WikiFormatter', () => ({
    WikiFormatter: () => <div className="mockWikiFormatter" />,
}));

jest.mock('../Field/Field', () => ({
    Field: ({ fieldId, handledValue }: FieldProps) => (<div className={`mockField_${fieldId}`}>{handledValue}</div>),
}));

jest.mock('../../../../../common/components/Service/Service', () => ({
    Service: (props: ServiceProps) => (<div className="mockService" {...props} />),
}));

jest.mock('../../../../../common/components/Tag/Tag', () => ({
    Tag: (props: ITag) => (<div className={`mockTag_${props.name}`} {...props} />),
}));

describe('StepPreview', () => {
    it('Should check step label', () => {
        const wrapper = shallow(
            <StepPreview
                formData={testFormData}
                stepId="general"
            />,
        );

        expect(wrapper.find('.PreviewStep-StepLabel').prop('children')).toEqual('i18n:general-step');
    });

    describe('general step', () => {
        const wrapper = mount(
            <StepPreview
                formData={testFormData}
                stepId="general"
            />,
        );

        it('Should render correct text for name field', () => {
            expect(wrapper.find('.mockField_name').text()).toBe(testFormData.general.name);
        });

        it('Should render correct text for englishName field', () => {
            expect(wrapper.find('.mockField_englishName').text()).toBe(testFormData.general.englishName);
        });

        it('Should render correct text for slug field', () => {
            expect(wrapper.find('.mockField_slug').text()).toBe(testFormData.general.slug);
        });

        it('Should render correct text for owner field', () => {
            expect(wrapper.find('.mockField_owner').text()).toBe(testFormData.general.owner?.title);
        });

        it('Should render Service component with correct props for parent field', () => {
            const mockServiceWrapper = wrapper.find('.mockField_parent').find('.mockService');

            expect(mockServiceWrapper.prop('name')).toEqual(testFormData.general.parent?.name);
            expect(mockServiceWrapper.prop('status')).toEqual(testFormData.general.parent?.status);
        });

        it('Should render Tag component with correct props for tags field', () => {
            const tagsWrapper = wrapper.find('.mockField_tags');
            const testTags = testFormData.general.tags;

            expect(tagsWrapper.find(`.mockTag_${testTags[0].name}`).prop('color')).toEqual(testTags[0].color);
            expect(tagsWrapper.find(`.mockTag_${testTags[1].name}`).prop('color')).toEqual(testTags[1].color);
        });
    });
});
