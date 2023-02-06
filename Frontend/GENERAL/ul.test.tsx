import * as React from 'react';
import { mount } from 'enzyme';

import { mockedRemove, mockedInsert } from '../modifiers/index.mock';

import { ul } from './ul';

describe('useWikiEditor/ul', () => {
    beforeEach(() => {
        mockedRemove.mockClear();
        mockedInsert.mockClear();
    });

    it('Should add indentation on Enter', () => {
        const wrapper = mount(<textarea value="* test" onKeyDown={ul.onKeyDown} />);
        const textarea = wrapper.find('textarea');
        const textareaDOM = textarea.getDOMNode() as HTMLTextAreaElement;

        textareaDOM.selectionStart = 6;

        textarea.simulate('keyDown', { key: 'Enter' });

        expect(mockedRemove).toBeCalledTimes(0);
        expect(mockedInsert).toBeCalledWith({
            text: '\n* ',
        });

        wrapper.unmount();
    });

    it('Should remove indentation from already indented row on Enter', () => {
        const wrapper = mount(<textarea value="* " onKeyDown={ul.onKeyDown} />);
        const textarea = wrapper.find('textarea');
        const textareaDOM = textarea.getDOMNode() as HTMLTextAreaElement;

        textareaDOM.selectionStart = 2;

        textarea.simulate('keyDown', { key: 'Enter' });

        expect(mockedInsert).toBeCalledTimes(0);
        expect(mockedRemove).toBeCalledWith({
            lines: true,
        });

        wrapper.unmount();
    });
});
