/* eslint-disable import/first */
jest.mock('./insert');
jest.mock('./wrap');

import * as React from 'react';
import { mount } from 'enzyme';

import { indent } from './indent';
import { wrap } from './wrap';
import { insert } from './insert';

const defaultConfig = {
    indentRegexp: /^(\s*)\*\s/,
    indent: '* ',
    placeholder: 'list element',
};

const mockedIncrement = jest.fn((indent: string) => String(parseInt(indent, 10) + 1));

describe('useWikiEditor/indent', () => {
    beforeEach(() => {
        // @ts-ignore
        insert.mockClear();
        // @ts-ignore
        wrap.mockClear();
    });

    it('Should indent one line string', () => {
        const wrapper = mount(<textarea value="test" />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 1;
        textarea.selectionEnd = 2;

        indent(defaultConfig, textarea);

        expect(insert).toBeCalledTimes(0);
        expect(wrap).toBeCalledTimes(1);
        expect(wrap).toBeCalledWith(
            {
                before: defaultConfig.indent,
                after: '',
                placeholder: defaultConfig.placeholder,
            },
            textarea,
        );

        wrapper.unmount();
    });

    it('Should indent multiple lines', () => {
        const wrapper = mount(<textarea value={'test\nsecond\nline'} />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 3;
        textarea.selectionEnd = 7;

        indent(defaultConfig, textarea);

        expect(wrap).toBeCalledTimes(0);
        expect(insert).toBeCalledWith(
            {
                text: '* test\n* second',
            },
            textarea,
        );

        wrapper.unmount();
    });

    it('Should not insert indentation to already indented lines', () => {
        const wrapper = mount(<textarea value={'test\n* second\nline'} />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 3;
        textarea.selectionEnd = 7;

        indent(defaultConfig, textarea);

        expect(wrap).toBeCalledTimes(0);
        expect(insert).toBeCalledTimes(1);
        expect(insert).toBeCalledWith(
            {
                text: '* test\n* second',
            },
            textarea,
        );

        wrapper.unmount();
    });

    it('Should call increment for each indented line', () => {
        const wrapper = mount(<textarea value={'test\n2second\nline'} />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 3;
        textarea.selectionEnd = 15;

        indent({
            ...defaultConfig,
            indent: '1',
            indentRegexp: /^(\d+)/,
            increment: mockedIncrement,
        }, textarea);

        expect(wrap).toBeCalledTimes(0);
        expect(mockedIncrement).toBeCalledTimes(2);
        expect(mockedIncrement).toHaveBeenNthCalledWith(1, '1');
        expect(mockedIncrement).toHaveBeenNthCalledWith(2, '2');

        wrapper.unmount();
    });
});
