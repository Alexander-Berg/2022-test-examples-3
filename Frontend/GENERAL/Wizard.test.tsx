import React, { FC } from 'react';
import { mount } from 'enzyme';
import { Wizard } from './Wizard';
import { AvailableStepEntity, IFormData, IStepEntity, StepState } from './Wizard.lib';

jest.mock('./Menu/Wizard-Menu', () => ({
    Menu: () => <div className="mockWizardMenu" />,
}));

jest.mock('./Footer/Wizard-Footer', () => ({
    Footer: () => <div className="mockWizardFooter" />,
}));

const MockStep: FC<{ name: string }> = ({ name }) => (
    <div className="MockStep">
        {name}
    </div>
);

const baseStepEntity: IStepEntity<IFormData, {}> = {
    id: 'base',
    title: 'Base',
    description: 'Base description',
    validate: () => Promise.resolve({ passed: true }),
    checkStepCondition: () => true,
    component: () => <MockStep name="base" />,
};

const baseSteps = [1, 2, 3, 4].map(i => ({
    ...baseStepEntity,
    id: `id${i}`,
    title: `Step${i}`,
}));

const availableStepEntities: AvailableStepEntity<{}, {}>[] = [
    { ...baseSteps[1], order: 3 },
    { ...baseSteps[3], order: 8 },
];

const EmptyStep:FC = () => <div className="EmptyStep" />;

describe('Wizard', () => {
    const baseProps = {
        currentStepId: baseSteps[1].id,
        currentStep: baseSteps[1],
        currentStepData: {},
        availableSteps: availableStepEntities,
        errors: {},
        stepStates: {},
        onMenuItemChange: jest.fn(),
        onNext: jest.fn(),
        onSubmit: jest.fn(),
        isStepVisited: jest.fn(),
        visitCurrentStep: jest.fn(),
        isCurrentStepTheLast: false,
        isCurrentStepTheFirst: false,
        visitedSteps: new Set<string>(),
        onStepChange: jest.fn(),
        emptyStepComponent: EmptyStep,
        stepEntities: baseSteps,
        formData: {},
        setFormData: jest.fn(),
        finalLoading: false,
        finalErrors: {},
        disableUnvisitedSteps: false,
        noCancelButton: false,
        noBackButton: false,
    };

    it('Should draw custom empty step component without current step', () => {
        const wrapper = mount(
            <Wizard
                {...baseProps}
                currentStepId={'non-existingId'}
                currentStep={undefined}
                availableSteps={[]}
            />,
        );

        expect(wrapper.find('.Wizard-Form').length).toBe(0);
        expect(wrapper.find('.EmptyStep').length).toBe(1);
        expect(wrapper.find('.Wizard-Content')).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should draw custom footer component', () => {
        const wrapper = mount(
            <Wizard
                {...baseProps}
                footerComponent={() => <div className="customFooter">Custom Footer</div>}
            />,
        );

        expect(wrapper.find('.customFooter').length).toBe(1);
        expect(wrapper.find('.customFooter')).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should draw custom step component', () => {
        const wrapper = mount(
            <Wizard
                {...baseProps}
            />,
        );

        expect(wrapper.find('.Wizard-StepHeader h3').contains('Step2')).toBeTruthy();
        expect(wrapper.find('.MockStep').length).toBe(1);
        expect(wrapper.find('.Wizard-Content')).toMatchSnapshot();

        wrapper.unmount();
    });

    it('Should call onNext if current step is not the last', () => {
        const onNext = jest.fn();
        const onSubmit = jest.fn();

        const wrapper = mount(
            <Wizard
                {...baseProps}
                onNext={onNext}
                onSubmit={onSubmit}
            />,
        );

        wrapper.find('.Wizard-Form').simulate('submit');

        expect(onNext).toBeCalledTimes(1);
        expect(onSubmit).not.toBeCalled();

        wrapper.unmount();
    });

    it('Should call onSubmit if current step is the last', () => {
        const onNext = jest.fn();
        const onSubmit = jest.fn();

        const wrapper = mount(
            <Wizard
                {...baseProps}
                isCurrentStepTheLast
                onNext={onNext}
                onSubmit={onSubmit}
            />,
        );

        wrapper.find('.Wizard-Form').simulate('submit');

        expect(onSubmit).toBeCalledTimes(1);
        expect(onNext).not.toBeCalled();

        wrapper.unmount();
    });

    it('Should pass props to child components', () => {
        const stepStates = {
            id1: StepState.passed,
            id2: StepState.loading,
            id3: StepState.error,
            id4: undefined,
        };
        const wrapper = mount(
            <Wizard
                {...baseProps}
                stepStates={stepStates}
            />,
        );

        const expectedAvailableSteps = [
            {
                checkStepCondition: baseStepEntity.checkStepCondition,
                component: baseStepEntity.component,
                description: 'Base description',
                id: 'id2',
                order: 3,
                title: 'Step2',
                validate: baseStepEntity.validate,
            }, {
                checkStepCondition: baseStepEntity.checkStepCondition,
                component: baseStepEntity.component,
                description: 'Base description',
                id: 'id4',
                order: 8,
                title: 'Step4',
                validate: baseStepEntity.validate,
            },
        ];

        const expectedMenuProps = {
            currentStepId: 'id2',
            currentSteps: expectedAvailableSteps,
            disableUnvisitedSteps: false,
            isStepVisited: baseProps.isStepVisited,
            onMenuItemChange: baseProps.onMenuItemChange,
            stepStates,
        };
        const expectedStepProps = {
            availableSteps: expectedAvailableSteps,
            className: 'Wizard-CurrentStep',
            errors: {},
            finalErrors: {},
            finalLoading: false,
            finalValidation: undefined,
            formData: {},
            onStepChange: baseProps.onStepChange,
            setFormData: baseProps.setFormData,
            stepData: {},
            stepEntities: [1, 2, 3, 4].map(i => ({
                checkStepCondition: baseStepEntity.checkStepCondition,
                component: baseStepEntity.component,
                description: 'Base description',
                id: `id${i}`,
                title: `Step${i}`,
                validate: baseStepEntity.validate,
            })),
            stepStates,
            visitStep: baseProps.visitCurrentStep,
            visitedSteps: new Set(),
        };
        const expectedFooterProps = {
            backButtonText: undefined,
            cancelButtonText: undefined,
            currentStepState: StepState.loading,
            finalLoading: false,
            isCurrentStepTheLast: false,
            isCurrentStepTheFirst: false,
            nextButtonText: undefined,
            noBackButton: false,
            noCancelButton: false,
            onBack: undefined,
            onCancel: undefined,
            onNext: baseProps.onNext,
            onSubmit: baseProps.onSubmit,
            submitButtonText: undefined,
        };

        const menuProps = wrapper.find('.Wizard-Aside').childAt(0).props();
        const stepProps = wrapper.find('.Wizard-Form').childAt(1).props();
        const footerProps = wrapper.find('.Wizard-Form').childAt(2).props();

        expect(menuProps).toStrictEqual(expectedMenuProps);
        expect(stepProps).toStrictEqual(expectedStepProps);
        expect(footerProps).toStrictEqual(expectedFooterProps);

        wrapper.unmount();
    });
});
