import React from 'react';
import { mount } from 'enzyme';

import { PreviewStep } from './PreviewStep';
import { testFinalErrors, testFormData, testMessageErrorData } from './testData/testData';
import { StepPreviewProps } from './StepPreview/StepPreview';

jest.mock('./StepPreview/StepPreview', () => ({
    StepPreview: (props: StepPreviewProps) => (<div className={`mockStepPreview_${props.stepId}`} {...props} />),
}));

describe('PreviewStep', () => {
    const wrapper = mount(
        <PreviewStep
            className="additionalClassName"
            formData={testFormData}

            setFormData={jest.fn()}
            visitStep={jest.fn()}
            stepData={{}}

            errors={{}}
            stepStates={{}}
            availableSteps={[]}
            stepEntities={[]}

            finalLoading={false}
            finalErrors={testFinalErrors}

            visitedSteps={new Set([])}
            onStepChange={jest.fn()}
        />,
    );

    it('Should check prop formData', () => {
        expect(wrapper.find('.mockStepPreview_general').prop('formData')).toEqual(testFormData);
        expect(wrapper.find('.mockStepPreview_description').prop('formData')).toEqual(testFormData);
    });

    it('Should pass formatted error to Message.Error component', () => {
        const dataProp = wrapper.find('.PreviewStep-Message .Message-Body').hostNodes().childAt(0).prop('data');
        expect(dataProp).toEqual(testMessageErrorData);
    });
});
