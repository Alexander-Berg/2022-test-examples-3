import * as React from 'react';
import { shallow } from 'enzyme';
import * as phoneHelpers from '@yandex-turbo/components/LcPhone/LcPhone.helpers';
import LcLayoutManager from '@yandex-turbo/components/LcLayoutManager/LcLayoutManager';
import { LcLink } from '@yandex-turbo/components/LcLink/LcLink';
import { LcButtonPresenter as LcButton } from '../LcButton';
import { LcButtonFillTypes, LcButtonThemes } from '../LcButton.types';

const UNDER_SELECTOR = '.lc-button__under';
const TEXT_SELECTOR = '.lc-button__text';

function getProps() {
    return {
        theme: LcButtonThemes.Base,
        fillType: LcButtonFillTypes.Fill,
        backgroundColor: '#111',
        textColor: '#222',
        sharedSettings: {
            buttonBackground: '#333',
            buttonTextColor: '#444',
        },
    };
}

function getLpcProps() {
    return { ...getProps(), isLpcMode: true };
}

function getUcProps() {
    return { ...getProps(), isLpcMode: false };
}

jest.mock('@yandex-turbo/components/LcPhone/LcPhone.helpers');

describe('LcButton', () => {
    const phoneLink = 'tel:89261234567';

    beforeEach(() => {
        jest.spyOn(phoneHelpers, 'isPhoneHidingAllowed').mockImplementation(() => false);
        jest.spyOn(phoneHelpers, 'isPhoneLink').mockImplementation(
            (...args) => jest.requireActual('@yandex-turbo/components/LcPhone/LcPhone.helpers').isPhoneLink(...args)
        );
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('inline styles in uc mode', () => {
        test('should not set up inline styles when theme is not base', () => {
            const wrapper = shallow(<LcButton {...getUcProps()} theme={LcButtonThemes.Action} />);

            const underInlineStyles = wrapper.find(UNDER_SELECTOR).prop('style');
            const textInlineStyles = wrapper.find(TEXT_SELECTOR).prop('style');

            expect(underInlineStyles).toEqual({});
            expect(textInlineStyles).toEqual({});
        });

        test('should set up inline styles when theme is base', () => {
            const wrapper = shallow(<LcButton {...getUcProps()} />);

            const underInlineStyles = wrapper.find(UNDER_SELECTOR).prop('style');
            const textInlineStyles = wrapper.find(TEXT_SELECTOR).prop('style');

            expect(underInlineStyles).toEqual({ backgroundColor: '#111' });
            expect(textInlineStyles).toEqual({ color: '#222' });
        });

        test('should ignore sharedSettings when theme is base', () => {
            const wrapper = shallow(<LcButton {...getUcProps()} backgroundColor={undefined} textColor={undefined} />);

            const underInlineStyles = wrapper.find(UNDER_SELECTOR).prop('style');
            const textInlineStyles = wrapper.find(TEXT_SELECTOR).prop('style');

            expect(underInlineStyles).toEqual({});
            expect(textInlineStyles).toEqual({});
        });
    });

    describe('inline styles in lpc mode', () => {
        test('should not set up inline styles when theme is not base', () => {
            const wrapper = shallow(<LcButton {...getLpcProps()} theme={LcButtonThemes.Action} />);

            const underInlineStyles = wrapper.find(UNDER_SELECTOR).prop('style');
            const textInlineStyles = wrapper.find(TEXT_SELECTOR).prop('style');

            expect(underInlineStyles).toEqual({});
            expect(textInlineStyles).toEqual({});
        });

        test('should set up inline styles when theme is base', () => {
            const wrapper = shallow(<LcButton {...getLpcProps()} />);

            const underInlineStyles = wrapper.find(UNDER_SELECTOR).prop('style');
            const textInlineStyles = wrapper.find(TEXT_SELECTOR).prop('style');

            expect(underInlineStyles).toEqual({ backgroundColor: '#333' });
            expect(textInlineStyles).toEqual({ color: '#444' });
        });

        test('should ignore backgroundColor and textColor when theme is base', () => {
            const wrapper = shallow(<LcButton {...getLpcProps()} sharedSettings={{}} />);

            const underInlineStyles = wrapper.find(UNDER_SELECTOR).prop('style');
            const textInlineStyles = wrapper.find(TEXT_SELECTOR).prop('style');

            expect(underInlineStyles).toEqual({});
            expect(textInlineStyles).toEqual({});
        });
    });

    test('Should render LcLayoutManager with hidden phone', () => {
        jest.spyOn(phoneHelpers, 'isPhoneHidingAllowed').mockImplementation(() => true);
        const wrapper = shallow(<LcButton {...getUcProps()} link={phoneLink} />);

        expect(wrapper.is(LcLayoutManager)).toBe(true);
    });

    test('Should not render LcLayoutManager with isImage prop', () => {
        jest.spyOn(phoneHelpers, 'isPhoneHidingAllowed').mockImplementation(() => true);
        const wrapper = shallow(<LcButton {...getUcProps()} link={phoneLink} isImage />);

        expect(wrapper.is(LcLink)).toBe(true);
    });
});
