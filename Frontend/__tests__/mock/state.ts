import { AppState } from '../..';
import { initialState as chatsInitialState } from '../../chats';
import { initialState as usersInitialState } from '../../users';
import { initialState as bucketsInitialState } from '../../buckets';
import { initialState as localSettingsInitialState } from '../../localSettings';
import { initialState as conversationInitialState } from '../../conversations';
import { initialState as metaInitialState } from '../../meta';
import { initialState as composeInitialState } from '../../compose';

export function stateMockFactory() {
    return {
        createState: (state: Partial<AppState> = {}) => ({
            chats: chatsInitialState,
            users: usersInitialState,
            buckets: bucketsInitialState,
            localSettings: localSettingsInitialState,
            conversations: conversationInitialState,
            metastore: metaInitialState,
            compose: composeInitialState,
            config: {},
            ...state,
        } as any as AppState),
    };
}
