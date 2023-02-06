/* eslint-disable @typescript-eslint/no-non-null-assertion */
import * as React from 'react';
import { mount } from 'enzyme';

import { insert } from './insert';

const defaultConfig = {
    text: 'text',
};

describe('useWikiEditor/insert', () => {
    it('Should exec comand for insert text, if command executed successfully', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        const mockedExecCommand = jest.fn(() => true);

        textarea.ownerDocument!.execCommand = mockedExecCommand;

        textarea.selectionStart = 1;
        textarea.selectionEnd = 2;

        insert(defaultConfig, textarea);

        expect(mockedExecCommand).toBeCalledWith(
            'insertText',
            false,
            defaultConfig.text,
        );

        wrapper.unmount();
    });

    it('Should use setRangeText if execCommand executed with failure', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        const mockedExecCommand = jest.fn(() => false);
        const spyTextareaSetRangeText = jest.spyOn(textarea, 'setRangeText');
        const spyTextareaDispatchEvent = jest.spyOn(textarea, 'dispatchEvent');

        textarea.ownerDocument!.execCommand = mockedExecCommand;

        insert(defaultConfig, textarea);

        expect(spyTextareaSetRangeText).toBeCalledWith(
            defaultConfig.text,
            0,
            0,
            'end',
        );

        expect(spyTextareaDispatchEvent).toBeCalledTimes(1);

        wrapper.unmount();
    });

    it('Should pass selection to setRangeText', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        const mockedExecCommand = jest.fn(() => false);
        const spyTextareaSetRangeText = jest.spyOn(textarea, 'setRangeText');

        textarea.ownerDocument!.execCommand = mockedExecCommand;

        const selectionStart = 1;
        const selectionEnd = 2;

        textarea.selectionStart = selectionStart;
        textarea.selectionEnd = selectionEnd;

        insert(defaultConfig, textarea);

        expect(spyTextareaSetRangeText).toBeCalledWith(
            defaultConfig.text,
            selectionStart,
            selectionEnd,
            'end',
        );

        wrapper.unmount();
    });
});
