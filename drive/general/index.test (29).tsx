import { mount } from 'enzyme';
import * as React from 'react';

import { IRichEditorProps, IRichEditorState, RichEditor } from './index';

const RICH_EDITOR_MOCK_PROPS: IRichEditorProps = {
    initData: 'сообщение',
    fastAnswers: [],
    tag_id: 'some_tag_id',
    onSendText: () => {},
    className: 'testClassName',
};

describe('Rich Editor closed by default', () => {
    RICH_EDITOR_MOCK_PROPS.isClosedByDefault = true;
    const RICH_EDITOR = mount(<RichEditor {...RICH_EDITOR_MOCK_PROPS}/>);

    it ('closed by default', () => {
        //@ts-ignore
        const STATE: IRichEditorState = RICH_EDITOR.state();
        expect(STATE.isClosed).toBeTruthy();
    });

    it ('correct open', () => {
        const OPEN_BUTTON = RICH_EDITOR.find('#openBtn');

        OPEN_BUTTON.simulate('click');
        //@ts-ignore
        const STATE1: IRichEditorState = RICH_EDITOR.state();
        expect(STATE1.isClosed).toBeFalsy();

        OPEN_BUTTON.simulate('click');
        //@ts-ignore
        const STATE2: IRichEditorState = RICH_EDITOR.state();
        expect(STATE2.isClosed).toBeTruthy();
    });
});

describe('Rich Editor opened by default', () => {
    RICH_EDITOR_MOCK_PROPS.isClosedByDefault = false;
    const RICH_EDITOR = mount(<RichEditor {...RICH_EDITOR_MOCK_PROPS}/>);

    it ('opened by default', () => {
        //@ts-ignore
        const STATE: IRichEditorState = RICH_EDITOR.state();
        expect(STATE.isClosed).toBeFalsy();
    });

    it ('correct close', () => {
        const OPEN_BUTTON = RICH_EDITOR.find('#openBtn');

        OPEN_BUTTON.simulate('click');
        //@ts-ignore
        const STATE1: IRichEditorState = RICH_EDITOR.state();
        expect(STATE1.isClosed).toBeTruthy();

        OPEN_BUTTON.simulate('click');
        //@ts-ignore
        const STATE2: IRichEditorState = RICH_EDITOR.state();
        expect(STATE2.isClosed).toBeFalsy();
    });
});
