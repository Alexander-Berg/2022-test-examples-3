/* eslint-disable import/first */
jest.mock('./insert');

import * as React from 'react';
import { mount } from 'enzyme';

import { wrap } from './wrap';
import { insert } from './insert';

const defaultConfig = {
    before: '**',
    after: '**',
    placeholder: 'Bold',
};

describe('useWikiEditor/wrap', () => {
    it('Should call insert with placeholder if text not selected', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        wrap(defaultConfig, textarea);

        expect(insert).toHaveBeenCalledWith(
            { text: '**Bold**' },
            textarea,
        );

        wrapper.unmount();
    });

    it('Should call insert with selected text', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 1;
        textarea.selectionEnd = 2;

        wrap(defaultConfig, textarea);

        expect(insert).toHaveBeenCalledWith(
            { text: '**e**' },
            textarea,
        );

        wrapper.unmount();
    });

    it('Should select text inside wrapper if inside flag is true', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;
        const initialSelection = {
            start: 1,
            end: 2,
        };

        textarea.selectionStart = initialSelection.start;
        textarea.selectionEnd = initialSelection.end;

        wrap(defaultConfig, textarea);

        expect(textarea.selectionStart).toBe(
            initialSelection.start + defaultConfig.before.length,
        );

        expect(textarea.selectionEnd).toBe(
            initialSelection.end + defaultConfig.before.length,
        );

        wrapper.unmount();
    });

    it('Should select text with wrapper if inside flag is false', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;
        const initialSelection = {
            start: 1,
            end: 2,
        };

        textarea.selectionStart = initialSelection.start;
        textarea.selectionEnd = initialSelection.end;

        wrap({
            ...defaultConfig,
            inner: false,
        }, textarea);

        expect(textarea.selectionStart).toBe(initialSelection.start);

        expect(textarea.selectionEnd).toBe(
            initialSelection.end + defaultConfig.before.length,
        );

        wrapper.unmount();
    });
});
