/* eslint-disable import/first */
jest.mock('../../services/History', () => {});
jest.mock('../../lib/exportMessagesToText');

import { AppState } from '../../store';

import * as en from '../../../langs/yamb/en.json';
import { exportMessagesToText } from '../../lib/exportMessagesToText';
import { getSelectionAsText } from '../selection';
import i18n from '../../../shared/lib/i18n';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { messagesMockFactory } from '../../store/__tests__/mock/messages';
import { localSettingsMockFactory } from '../../store/__tests__/mock/localSettings';
import { createTextData, normalizeMessageKey } from '../../helpers/messages';
import { stateMockFactory } from '../../store/__tests__/mock/state';

i18n.locale('en', en);

function getSelectionState(partialSelection: Partial<AppState['selection']> = {}) {
    return partialSelection as AppState['selection'];
}

describe('SelectionSelectors', () => {
    const stateMock = stateMockFactory();
    const usersMock = usersMockFactory();
    const messagesMock = messagesMockFactory();
    const localSettingsMock = localSettingsMockFactory();

    describe('#getSelectionAsText', () => {
        it('Should exported messages data in right sort', () => {
            const [user1, user2] = usersMock.createUnlimited()(
                {
                    display_name: 'John',
                },
                {
                    display_name: 'Jane',
                },
            );

            const [message1, message3] = messagesMock.createTextMessage()(
                {
                    from: user2,
                    data: createTextData('Message text 1'),
                },
                {
                    from: user2,
                    timestamp: 1572598449443004,
                    data: createTextData('Message text 3'),
                },
            );

            const [message2] = messagesMock.createTextMessage()(
                {
                    from: user1,
                    timestamp: 1572526283519004,
                    data: createTextData('Message text 2'),
                    forwarded: [message1],
                },
            );

            const state = stateMock.createState({
                users: usersMock.createState(user1, user2),
                localSettings: localSettingsMock.createState(),
                selection: getSelectionState({
                    selected: [message3, message2].map(normalizeMessageKey),
                }),
                messages: messagesMock.createState([message1, message2, message3]),
            });

            getSelectionAsText(state);

            expect(exportMessagesToText).toBeCalledWith([
                {
                    author: 'John',
                    date: '31 October 2019',
                    forwarded: {
                        author: 'Jane',
                        text: 'Message text 1',
                    },
                    text: 'Message text 2',
                    time: '15:51',
                    timestamp: 1572526283519004,
                }, {
                    author: 'Jane',
                    date: '1 November 2019',
                    forwarded: undefined,
                    text: 'Message text 3',
                    time: '11:54',
                    timestamp: 1572598449443004,
                },
            ]);
        });
    });
});
