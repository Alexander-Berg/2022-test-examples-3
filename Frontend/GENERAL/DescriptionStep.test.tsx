import React from 'react';

import { shallow } from 'enzyme';
import { DescriptionStep, DescriptionStepChangeHandler } from './DescriptionStep';
import { WizardFormData } from '../WizardForm/WizardForm.container';
import { IDescriptionStepData, IMainStepData } from '../../ServiceCreation.types';

const stepData: IDescriptionStepData = {
    description: 'testDescription',
    englishDescription: 'testEnglishDescription',
};

const mainStepData: IMainStepData = {
    name: 'testName',
    englishName: 'testEnglishName',
    slug: 'testSlug',
    tags: [],
};

describe('DescriptionStep', () => {
    const handleStepChange = jest.fn();
    const visitStep = jest.fn();
    const formData: WizardFormData = {
        description: stepData,
        general: mainStepData,
    };

    const setFormData = jest.fn(
        fn => {
            return fn(formData);
        },
    );

    const wrapper = shallow(
        <DescriptionStep
            formData={formData}
            setFormData={setFormData}
            visitStep={visitStep}
            stepData={stepData}

            errors={{}}
            stepStates={{}}
            availableSteps={[]}
            stepEntities={[]}

            finalLoading={false}
            finalErrors={{}}

            visitedSteps={new Set([])}
            onStepChange={handleStepChange}
        />,
    );

    const description = wrapper.find('.DescriptionStep-Description');
    const englishDescription = wrapper.find('.DescriptionStep-EnglishDescription');

    const fields = [{ field: description, fieldName: 'description' }, { field: englishDescription, fieldName: 'englishDescription' }];

    it('Should check fields presence', () => {
        fields.forEach(({ field }) => expect(field));
    });

    describe('Should check value prop', () => {
        fields.forEach(({ field, fieldName }) => {
            it(`field ${fieldName}`, () => {
                expect(field.prop('value')).toEqual(stepData[fieldName]);
            });
        });
    });

    describe('Should check fieldName prop in textinput fields', () => {
        fields.forEach(({ field, fieldName }) => {
            it(`field ${fieldName}`, () => {
                expect(field.prop('fieldName')).toEqual(fieldName);
            });
        });
    });

    describe('Should check onChange prop', () => {
        fields.forEach(({ field, fieldName }) => {
            it(`field ${fieldName}`, () => {
                const diff = { [fieldName]: `test${fieldName}` };

                const onChange = field.prop<DescriptionStepChangeHandler>('onChange');
                onChange(diff);

                expect(visitStep).toHaveBeenCalled();

                expect(setFormData).toHaveBeenCalled();
                expect(setFormData).toHaveLastReturnedWith(({
                    ...formData,
                    description: { ...formData.description, ...diff },
                }));
            });
        });
    });
});
