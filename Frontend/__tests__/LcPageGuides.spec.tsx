import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import * as isEditorConst from '@yandex-turbo/components/lcUtils/isEditor';
import { LcPageGuides } from '../LcPageGuides';

jest.mock('@yandex-turbo/components/lcUtils/isEditor', () => ({
    get isEditor() {
        return undefined;
    },
}));

describe('LcPageGuides tests', () => {
    const existTest = (wrapper: ShallowWrapper) => {
        expect(wrapper.hasClass('lc-page-guides')).toBe(true);
        expect(wrapper.find('.lc-page-guides__guides-item').length).toBe(12);
    };

    const mockIsEditor = (value: boolean) => {
        const isEditorSpy = jest.spyOn(isEditorConst, 'isEditor', 'get');
        isEditorSpy.mockReturnValueOnce(value);
    };

    test('should render correctly if isEditor is true', () => {
        mockIsEditor(true);
        const wrapper = shallow(<LcPageGuides />);
        existTest(wrapper);
    });

    test('should render correctly if isEditor is false, and ignoreIsEditor is true', () => {
        mockIsEditor(false);
        const wrapper = shallow(<LcPageGuides ignoreIsEditor />);
        existTest(wrapper);
    });

    test('should render correctly if isEditor is false', () => {
        mockIsEditor(false);
        const wrapper = shallow(<LcPageGuides />);
        expect(wrapper.hasClass('lc-page-guides')).toBe(false);
    });
});
