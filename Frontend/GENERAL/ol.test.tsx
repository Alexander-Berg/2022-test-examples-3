import * as React from 'react';
import { mount } from 'enzyme';

import { mockedRemove, mockedInsert } from '../modifiers/index.mock';

import { ol } from './ol';

describe('useWikiEditor/ol', () => {
    beforeEach(() => {
        mockedRemove.mockClear();
        mockedInsert.mockClear();
    });

    it('Should add indentation on Enter', () => {
        const wrapper = mount(<textarea value="2. test" onKeyDown={ol.onKeyDown} />);
        const textarea = wrapper.find('textarea');
        const textareaDOM = textarea.getDOMNode() as HTMLTextAreaElement;

        textareaDOM.selectionStart = 7;

        textarea.simulate('keyDown', { key: 'Enter' });

        expect(mockedRemove).toBeCalledTimes(0);
        expect(mockedInsert).toBeCalledWith({
            text: '\n3. ',
        });

        wrapper.unmount();
    });

    it('Should remove indentation from already indented row on Enter', () => {
        const wrapper = mount(<textarea value="2. " onKeyDown={ol.onKeyDown} />);
        const textarea = wrapper.find('textarea');
        const textareaDOM = textarea.getDOMNode() as HTMLTextAreaElement;

        textareaDOM.selectionStart = 3;

        textarea.simulate('keyDown', { key: 'Enter' });

        expect(mockedInsert).toBeCalledTimes(0);
        expect(mockedRemove).toBeCalledWith({
            lines: true,
        });

        wrapper.unmount();
    });
});
