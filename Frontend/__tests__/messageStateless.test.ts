import { CallInfoStatus } from '../../constants/call';
import {
    isPlainMessageStateless,
    isSystemMessageStateless,
    getSystemMessageContentTypeStateless,
    getFormatedMessageTimeStateless,
    getMessageNameStateless,
    getMessageTextStateless,
    getDownloadUrlStateless,
    getFileNameStateless,
    getFileInfoStateless,
} from '../messageStateless';

import i18n from '../../../shared/lib/i18n';
import { getShortTime, getHhMm } from '../../lib/Date';
import { MessageType, SystemMessageContentType } from '../../constants/message';
import { UsersState } from '../../store/users';
import { ModerationActions } from '../../constants/fanout';

import * as en from '../../../langs/yamb/en.json';
import * as ru from '../../../langs/yamb/ru.json';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { createGalleryData, createTextData } from '../../helpers/messages';
import { generateGuid } from '../../store/__tests__/mock/common';

const PRIVATE_CHAT_ID = '00000000-0000-0000-0000-000000000000_00000000-0000-0000-0000-000000000000';

const storeEmptyUsers: UsersState = {};

const emptyUserInfo: APIv3.UserInfo = {
    display_name: '',
    guid: '',
    version: 0,
};

const emptyMessage: APIv3.Message = {
    type: MessageType.UNKNOWN,
    chatId: '',
    deleted: false,
    from: emptyUserInfo,
    messageId: '',
    timestamp: 0,
};

const emptyFileInfo: APIv3.FileInfo = {
    name: '',
    size: 0,
};

const emptyImage: APIv3.Image = {
    width: 0,
    height: 0,
    file_info: emptyFileInfo,
};

const emptyFile: APIv3.File = {
    file_info: emptyFileInfo,
};

const emptySticker: APIv3.Sticker = {
    id: '0',
    set_id: '0',
};

const emptyCard: APIv3.Card = {
    data: {},
};

const canceledCallInfo: APIv3.CallInfo = {
    callGuid: '',
    status: CallInfoStatus.CANCELED,
    duration: 0,
};

describe('MessageStatelessSelectors', () => {
    const usersMock = usersMockFactory();
    const users = usersMock.createState([usersMock.createAnonimous()()]);

    describe('#isPlainMessageStateless', () => {
        it('Should return true when message.type is PLAIN', () => {
            expect(isPlainMessageStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
            })).toEqual(true);
        });

        it('Should return false when message.type is SYSTEM', () => {
            expect(isPlainMessageStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
            })).toEqual(false);
        });

        it('Should return false when message.type is UNKNOWN', () => {
            expect(isPlainMessageStateless({
                ...emptyMessage,
                type: MessageType.UNKNOWN,
            })).toEqual(false);
        });
    });

    describe('#isSystemMessageStateless', () => {
        it('Should return false when message.type is PLAIN', () => {
            expect(isSystemMessageStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
            })).toEqual(false);
        });

        it('Should return true when message.type is SYSTEM', () => {
            expect(isSystemMessageStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
            })).toEqual(true);
        });

        it('Should return false when message.type is UNKNOWN', () => {
            expect(isSystemMessageStateless({
                ...emptyMessage,
                type: MessageType.UNKNOWN,
            })).toEqual(false);
        });
    });

    describe('#getSystemMessageContentTypeStateless', () => {
        it('Should return undefined when message.data is undefined', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: undefined,
            })).toEqual(undefined);
        });

        it('Should return CREATED when message.data has key created', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    created: {},
                },
            })).toEqual(SystemMessageContentType.CREATED);
        });

        it('Should return INFO_CHANGED when message.data has key info_changed', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    info_changed: {},
                },
            })).toEqual(SystemMessageContentType.INFO_CHANGED);
        });

        it('Should return MEMBERS_CHANGED when message.data has key members_changed', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    members_changed: {},
                },
            })).toEqual(SystemMessageContentType.MEMBERS_CHANGED);
        });

        it('Should return LEFT when message.data has key left', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    left: true,
                },
            })).toEqual(SystemMessageContentType.LEFT);
        });

        it('Should return JOINED when message.data has key joined', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    joined: true,
                },
            })).toEqual(SystemMessageContentType.JOINED);
        });

        it('Should return JOINED_BY_LINK when message.data has key joined_by_link', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    joined_by_link: true,
                },
            })).toEqual(SystemMessageContentType.JOINED_BY_LINK);
        });

        it('Should return CALL_INFO when message.data has key call_info', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    call_info: canceledCallInfo,
                },
            })).toEqual(SystemMessageContentType.CALL_INFO);
        });

        it('Should return GENERIC_MESSAGE_TEXT when message.data has key generic_message_text', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {
                    generic_message_text: '',
                },
            })).toEqual(SystemMessageContentType.GENERIC_MESSAGE_TEXT);
        });

        it('Should return undefined when message.data has no known key', () => {
            expect(getSystemMessageContentTypeStateless({
                ...emptyMessage,
                data: {} as unknown as any,
            })).toEqual(undefined);
        });
    });

    describe('#getFormatedMessageTimeStateless', () => {
        it('Should return value from getHhMm when relative is false', () => {
            const timestamp = Date.now() * 1000;

            expect(getFormatedMessageTimeStateless({
                ...emptyMessage,
                timestamp,
            })).toEqual(getHhMm(timestamp / 1000 / 1000));
        });

        it('Should return value from getShortTime when relative is false', () => {
            const timestamp = Date.now() * 1000;

            expect(getFormatedMessageTimeStateless({
                ...emptyMessage,
                timestamp,
            }, true)).toEqual(getShortTime(timestamp / 1000 / 1000));
        });
    });

    describe('#getMessageNameStateless', () => {
        it('Should return Daria when exists user.custom_display_name is Daria', () => {
            expect(getMessageNameStateless({
                ...emptyMessage,
                from: {
                    ...emptyUserInfo,
                    display_name: 'Yandex Team',
                    custom_display_name: 'Daria',
                    guid: 'my_test_guid',
                },
            }, {
                my_test_guid: {
                    ...emptyUserInfo,
                },
            })).toEqual('Daria');
        });

        it('Should return Fox when user.contact_name is Fox', () => {
            expect(getMessageNameStateless({
                ...emptyMessage,
                from: {
                    ...emptyUserInfo,
                    display_name: 'Mike',
                    guid: 'my_test_guid',
                },
            }, {
                my_test_guid: {
                    ...emptyUserInfo,
                    contact_name: 'Fox',
                    display_name: 'George',
                    guid: 'my_test_guid',
                },
            })).toEqual('Fox');
        });

        it('Should return George when user.contact_name is undefined and user.display_name is George', () => {
            expect(getMessageNameStateless({
                ...emptyMessage,
                from: {
                    ...emptyUserInfo,
                    display_name: 'Mike',
                    guid: 'my_test_guid',
                },
            }, {
                my_test_guid: {
                    ...emptyUserInfo,
                    display_name: 'George',
                    guid: 'my_test_guid',
                },
            })).toEqual('George');
        });

        it('Should return Mike when user is undefined and message.from.display_name is Mike', () => {
            expect(getMessageNameStateless({
                ...emptyMessage,
                from: {
                    ...emptyUserInfo,
                    display_name: 'Mike',
                },
            }, {})).toEqual('Mike');
        });

        it('Should return message.from.display_name when user.contact_name and user.display_name is empty', () => {
            expect(getMessageNameStateless({
                ...emptyMessage,
                from: {
                    display_name: 'Donald Dumb',
                    guid: 'my_test_guid',
                    version: 0,
                },
            }, {
                my_test_guid: {
                    ...emptyUserInfo,
                },
            })).toEqual('Donald Dumb');
        });

        it('Should return default username when message.from.display_name, user.contact_name and user.display_name is empty (ru)',
            () => {
                i18n.locale('en', en);
                expect(getMessageNameStateless({
                    ...emptyMessage,
                }, {
                    my_test_guid: {
                        ...emptyUserInfo,
                    },
                })).toEqual('User');
            });

        it('Should return default username when message.from.display_name, user.contact_name and user.display_name is empty (en)',
            () => {
                i18n.locale('ru', ru);
                expect(getMessageNameStateless({
                    ...emptyMessage,
                }, {
                    my_test_guid: {
                        ...emptyUserInfo,
                    },
                })).toEqual('Пользователь');
            });
    });

    describe('#getMessageTextStateless en', () => {
        const authId = generateGuid();

        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Should return deleted', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                deleted: true,
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Message deleted');
        });

        it('Should return moderation.hidden when moderated hidden', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.HIDE,
                data: createTextData('Text for test'),
            }, storeEmptyUsers, authId, { markup: false, hidden: true, withPollEmoji: false })).toEqual('Message hidden');
        });

        it('Should return text when moderated shown', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.HIDE,
                data: createTextData('Text for test'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Text for test');
        });

        it('Should return moderation.deleted', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.DELETE,
                data: createTextData('Text for test'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Message removed by moderator due to a violation of the rules.');
        });

        it('Should markup moderation.deleted', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.DELETE,
                data: createTextData('Text for test'),
            }, storeEmptyUsers, authId, { markup: true, hidden: false, withPollEmoji: false })).toEqual('Message removed by moderator due to a violation of <a href="moderationRulesUrl?undefined" class="link" target="_blank">the rules</a>.');
        });

        it('Should return text', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: createTextData('Text for test'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Text for test');
        });

        it('Should return image', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    image: emptyImage,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Image');
        });

        it('Should return file', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    file: emptyFile,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('File');
        });

        it('Should return sticker', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    sticker: emptySticker,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Sticker');
        });

        it('Should return card', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    card: emptyCard,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Card');
        });

        it('Should return forwarded', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                forwarded: [emptyMessage],
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('→ User: Unknown message format');
        });

        it('Should return fallback when message is unknown plain', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Unknown message format');
        });

        it('Should return created', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    created: {
                        name: 'New group name',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike created group “New group name”');
        });

        it('Should return info_changed name', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    info_changed: {
                        name: 'New group name',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike changed group name to “New group name”');
        });

        it('Should return info_changed description', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    info_changed: {
                        description: 'New group description',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike changed group description to “New group description”');
        });

        it('Should return info_changed avatar', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    info_changed: {
                        avatar_id: '',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike changed group image');
        });

        it('Should return info_changed fallback', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    info_changed: {},
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike updated group info');
        });

        it('Should return members_changed added & removed', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    members_changed: {
                        added_users: [
                            {
                                ...emptyUserInfo,
                                guid: '222',
                                display_name: 'Tom',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '333',
                                display_name: 'Bob',
                            },
                        ],
                        removed_users: [
                            {
                                ...emptyUserInfo,
                                guid: '444',
                                display_name: 'George',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '555',
                                display_name: 'Lisa',
                            },
                        ],
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike added: Tom, Bob and removed: George, Lisa');
        });

        it('Should return members_changed added', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    members_changed: {
                        added_users: [
                            {
                                ...emptyUserInfo,
                                guid: '222',
                                display_name: 'Tom',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '333',
                                display_name: 'Bob',
                            },
                        ],
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike added: Tom, Bob');
        });

        it('Should return members_changed removed', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    members_changed: {
                        removed_users: [
                            {
                                ...emptyUserInfo,
                                guid: '444',
                                display_name: 'George',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '555',
                                display_name: 'Lisa',
                            },
                        ],
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike removed: George, Lisa');
        });

        it('Should return members_changed fallback', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    members_changed: {},
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Unknown message format');
        });

        it('Should return left', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    left: true,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike left the group');
        });

        it('Should return joined', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    joined: true,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike joined');
        });

        it('Should return joined_by_link', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
                data: {
                    joined_by_link: true,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike joined via link');
        });

        it('Should return generic_message_text', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                data: {
                    generic_message_text: 'Hello',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Hello');
        });

        it('Should return fallback when message is unknown system (group chat)', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike updated group info');
        });

        it('Should return fallback when message is unknown system (private chat)', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                chatId: PRIVATE_CHAT_ID,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Mike updated chat info');
        });

        it('Should return fallback when message is unknown system (channel)', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                chatId: '1/0/',
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Mike',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Channel info updated');
        });

        it('Should return fallback when message is unknown', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Unknown message format');
        });
    });

    describe('#getMessageTextStateless ru', () => {
        const authId = generateGuid();

        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Should return deleted 1 message', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                deleted: true,
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Сообщение было удалено');
        });

        it('Should return moderation.hidden when moderated hidden', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.HIDE,
                data: createTextData('Text for test'),
            }, storeEmptyUsers, authId, { markup: false, hidden: true, withPollEmoji: false })).toEqual('Сообщение скрыто');
        });

        it('Should return text when moderated shown', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.HIDE,
                data: createTextData('Текст для теста'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Текст для теста');
        });

        it('Should return plain moderation.deleted', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.DELETE,
                data: createTextData('Текст для теста'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Сообщение удалено модератором в связи с нарушением правил сообщества.'); // tslint:disable-line:no-irregular-whitespace
        });

        it('Should markup moderation.deleted', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                moderationAction: ModerationActions.DELETE,
                data: createTextData('Текст для теста'),
            }, storeEmptyUsers, authId, { markup: true, hidden: false, withPollEmoji: false })).toEqual('Сообщение удалено модератором в связи с нарушением <a href="moderationRulesUrl?undefined" class="link" target="_blank">правил сообщества</a>.'); // tslint:disable-line:no-irregular-whitespace
        });

        it('Should return text', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: createTextData('Текст для теста'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Текст для теста');
        });

        it('Should return escaped html', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: createTextData('Текст&nbsp;для теста'),
            }, storeEmptyUsers, authId, { markup: true, hidden: false, withPollEmoji: false })).toEqual('Текст&amp;nbsp;для теста');
        });

        it('Should return original html', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: createTextData('Текст&nbsp;для теста'),
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Текст&nbsp;для теста');
        });

        it('Should return image', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    image: emptyImage,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Изображение');
        });

        it('Should return file', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    file: emptyFile,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Файл');
        });

        it('Should return sticker', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    sticker: emptySticker,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Стикер');
        });

        it('Should return card', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    card: emptyCard,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Карточка');
        });

        it('Should return forwarded', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                forwarded: [emptyMessage],
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('→ Пользователь: Неизвестный формат сообщения');
        });

        it('Should return fallback when message is unknown plain', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Неизвестный формат сообщения');
        });

        it('Should return created', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    created: {
                        name: 'Новое название группы',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай создал(а) чат «Новое название группы»');
        });

        it('Should return info_changed name', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    info_changed: {
                        name: 'Новое название группы',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай изменил(а) название чата на «Новое название группы»');
        });

        it('Should return info_changed description', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    info_changed: {
                        description: 'Новое описание группы',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай изменил(а) описание чата на «Новое описание группы»');
        });

        it('Should return info_changed avatar', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    info_changed: {
                        avatar_id: '',
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай изменил(а) изображение чата');
        });

        it('Should return call_info', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                data: {
                    call_info: canceledCallInfo,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Звонок отменён');
        });

        it('Should return info_changed fallback', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    info_changed: {},
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай обновил(а) информацию о чате');
        });

        it('Should return members_changed added & removed', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    members_changed: {
                        added_users: [
                            {
                                ...emptyUserInfo,
                                guid: '222',
                                display_name: 'Петр',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '333',
                                display_name: 'Василий',
                            },
                        ],
                        removed_users: [
                            {
                                ...emptyUserInfo,
                                guid: '444',
                                display_name: 'Максим',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '555',
                                display_name: 'Лиза',
                            },
                        ],
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай добавил(а): Петр, Василий и удалил(а): Максим, Лиза');
        });

        it('Should return members_changed added', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    members_changed: {
                        added_users: [
                            {
                                ...emptyUserInfo,
                                guid: '222',
                                display_name: 'Петр',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '333',
                                display_name: 'Василий',
                            },
                        ],
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай добавил(а): Петр, Василий');
        });

        it('Should return members_changed removed', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    members_changed: {
                        removed_users: [
                            {
                                ...emptyUserInfo,
                                guid: '444',
                                display_name: 'Максим',
                            },
                            {
                                ...emptyUserInfo,
                                guid: '555',
                                display_name: 'Лиза',
                            },
                        ],
                    },
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай удалил(а): Максим, Лиза');
        });

        it('Should return members_changed fallback', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    members_changed: {},
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Неизвестный формат сообщения');
        });

        it('Should return left', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    left: true,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай покинул(а) чат');
        });

        it('Should return joined', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    joined: true,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай присоединился(ась)');
        });

        it('Should return joined_by_link', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
                data: {
                    joined_by_link: true,
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай присоединился(ась) по ссылке');
        });

        it('Should return fallback when message is unknown system (group chat)', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай обновил(а) информацию о чате');
        });

        it('Should return fallback when message is unknown system (private chat)', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                chatId: PRIVATE_CHAT_ID,
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Николай обновил(а) информацию о чате');
        });

        it('Should return fallback when message is unknown system (channel)', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
                chatId: '1/0/',
                type: MessageType.SYSTEM,
                from: {
                    ...emptyUserInfo,
                    guid: '111',
                    display_name: 'Николай',
                },
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Обновлена информация о канале');
        });

        it('Should return fallback when message is unknown unknown', () => {
            expect(getMessageTextStateless({
                ...emptyMessage,
            }, users, authId, { markup: false, hidden: false, withPollEmoji: false })).toEqual('Неизвестный формат сообщения');
        });
    });

    describe('#getFileTransferStateless', () => {
        it('Should return file info for file', () => {
            expect(getFileInfoStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                messageId: '1',
                data: {
                    file: {
                        file_info: {
                            ...emptyFileInfo,
                            id: '2',
                        },
                    },
                },
            })).toMatchObject({
                ...emptyFileInfo,
                id: '2',
            });
        });

        it('Should return file info for image', () => {
            expect(getFileInfoStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                messageId: '1',
                data: {
                    image: {
                        ...emptyImage,
                        file_info: {
                            ...emptyFileInfo,
                            id: '2',
                        },
                    },
                },
            })).toMatchObject({
                ...emptyFileInfo,
                id: '2',
            });
        });

        it('Should return file info for image in gallery', () => {
            expect(getFileInfoStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                messageId: '1',
                data: createGalleryData([
                    {
                        ...emptyImage,
                        file_info: {
                            ...emptyFileInfo,
                            id: '2',
                        },
                    },
                    {
                        ...emptyImage,
                        file_info: {
                            ...emptyFileInfo,
                            id: '3',
                        },
                    },
                ]),
            }, 1)).toMatchObject({
                ...emptyFileInfo,
                id: '3',
            });
        });
    });

    describe('#getDownloadUrlStateless', () => {
        it('Should return url by file', () => {
            expect(getDownloadUrlStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                chatId: '1',
                data: {
                    file: {
                        file_info: {
                            ...emptyFileInfo,
                            id: '2',
                            name: '3',
                        },
                    },
                },
            })).toEqual('fileDownloadUrl?{"chatId":"1","fileId":"2","filename":"3"}');
        });

        it('Should return url by image', () => {
            expect(getDownloadUrlStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                chatId: '1',
                data: {
                    image: {
                        ...emptyImage,
                        file_info: {
                            ...emptyFileInfo,
                            id: '2',
                            name: '3',
                        },
                    },
                },
            })).toEqual('fileDownloadUrl?{"chatId":"1","fileId":"2","filename":"3"}');
        });

        it('Should fallback to empty string', () => {
            expect(getDownloadUrlStateless(emptyMessage)).toEqual('');
        });
    });

    describe('#getFileNameStateless', () => {
        it('Should return name by file', () => {
            expect(getFileNameStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    file: {
                        file_info: {
                            ...emptyFileInfo,
                            name: '2',
                        },
                    },
                },
            })).toEqual('2');
        });

        it('Should return name by image', () => {
            expect(getFileNameStateless({
                ...emptyMessage,
                type: MessageType.PLAIN,
                data: {
                    image: {
                        ...emptyImage,
                        file_info: {
                            ...emptyFileInfo,
                            name: '2',
                        },
                    },
                },
            })).toEqual('2');
        });

        it('Should fallback to empty string', () => {
            expect(getFileNameStateless(emptyMessage)).toEqual('');
        });
    });
});
