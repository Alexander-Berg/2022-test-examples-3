import React, { FC } from 'react';
import { mount } from 'enzyme';
import { WizardContainer } from './Wizard.container';
import { IFormData, IStepEntity } from './Wizard.lib';

jest.mock('./Wizard', () => ({
    Wizard: () => <div className="mockWizard" />,
}));

const MockStep: FC<{ name: string }> = ({ name }) => (
    <div className="MockStep">
        {name}
    </div>
);

const baseStepEntityI = (i: number): IStepEntity<IFormData, {}> => ({
    id: `id${i}`,
    title: `Step${i}`,
    description: 'Base description',
    validate: () => Promise.resolve({ passed: true }),
    checkStepCondition: () => true,
    component: () => <MockStep name="base" />,
});

describe('WizardContainer', () => {
    it('Should set initial currentStepId to first visible step', () => {
        const setFormData = jest.fn();
        const steps = [
            {
                ...baseStepEntityI(1),
                checkStepCondition: () => false,
            },
            baseStepEntityI(2),
            baseStepEntityI(3),
            baseStepEntityI(4),
        ];

        const wrapper = mount(
            <WizardContainer
                stepEntities={steps}
                formData={{}}
                setFormData={setFormData}
            />,
        );

        expect(wrapper.childAt(0).prop('currentStepId')).toBe('id2');

        wrapper.unmount();
    });

    it('Should change currentStepId on prop change', () => {
        const setFormData = jest.fn();
        const steps = [
            baseStepEntityI(1),
            baseStepEntityI(2),
            baseStepEntityI(3),
            baseStepEntityI(4),
        ];

        const wrapper = mount(
            <WizardContainer
                stepEntities={steps}
                formData={{}}
                setFormData={setFormData}
            />,
        );

        expect(wrapper.childAt(0).prop('currentStepId')).toBe('id1');

        wrapper.setProps({ currentStepId: 'id3' });
        wrapper.update();

        expect(wrapper.childAt(0).prop('currentStepId')).toBe('id3');

        wrapper.unmount();
    });

    it('Should pass only visible steps in availableSteps', () => {
        const setFormData = jest.fn();
        const steps = [
            {
                ...baseStepEntityI(1),
                checkStepCondition: () => true,
            }, {
                ...baseStepEntityI(2),
                checkStepCondition: () => false,
            }, {
                ...baseStepEntityI(3),
                checkStepConditionAsync: () => Promise.resolve(true),
            }, {
                ...baseStepEntityI(4),
                checkStepConditionAsync: () => Promise.resolve(false),
            }, {
                ...baseStepEntityI(5),
                checkStepConditionAsync: () => Promise.reject(),
            },
        ];

        const wrapper = mount(
            <WizardContainer
                stepEntities={steps}
                formData={{}}
                setFormData={setFormData}
            />,
        );

        setTimeout(() => {
            expect(
                wrapper.childAt(0).prop('availableSteps').map((s: IStepEntity<IFormData, {}>) => s.id),
            ).toStrictEqual(['id1', 'id3']);

            wrapper.unmount();
        }, 100);
    });

    it('Should change availableSteps after formData change', () => {
        const setFormData = jest.fn();
        const steps = [
            baseStepEntityI(1),
            {
                ...baseStepEntityI(2),
                checkStepCondition: (formData: IFormData) => formData?.id1?.data === 'some-data',
            },
            baseStepEntityI(3),
        ];

        const wrapper = mount(
            <WizardContainer
                stepEntities={steps}
                formData={{}}
                setFormData={setFormData}
            />,
        );
        expect(wrapper.childAt(0).prop('availableSteps').map((i: IStepEntity<IFormData, {}>) => i.id))
            .toStrictEqual(['id1', 'id3']);

        const newFormData = { id1: { data: 'some-data' } };

        wrapper.setProps({ formData: newFormData });
        wrapper.update();

        expect(wrapper.childAt(0).prop('formData')).toStrictEqual(newFormData);
        expect(wrapper.childAt(0).prop('availableSteps').map((i: IStepEntity<IFormData, {}>) => i.id))
            .toStrictEqual(['id1', 'id2', 'id3']);

        wrapper.unmount();
    });

    it('Should mark step as visited', () => {
        const setFormData = jest.fn();
        const steps = [
            baseStepEntityI(1),
            baseStepEntityI(2),
            baseStepEntityI(3),
            baseStepEntityI(4),
        ];

        const wrapper = mount(
            <WizardContainer
                stepEntities={steps}
                formData={{}}
                setFormData={setFormData}
                currentStepId="id3"
                initialVisitedSteps={['id1']}
            />,
        );

        wrapper.childAt(0).prop('visitCurrentStep')();
        wrapper.update();

        expect(wrapper.childAt(0).prop('visitedSteps')).toStrictEqual(new Set(['id1', 'id3']));
        expect(wrapper.childAt(0).prop('isStepVisited')('id1')).toBeTruthy();
        expect(wrapper.childAt(0).prop('isStepVisited')('id3')).toBeTruthy();

        wrapper.unmount();
    });
});
