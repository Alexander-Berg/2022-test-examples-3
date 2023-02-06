/* eslint-disable import/first */
jest.mock('./insert');

import * as React from 'react';
import { mount } from 'enzyme';

import { remove } from './remove';
import { insert } from './insert';

describe('useWikiEditor/remove', () => {
    beforeEach(() => {
        // @ts-ignore
        insert.mockClear();
    });

    it('Should delete selected symbols', () => {
        const wrapper = mount(<textarea value={'test\nsecond\nline'} />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 1;
        textarea.selectionEnd = 2;

        remove({}, textarea);

        expect(insert).toHaveBeenCalledWith(
            { text: '' },
            textarea,
        );

        wrapper.unmount();
    });

    it('Should delete lines when lines=true', () => {
        const wrapper = mount(<textarea value={'test\nsecond\nline'} />);
        const textarea = wrapper.find('textarea').getDOMNode() as HTMLTextAreaElement;

        textarea.selectionStart = 3;
        textarea.selectionEnd = 7;

        remove({ lines: true }, textarea);

        expect(insert).toBeCalledTimes(2);

        expect(insert).toHaveBeenNthCalledWith(
            1,
            { text: '' },
            textarea,
        );

        expect(insert).toHaveBeenNthCalledWith(
            2,
            { text: '\n' },
            textarea,
        );

        wrapper.unmount();
    });
});
