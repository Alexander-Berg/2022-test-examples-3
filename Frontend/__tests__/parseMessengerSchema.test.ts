import { parseMessengerScheme } from '../parseMessengerSchema';

describe('Parse messenger scheme', () => {
    describe('is not schema', () => {
        it('with protocol yandex.ru', () => {
            expect(parseMessengerScheme('https://yandex.ru/chat')).toBeUndefined();
        });
        it('yandex.ru', () => {
            expect(parseMessengerScheme('yandex.ru/chat')).toBeUndefined();
        });
        it('other url', () => {
            expect(parseMessengerScheme('https://example.com')).toBeUndefined();
        });
    });

    describe('chatList', () => {
        it('without text', () => {
            expect(parseMessengerScheme('messenger://chat/list')).toEqual({ chatList: true });
        });
        it('with text', () => {
            expect(parseMessengerScheme('messenger://chat/list?text=1234')).toEqual({ chatList: true });
        });
        it('with array text', () => {
            expect(parseMessengerScheme('messenger://chat/list?text=1234&text=qwer')).toEqual({ chatList: true });
        });
    });

    describe('chatOpen', () => {
        it('without text', () => {
            expect(parseMessengerScheme('messenger://chat/open/?chat_id=testChatId')).toEqual({
                chatId: 'testChatId',
            });
        });
        it('without require param', () => {
            expect(parseMessengerScheme('messenger://chat/open/?text=asdfzxf')).toBeUndefined();
        });
        it('with text', () => {
            expect(parseMessengerScheme('messenger://chat/open/?chat_id=testChatId&text=1234')).toEqual({
                chatId: 'testChatId',
                pasteText: '1234',
            });
        });
        it('with array text', () => {
            expect(parseMessengerScheme('messenger://chat/open/?chat_id=testChatId&text=1234&text=qwer')).toEqual({
                chatId: 'testChatId',
                pasteText: '1234',
            });
        });
    });

    describe('chatInvite', () => {
        it('without text', () => {
            expect(parseMessengerScheme('messenger://chat/invite/?chat_id=testChatId')).toEqual({
                chatId: 'testChatId',
            });
        });
        it('with text', () => {
            expect(parseMessengerScheme('messenger://chat/invite/?chat_id=testChatId&text=1234')).toEqual({
                chatId: 'testChatId',
                pasteText: '1234',
            });
        });
    });

    describe('chatInviteByHash', () => {
        it('without text', () => {
            expect(parseMessengerScheme('messenger://chat/invite_byhash/?invite_hash=testChatId')).toEqual({
                inviteHash: 'testChatId',
            });
        });
        it('with text', () => {
            expect(parseMessengerScheme('messenger://chat/invite_byhash/?invite_hash=testChatId&text=1234')).toEqual({
                inviteHash: 'testChatId',
                pasteText: '1234',
            });
        });
    });

    describe('user', () => {
        it('without text', () => {
            expect(parseMessengerScheme('messenger://user?user_id=testUserId')).toEqual({
                guid: 'testUserId',
            });
        });
        it('with text', () => {
            expect(parseMessengerScheme('messenger://user?user_id=testUserId&text=1234')).toEqual({
                guid: 'testUserId',
                pasteText: '1234',
            });
        });
    });
});
