import { SearchBandMessage, TextMessageButton, TestChatHistory } from '../../types/skillTest';
import { ReducerAction } from '../../model/reducer';

type SessionStateActionType =
    | 'ReplyMessage'
    | 'ReplyErrorMessage'
    | 'Message'
    | 'SetHistory'
    | 'ResetSessionState'
    | 'LogoutMessage'
    | 'HideButtons';

type SessionStatePayload = {
    ReplyErrorMessage: {
        message: SearchBandMessage;
        history?: TestChatHistory;
    };
    ReplyMessage: {
        message: SearchBandMessage;
        buttons: TextMessageButton[];
        history?: TestChatHistory;
        isLoggedIn: boolean;
        /**
         * Сообщение, отображающееся сразу под ответом из навыка.
         * Может быть полезно для отображения сервисных сообщений
         */
        trailingMessage?: SearchBandMessage;
    };
    Message: {
        message: SearchBandMessage;
    };
    ResetSessionState: undefined;
    LogoutMessage: {
        message: SearchBandMessage;
        isLoggedIn: boolean;
    };
    HideButtons: undefined;
    SetHistory: {
        history: TestChatHistory;
    };
};

export interface TestChatSessionState {
    messages: SearchBandMessage[];
    buttons: TextMessageButton[];
    history?: TestChatHistory;
    isLoggedIn: boolean;
}

type TestChatReducerAction = ReducerAction<SessionStateActionType, SessionStatePayload>;

export const initialSessionState: TestChatSessionState = {
    buttons: [],
    messages: [],
    isLoggedIn: false,
};

export const sessionStateReducer: React.Reducer<TestChatSessionState, TestChatReducerAction> = (state, action) => {
    switch (action.type) {
        case 'ReplyErrorMessage': {
            const { message, history } = action.payload;

            return {
                ...state,
                history,
                messages: [...state.messages, message],
            };
        }

        case 'ReplyMessage': {
            const { message, history, buttons, isLoggedIn, trailingMessage } = action.payload;

            return {
                ...state,
                history,
                messages: trailingMessage ?
                    [...state.messages, message, trailingMessage] :
                    [...state.messages, message],
                buttons,
                isLoggedIn,
            };
        }

        case 'Message': {
            const { message } = action.payload;

            return {
                ...state,
                messages: [...state.messages, message],
            };
        }

        case 'ResetSessionState': {
            return initialSessionState;
        }

        case 'LogoutMessage': {
            const { isLoggedIn, message } = action.payload;

            return {
                ...state,
                messages: [...state.messages, message],
                isLoggedIn,
            };
        }

        case 'HideButtons': {
            return {
                ...state,
                buttons: [],
            };
        }

        case 'SetHistory': {
            const { history } = action.payload;
            return {
                ...state,
                history,
            };
        }
    }
};
