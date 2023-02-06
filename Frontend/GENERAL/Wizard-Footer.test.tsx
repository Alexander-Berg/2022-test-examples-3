import React from 'react';
import { render, shallow } from 'enzyme';

import { Footer } from './Wizard-Footer';
import { StepState } from '../Wizard.lib';

const defaultProps = {
    currentStepState: undefined,
    isCurrentStepTheLast: false,
    isCurrentStepTheFirst: false,

    onNext: jest.fn(),
    onSubmit: jest.fn(),

    finalLoading: false,

    noCancelButton: false,
    noBackButton: false,
};

const getWizardButton = (wrapper: Cheerio, mode: string, addition = '') => wrapper.find(`.Wizard-FooterButton_${mode}${addition}`).length;
const getWizardButtonText = (wrapper: Cheerio, mode: string) => wrapper.find(`.Wizard-FooterButton_${mode}`).text();

describe('Wizard-Footer', () => {
    it('All buttons except submit', () => {
        const wrapper = render(
            <Footer
                {...defaultProps}
            />,
        );

        expect(getWizardButton(wrapper, 'cancel')).toBe(1);
        expect(getWizardButtonText(wrapper, 'cancel')).toBe('i18n:cancel');

        expect(getWizardButton(wrapper, 'back')).toBe(1);
        expect(getWizardButtonText(wrapper, 'back')).toBe('i18n:back');

        expect(getWizardButton(wrapper, 'next')).toBe(1);
        expect(getWizardButtonText(wrapper, 'next')).toBe('i18n:next');

        expect(getWizardButton(wrapper, 'submit')).toBe(0);
    });

    it('On last step all buttons except next', () => {
        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    isCurrentStepTheLast: true,
                }}
            />,
        );

        expect(getWizardButton(wrapper, 'submit')).toBe(1);
        expect(getWizardButtonText(wrapper, 'submit')).toBe('i18n:submit');

        expect(getWizardButton(wrapper, 'next')).toBe(0);
    });

    it('Should check on the first step absence back button', () => {
        const wrapper = render(
            <Footer
                {...defaultProps}
                isCurrentStepTheFirst
            />,
        );

        expect(getWizardButton(wrapper, 'back')).toBe(0);
    });

    it('Submit loading', () => {
        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    currentStepState: StepState.passed,
                    isCurrentStepTheLast: true,
                    finalLoading: true,
                }}
            />,
        );

        expect(getWizardButton(wrapper, 'cancel', ':disabled')).toBe(1);
        expect(getWizardButton(wrapper, 'back', ':disabled')).toBe(1);
        expect(getWizardButton(wrapper, 'submit', '.Button2_progress')).toBe(1);
    });

    it('Validation loading', () => {
        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    currentStepState: StepState.loading,
                }}
            />,
        );

        expect(getWizardButton(wrapper, 'cancel', ':disabled')).toBe(1);
        expect(getWizardButton(wrapper, 'back', ':disabled')).toBe(1);
        expect(getWizardButton(wrapper, 'next', '.Button2_progress')).toBe(1);
    });

    it('No cancel button', () => {
        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    noCancelButton: true,
                }}
            />,
        );

        expect(getWizardButton(wrapper, 'cancel')).toBe(0);
    });

    it('No back button', () => {
        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    noBackButton: true,
                }}
            />,
        );

        expect(getWizardButton(wrapper, 'back')).toBe(0);
    });

    describe('Custom button texts', () => {
        const cancelButtonText = 'Кастомная Отмена';
        const backButtonText = 'Кастомный Назад';
        const nextButtonText = 'Кастомный Далее';
        const submitButtonText = 'Кастомный Отправить';

        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    cancelButtonText,
                    backButtonText,
                    nextButtonText,
                    submitButtonText,
                }}
            />,
        );

        it('Custom cancel button text', () => {
            expect(getWizardButtonText(wrapper, 'cancel')).toBe(cancelButtonText);
        });

        it('Custom back button text', () => {
            expect(getWizardButtonText(wrapper, 'back')).toBe(backButtonText);
        });

        it('Custom next button text', () => {
            expect(getWizardButtonText(wrapper, 'next')).toBe(nextButtonText);
        });
    });

    it('Custom submit button text', () => {
        const submitButtonText = 'Кастомный Отправить';

        const wrapper = render(
            <Footer
                {...{
                    ...defaultProps,
                    submitButtonText,
                    isCurrentStepTheLast: true,
                }}
            />,
        );

        expect(getWizardButtonText(wrapper, 'submit')).toBe(submitButtonText);
    });

    describe('Handler checks', () => {
        const onNext = jest.fn();
        const onBack = jest.fn();
        const onCancel = jest.fn();
        const onSubmit = jest.fn();

        const wrapper = shallow(
            <Footer
                {...{
                    ...defaultProps,
                    onNext,
                    onBack,
                    onCancel,
                    onSubmit,
                }}
            />,
        );

        const getWizardButton = (mode: string) => wrapper.find(`.Wizard-FooterButton_${mode}`);

        it('cancel handler check', () => {
            expect(onCancel).toBeCalledTimes(0);
            getWizardButton('cancel').simulate('click');
            expect(onCancel).toBeCalledTimes(1);
        });

        it('back handler check', () => {
            expect(onBack).toBeCalledTimes(0);
            getWizardButton('back').simulate('click');
            expect(onBack).toBeCalledTimes(1);
        });
    });
});
