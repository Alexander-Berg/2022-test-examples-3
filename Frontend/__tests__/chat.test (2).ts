const services = {
    other: { id: 0, ns: 0, chatId: '0/0/123', channelId: '1/0/123', businessId: '2/0/123' },
    first: { id: 1, ns: 1, chatId: '0/1/123', channelId: '1/1/123', businessId: '2/1/123' },
    second: { id: 2, ns: 2, chatId: '0/2/123', channelId: '1/2/123', businessId: '2/2/123' },
};

const mockGetGlobalParam = jest.fn();

jest.mock('../../../shared/lib/globalParams', () => ({
    getGlobalParam: mockGetGlobalParam,
}));
jest.mock('../../../configs/separatedServices', () => ({
    default: {
        [services.first.id]: [services.first.ns],
        [services.second.id]: [services.second.ns],
    },
}));

import { testDelete } from '@yandex-int/messenger.utils';
/* eslint-disable import/first */
import {
    isBackofficeChat,
    isChatWithOneself,
    getPrivateChatId,
    getPrivateChatPartnerGuid,
    isValidTankerText,
    isValidChatBarMetadata,
    isHiddenChatById,
    isChannelChatId,
    isBusinessChatId,
} from '../chat';
import { getChatBar } from './chat.dataset';

describe('ChatHelpers', () => {
    describe('#getPrivateChatId', () => {
        it('Should return empty chatId', () => {
            expect(getPrivateChatId('', 'abc')).toBe('');

            expect(getPrivateChatId('abc', '')).toBe('');
        });

        it('Should return chatId', () => {
            expect(getPrivateChatId('xyz', 'abc')).toBe('abc_xyz');

            expect(getPrivateChatId('abc', 'xyz')).toBe('abc_xyz');
        });
    });

    describe('isBackofficeChat', () => {
        it('Should return false for no backoffice chat id', () => {
            expect(isBackofficeChat('42bd799b-af10-4750-9516-e67fc5bd9c3a_ca4e0835-0998-4780-949a-fd2004a72c59')).toBe(false);
            expect(isBackofficeChat('0/0/9c3d324e-3400-48fd-9837-861ad5a996a0')).toBe(false);
            expect(isBackofficeChat('0/7/9c3d324e-3400-48fd-9837-861ad5a996a0')).toBe(false);
            expect(isBackofficeChat('0/4')).toBe(false);
            expect(isBackofficeChat('')).toBe(false);
        });

        it('Should return true for backoffice chat id', () => {
            expect(isBackofficeChat('0/4/9c3d324e-3400-48fd-9837-861ad5a996a0')).toBe(true);
            expect(isBackofficeChat('0/4/ololo')).toBe(true);
            expect(isBackofficeChat('0/4/')).toBe(true);
        });
    });

    describe('isChannelChatId', () => {
        it('Should return false for not channel chat id', () => {
            expect(isChannelChatId(services.other.chatId)).toBe(false);
            expect(isChannelChatId(services.other.businessId)).toBe(false);
            expect(isChannelChatId('0/4')).toBe(false);
            expect(isChannelChatId('')).toBe(false);
        });

        it('Should return true for channel chat id', () => {
            expect(isChannelChatId(services.other.channelId)).toBe(true);
        });
    });

    describe('isBusinessChatId', () => {
        it('Should return false for not business chat id', () => {
            expect(isBusinessChatId(services.other.chatId)).toBe(false);
            expect(isBusinessChatId(services.other.channelId)).toBe(false);
            expect(isBusinessChatId('0/4')).toBe(false);
            expect(isBusinessChatId('')).toBe(false);
        });

        it('Should return true for business chat id', () => {
            expect(isBusinessChatId(services.other.businessId)).toBe(true);
        });
    });

    describe('#isChatWithOneself', () => {
        it('Чат с самим собой', () => {
            expect(isChatWithOneself('7b39b71f-f82b-4141-9190-778705ba1a73_7b39b71f-f82b-4141-9190-778705ba1a73')).toBeTruthy();
        });

        it('Чат не с самим собой', () => {
            expect(isChatWithOneself('b39b71f-f82b-4141-9190-778705ba1a73_7b39b71f-f82b-4141-9190-778705ba1a73')).toBeFalsy();
            expect(isChatWithOneself('7b39b71f-f82b-4141-9190-778705ba1a73_7b39b71f-f82b-4141-9190-778705ba1a7')).toBeFalsy();
            expect(isChatWithOneself('7b39b71f-f82b-4141-9190-778705ba1a73')).toBeFalsy();
            expect(isChatWithOneself('0/0/9c3d324e-3400-48fd-9837-861ad5a996a0')).toBeFalsy();
            expect(isChatWithOneself('7b39b71f-f82b-4141-9190-778705ba1a73_')).toBeFalsy();
            expect(isChatWithOneself('7b39b71f-f82b-4141-9190-778705ba1a73_asdasdas-asdasdasdas-asdasd')).toBeFalsy();
        });
    });

    describe('#getPrivateChatPartnerGuid', () => {
        it('Чат с роботом', () => {
            expect(getPrivateChatPartnerGuid(
                '48acbfc2-5761-46b4-a031-65a06ba03d99_22acbbc2-4531-4fb4-aa31-44a06ba43d49',
                '48acbfc2-5761-46b4-a031-65a06ba03d99',
            )).toBe('22acbbc2-4531-4fb4-aa31-44a06ba43d49');
        });

        it('Групповой чат', () => {
            expect(getPrivateChatPartnerGuid(
                '0/0/8172145b-cec4-43f2-a73a-8e131b008ace',
                '48acbfc2-5761-46b4-a031-65a06ba03d99',
            )).toBeUndefined();
        });

        it('Чат с самим собой', () => {
            expect(getPrivateChatPartnerGuid(
                '48acbfc2-5761-46b4-a031-65a06ba03d99_48acbfc2-5761-46b4-a031-65a06ba03d99',
                '48acbfc2-5761-46b4-a031-65a06ba03d99',
            )).toBe('48acbfc2-5761-46b4-a031-65a06ba03d99');
        });
    });

    describe('#isValidTankerText', () => {
        it('returns true when textObj is valid', () => {
            expect(isValidTankerText({
                i18n_key: 'key',
                text: 'value',
            })).toBeTruthy();
        });

        it('returns false when textObj is empty', () => {
            expect(isValidTankerText(undefined)).toBeFalsy();
        });

        it('returns false when textObj has wrong type', () => {
            expect(isValidTankerText('text')).toBeFalsy();
        });

        it('returns false when textObj.i18n_key is not defined', () => {
            expect(isValidTankerText({
                text: 'value',
            })).toBeFalsy();
        });

        it('returns false when textObj.i18n_key has wrong type', () => {
            expect(isValidTankerText({
                i18n_key: [],
                text: 'value',
            })).toBeFalsy();
        });

        it('returns false when textObj.text is not defined', () => {
            expect(isValidTankerText({
                i18n_key: 'key',
            })).toBeFalsy();
        });

        it('returns false when textObj.text has wrong type', () => {
            expect(isValidTankerText({
                i18n_key: 'key',
                text: [],
            })).toBeFalsy();
        });
    });

    describe('#isValidChatBarMetadata', () => {
        it('returns true when data is valid', () => {
            const chatBar = getChatBar();

            expect(isValidChatBarMetadata(chatBar)).toBeTruthy();
        });

        it('returns true when data has no optional fields', () => {
            const chatBar = getChatBar();

            testDelete(chatBar, 'subtitle', 'img', 'button');

            expect(isValidChatBarMetadata(chatBar)).toBeTruthy();
        });

        it('returns false when chatbar is not defined', () => {
            const chatBar = undefined;

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when title is not defined', () => {
            const chatBar = getChatBar();

            testDelete(chatBar, 'title');

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when title is invalid', () => {
            const chatBar = getChatBar();

            testDelete(chatBar.title, 'text');

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when subtitle is invalid', () => {
            const chatBar = getChatBar();

            testDelete(chatBar.subtitle, 'text');

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when button.title is not defined', () => {
            const chatBar = getChatBar();

            testDelete(chatBar.button, 'title');

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when button.title is invalid', () => {
            const chatBar = getChatBar();

            testDelete(chatBar.button.title, 'text');

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when button.directives is not defined', () => {
            const chatBar = getChatBar();

            testDelete(chatBar.button, 'directives');

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });

        it('returns false when button.directives is empty', () => {
            const chatBar = getChatBar();

            chatBar.button.directives = [];

            expect(isValidChatBarMetadata(chatBar)).toBeFalsy();
        });
    });

    describe('#isHiddenChat', () => {
        function mockServiceId(serviceId) {
            mockGetGlobalParam.mockImplementation((param) => {
                switch (param) {
                    case 'serviceId':
                        return serviceId;
                    case 'backendConfig':
                        return {
                            hidden_namespaces: [
                                services.first.ns,
                                services.second.ns,
                            ],
                        };
                    default:
                        return undefined;
                }
            });
        }

        it('on first service we show all chats with only him chats', () => {
            mockServiceId(services.first.id);
            expect(isHiddenChatById(services.first.chatId)).toBeFalsy();
            expect(isHiddenChatById(services.second.chatId)).toBeTruthy();
            expect(isHiddenChatById(services.other.chatId)).toBeFalsy();
            expect(isHiddenChatById(services.first.channelId)).toBeFalsy();
        });

        it('on second service we show all chats with only him chats', () => {
            mockServiceId(services.second.id);
            expect(isHiddenChatById(services.first.chatId)).toBeTruthy();
            expect(isHiddenChatById(services.second.chatId)).toBeFalsy();
            expect(isHiddenChatById(services.other.chatId)).toBeFalsy();
            expect(isHiddenChatById(services.first.channelId)).toBeFalsy();
        });

        it('on other service we show all chats without hidden chats', () => {
            mockServiceId(services.other.id);
            expect(isHiddenChatById(services.first.chatId)).toBeTruthy();
            expect(isHiddenChatById(services.second.chatId)).toBeTruthy();
            expect(isHiddenChatById(services.other.chatId)).toBeFalsy();
            expect(isHiddenChatById(services.first.channelId)).toBeFalsy();
        });
    });
});
