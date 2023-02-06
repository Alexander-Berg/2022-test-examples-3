import React from 'react';

import { shallow } from 'enzyme';
import { MainStep } from './MainStep';
import { Parent } from './Parent/Parent';
import { IMainStepData } from '../../ServiceCreation.types';

const stepData: IMainStepData = {
    name: 'testName',
    englishName: 'testEnglishName',
    slug: 'testSlug',
    tags: [],
};

const suitableParents = [{ text: 'text', val: 'val' }];
const externalParentId = '383';

const errors = {
    name: 'error with name',
    englishName: 'error with englishName',
    slug: 'error with slug',
    owner: 'error with owner',
};

describe('MainStep', () => {
    const handleChange = jest.fn();
    const handleCommit = jest.fn();
    const handleEnglishNameCommit = jest.fn();
    const suitableSlug = { text: 'slug', val: 'slug' };
    const setExternalParentId = jest.fn();

    const wrapper = shallow(
        <MainStep
            stepData={stepData}
            errors={errors}

            suitableParents={suitableParents}
            externalParentId={externalParentId}
            setExternalParentId={setExternalParentId}

            onChange={handleChange}
            onCommit={handleCommit}
            suitableSlug={suitableSlug}
            onEnglishNameCommit={handleEnglishNameCommit}
        />,
    );

    const name = wrapper.find('.MainStep-Name');
    const englishName = wrapper.find('.MainStep-EnglishName');
    const slug = wrapper.childAt(2).dive();

    const owner = wrapper.find('.MainStep-Owner');
    const parent = wrapper.find('.MainStep-Parent');
    const tags = wrapper.find('.MainStep-Tags');

    const textinputFields = [
        { field: name, fieldName: 'name' },
        { field: englishName, fieldName: 'englishName' },
    ];
    const fields = [
        ...textinputFields,
        { field: slug, fieldName: 'slug' },
        { field: owner, fieldName: 'owner' },
        { field: tags, fieldName: 'tags' },
        { field: parent, fieldName: 'parent' },
    ];

    it('Should check fields presence', () => {
        fields.forEach(({ field }) => expect(field));
    });

    it('Should pass correct slug prop', () => {
        expect(slug.prop('suitableSlug')).toEqual(suitableSlug);
    });

    describe('Should pass correct value prop', () => {
        fields.forEach(({ field, fieldName }) => {
            it(`field ${fieldName}`, () => {
                expect(field.prop('value')).toEqual(stepData[fieldName]);
            });
        });
    });

    it('Should pass to Parent correct props', () => {
        expect(wrapper.find(Parent).prop('suitableParents')).toEqual(suitableParents);
        expect(wrapper.find(Parent).prop('externalParentId')).toEqual(externalParentId);
        expect(wrapper.find(Parent).prop('setExternalParentId')).toEqual(setExternalParentId);
    });

    describe('Should pass correct fieldName prop in textinput fields', () => {
        textinputFields.forEach(({ field, fieldName }) => {
            it(`field ${fieldName}`, () => {
                expect(field.prop('fieldName')).toEqual(fieldName);
            });
        });
    });

    it('Should pass required=true for required FieldWithTextinput', () => {
        expect(name.prop('required')).toEqual(true);
        expect(englishName.prop('required')).toEqual(true);
    });
});
