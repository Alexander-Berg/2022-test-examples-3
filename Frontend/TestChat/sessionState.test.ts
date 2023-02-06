/* eslint-disable */
import { sessionStateReducer, initialSessionState as defaultState } from './sessionState';
import { TextMessageButton, TextMessageSession } from '../../types/skillTest';

describe('testChatReducer', () => {
    it('Handles error message action without history', () => {
        const message = {
            messageId: '1',
        };

        const newState = sessionStateReducer(defaultState, {
            type: 'ReplyErrorMessage',
            payload: { message, history: undefined },
        });

        expect(newState).toEqual({
            ...defaultState,
            messages: [message],
            history: undefined,
        });
    });

    it('Handles error message action with history', () => {
        const message = {
            messageId: '1',
        };

        const history = {
            request: {},
            response_raw: '',
        };

        const newState = sessionStateReducer(defaultState, {
            type: 'ReplyErrorMessage',
            payload: { message, history },
        });

        expect(newState).toEqual({
            ...defaultState,
            messages: [message],
            history,
        });
    });

    it('handles error message adding to non empty messages', () => {
        const message = {
            messageId: '1',
        };

        const newState = sessionStateReducer(
            {
                ...defaultState,
                messages: [{ messageId: '2' }, { messageId: '3' }],
            },
            {
                type: 'ReplyErrorMessage',
                payload: { message, history: undefined },
            },
        );

        expect(newState).toEqual({
            ...defaultState,
            messages: [{ messageId: '2' }, { messageId: '3' }, message],
            history: undefined,
        });
    });

    it('handler message action', () => {
        const message = {
            messageId: '1',
        };

        const buttons: TextMessageButton[] = [{ title: 'test1' }, { title: 'test' }];

        const newState = sessionStateReducer(defaultState, {
            type: 'ReplyMessage',
            payload: { message, buttons, isLoggedIn: false, history: undefined },
        });

        expect(newState).toEqual({
            ...defaultState,
            buttons,
            messages: [message],
            isLoggedIn: false,
            history: undefined,
        });
    });

    it('handler message action adding to non empty messages', () => {
        const message = {
            messageId: '1',
        };

        const buttons: TextMessageButton[] = [{ title: 'test1' }, { title: 'test' }];

        const newState = sessionStateReducer(
            {
                ...defaultState,
                messages: [{ messageId: '2' }, { messageId: '3' }],
            },
            {
                type: 'ReplyMessage',
                payload: { message, buttons, isLoggedIn: false, history: undefined },
            },
        );

        expect(newState).toEqual({
            ...defaultState,
            buttons,
            messages: [{ messageId: '2' }, { messageId: '3' }, message],
            isLoggedIn: false,
            history: undefined,
        });
    });

    it('handles message', () => {
        const message = {
            messageId: '1',
        };

        const newState = sessionStateReducer(defaultState, {
            type: 'Message',
            payload: { message },
        });

        expect(newState).toEqual({
            ...defaultState,
            messages: [message],
        });
    });

    it('handles message adding to non empty messages', () => {
        const message = {
            messageId: '1',
        };

        const newState = sessionStateReducer(
            {
                ...defaultState,
                messages: [{ messageId: '2' }, { messageId: '3' }],
            },
            {
                type: 'Message',
                payload: { message },
            },
        );

        expect(newState).toEqual({
            ...defaultState,
            messages: [{ messageId: '2' }, { messageId: '3' }, message],
        });
    });

    it('handles reset action', () => {
        const message = {
            messageId: '1',
        };

        const buttons: TextMessageButton[] = [{ title: 'test1' }, { title: 'test' }];

        const changedState = sessionStateReducer(defaultState, {
            type: 'ReplyMessage',
            payload: { message, buttons, isLoggedIn: false, history: undefined },
        });

        const resetState = sessionStateReducer(changedState, {
            type: 'ResetSessionState',
        });

        expect(resetState).toEqual(defaultState);
    });

    it('handles logout message', () => {
        const message = {
            messageId: '1',
        };

        const newState = sessionStateReducer(defaultState, {
            type: 'LogoutMessage',
            payload: { message, isLoggedIn: false },
        });

        expect(newState).toEqual({
            ...defaultState,
            isLoggedIn: false,
            messages: [message],
        });
    });

    it('hides buttons', () => {
        const newState = sessionStateReducer(defaultState, { type: 'HideButtons' });

        expect(newState).toEqual({
            ...defaultState,
            buttons: [],
        });
    });
});
