jest.mock('react-loadable', () => {
    return {
        default: {
            Map: () => () => null,
        },
    };
});

jest.mock('../../helpers/chat', () => {
    return {
        // @ts-ignore
        ...(jest.requireActual('../../helpers/chat')),
        getLinkOrigin: () => 'https://yandex.ru/chat',
    };
});

import { testDelete } from '@yandex-int/messenger.utils';
import { AppState } from '../../store';
import { processActions } from '../Navigate';
import * as ParentWindowForSpy from '../ParentWindow';
import * as HistoryForSpy from '../../services/History';

describe('Navigate', () => {
    const dispatch = jest.fn((func) => func(() => {}));

    function getStateFactory(state: any) {
        return jest.fn(() => state as AppState);
    }

    beforeEach(() => {
        window.open = jest.fn();

        jest.spyOn(ParentWindowForSpy, 'navigateTo');
        jest.spyOn(HistoryForSpy, 'pushHistory');
    });

    afterEach(() => {
        jest.spyOn(ParentWindowForSpy, 'navigateTo').mockReset();
        jest.spyOn(HistoryForSpy, 'pushHistory').mockReset();
    });

    describe('openComplainAction() thunk', () => {
        it('Open messenger schema', () => {
            processActions([
                'messenger://chat/open/?chat_id=testChatId&text=1234',
            ])(dispatch, getStateFactory({}));

            expect(ParentWindowForSpy.navigateTo).toBeCalledWith({
                chatId: 'testChatId',
                pasteText: '1234',
            }, {
                logParams: { source: 'open_by_report' },
            });
        });

        describe('Open messenger schema in new tab', () => {
            beforeEach(() => {
                window.flags.disableNavigation = '1';
            });

            afterEach(() => {
                window.flags.disableNavigation = '0';
            });

            it('Open chat list', () => {
                processActions([
                    'messenger://chat/list',
                ])(dispatch, getStateFactory({}));

                expect(window.open).toBeCalledWith('https://yandex.ru/chat#/');
            });

            it('Open chat', () => {
                processActions([
                    'messenger://chat/open/?chat_id=testChatId',
                ])(dispatch, getStateFactory({}));

                expect(window.open).toBeCalledWith('https://yandex.ru/chat#/chats/testChatId');
            });

            it('Open chat with text', () => {
                processActions([
                    'messenger://chat/open/?chat_id=testChatId&text=1234',
                ])(dispatch, getStateFactory({}));

                expect(window.open).toBeCalledWith('https://yandex.ru/chat?text=1234#/chats/testChatId');
            });

            it('Open invite', () => {
                processActions([
                    'messenger://chat/invite_byhash/?invite_hash=testChatId',
                ])(dispatch, getStateFactory({}));

                expect(window.open).toBeCalledWith('https://yandex.ru/chat#/join/testChatId');
            });

            it('Open user', () => {
                processActions([
                    'messenger://user?user_id=testUserId',
                ])(dispatch, getStateFactory({}));

                expect(window.open).toBeCalledWith('https://yandex.ru/chat#/user/testUserId');
            });
        });

        it('Open deep link', () => {
            const oldLocation = window.location;

            testDelete(window, 'location');

            // @ts-ignore
            window.location = {
                hostname: 'yandex.ru',
                pathname: '/chat',
            };

            processActions([
                'https://yandex.ru/chat?text=1234#/chat/testChatId',
            ])(dispatch, getStateFactory({}));

            expect(HistoryForSpy.pushHistory).toBeCalledWith('/chat/testChatId');

            window.location = oldLocation;
        });

        it('Open deep link with disableNavigation', () => {
            window.flags.disableNavigation = '1';

            processActions([
                'https://yandex.ru/chat?text=1234#/chat/testChatId',
            ])(dispatch, getStateFactory({}));

            expect(window.open).toBeCalledWith('https://yandex.ru/chat?text=1234#/chat/testChatId');

            window.flags.disableNavigation = '0';
        });

        it('Open link new tab', () => {
            processActions([
                'https://example.com/?params=1',
            ])(dispatch, getStateFactory({}));

            expect(window.open).toBeCalledWith('https://example.com/?params=1');
        });
    });
});
