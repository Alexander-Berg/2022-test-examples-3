import React from 'react';

import { shallow } from 'enzyme';
import { MainStepContainer } from './MainStep.container';
import { HandleMainStepChange } from './MainStep.container';
import { WizardFormData } from '../WizardForm/WizardForm.container';
import { MainStep } from './MainStep';
import { suitableParentsResponse } from '../../testData/suitableParentsResponse';
import { IMainStepData } from '../../ServiceCreation.types';

const stepData: IMainStepData = {
    name: 'testName',
    englishName: 'testEnglishName',
    slug: 'testSlug',
    tags: [],
};

const newSlug = 'englishname';

jest.mock('../../redux/ServiceCreation.api', () => ({
    requestSlug: jest.fn(() => Promise.resolve({ slug: 'englishname1' })),
}));

jest.mock('../../utils/suitableParents', () => ({
    getSuitableParents: jest.fn(() => Promise.resolve(suitableParentsResponse)),
}));

describe('MainStepContainer', () => {
    const onStepChange = jest.fn();
    const visitStep = jest.fn();
    const formData: WizardFormData = {
        description: { description: '', englishDescription: '' },
        general: stepData,
    };

    const setFormData = jest.fn(
        fn => {
            return fn(formData);
        },
    );

    const wrapper = shallow(
        <MainStepContainer
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
            onStepChange={onStepChange}
        />,
    );

    const mainStep = wrapper.find(MainStep);

    it('Should check onChange prop', () => {
        const fieldName = 'name';

        const diff = { [fieldName]: `test${fieldName}` };

        const onChange = mainStep.prop<HandleMainStepChange>('onChange');
        onChange(diff);

        expect(setFormData).toHaveBeenCalled();
        expect(setFormData).toHaveLastReturnedWith(({
            ...formData,
            general: { ...formData.general, ...diff },
        }));
    });

    it('Should change suitableSlug on english name commit', done => {
        const handleEnglishNameCommit = mainStep.prop<() => void>('onEnglishNameCommit');
        handleEnglishNameCommit();

        setTimeout(() => {
            expect(wrapper.find(MainStep).prop('suitableSlug')).toEqual({ text: `${newSlug}1`, val: `${newSlug}1` });

            done();
        }, 0);
    });

    it('Should not change suitableSlug on english name commit because english name is empty', done => {
        const wrapper = shallow(
            <MainStepContainer
                formData={{ ...formData, general: { ...formData.general, englishName: '' } }}
                setFormData={setFormData}
                visitStep={visitStep}
                stepData={{ ...stepData, englishName: '' }}

                errors={{}}
                stepStates={{}}
                availableSteps={[]}
                stepEntities={[]}

                finalLoading={false}
                finalErrors={{}}

                visitedSteps={new Set([])}
                onStepChange={onStepChange}
            />,
        );

        const mainStep = wrapper.find(MainStep);

        const handleEnglishNameCommit = mainStep.prop<() => void>('onEnglishNameCommit');
        handleEnglishNameCommit();

        setTimeout(() => {
            expect(wrapper.find(MainStep).prop('suitableSlug')).toEqual(undefined);

            done();
        }, 0);
    });

    it('Should get suitable parents', done => {
        const newOwner = { id: 'login', title: 'Name' };

        const onChange = mainStep.prop<HandleMainStepChange>('onChange');
        onChange({ owner: newOwner });

        setTimeout(() => {
            const suitableParents = suitableParentsResponse.map(({ name: { ru: name }, id, slug }) => {
                return { text: name, val: id, link: `/services/${slug}` };
            });

            expect(wrapper.find(MainStep).prop('suitableParents')).toEqual(suitableParents);

            done();
        }, 0);
    });

    it('Should call visitStep on commit', () => {
        const initialCalls = visitStep.mock.calls.length;

        mainStep.prop<() => void>('onCommit')();

        expect(visitStep).toHaveBeenCalledTimes(initialCalls + 1);
    });

    it('Should call visitStep on english name commit', () => {
        const initialCalls = visitStep.mock.calls.length;

        mainStep.prop<() => void>('onEnglishNameCommit')();

        expect(visitStep).toHaveBeenCalledTimes(initialCalls + 1);
    });
});
