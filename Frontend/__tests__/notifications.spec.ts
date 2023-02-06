import {
    shouldShowNewAssignedChatNotification,
    shouldShowNewOpenChatNotification,
    shouldShowNewMessageNotification,
    getNewAssignedChatNotification,
    getNewOpenChatNotification,
    getNewMessageNotification,
} from '../notifications';

import * as faker from '../../jest/faker';

import { createState, createCurrentOperatorAppState, createOperatorsState } from '../../jest/state';
import i18n from '../../i18n';

let appState;
let currentOperator;
let anotherOperator;

describe('selectors/notifications', () => {
    beforeEach(() => {
        currentOperator = faker.operator();
        anotherOperator = faker.operator();

        appState = createState({
            operators: createOperatorsState(currentOperator, anotherOperator),
            app: createCurrentOperatorAppState(currentOperator.uid),
        });
    });

    describe('shouldShowNewAssignedChatNotification', () => {
        it('returns true for "switch_operator" "assigned" message for the current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'switch_operator',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [currentOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(true);
        });

        it('returns false for "switch_operator" "assigned" message for non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'switch_operator',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [anotherOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for "switch_operator" non-"assigned" message for current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'switch_operator',
                result: faker.queueItem({
                    status: 'open',
                    members: [currentOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns true for "change_queue_status" "assigned" message for the current operator initiated by non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [currentOperator.uid],
                }),
                meta: {
                    initiator_uid: anotherOperator.uid,
                },
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(true);
        });

        it('returns true for "change_queue_status" "assigned" message for the current operator initiated by current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [currentOperator.uid],
                }),
                meta: {
                    initiator_uid: currentOperator.uid,
                },
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });
        it('returns false for "change_queue_status" "assigned" message for non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [anotherOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for "change_queue_status" non-"assigned" message for current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'open',
                    members: [currentOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns true for "new_chat" "assigned" message for the current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_chat',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [currentOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(true);
        });

        it('returns false for "new_chat" "assigned" message for non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_chat',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [anotherOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for "new_chat" non-"assigned" message for current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_chat',
                result: faker.queueItem({
                    status: 'open',
                    members: [currentOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for message of other than "switch_operator", "change_queue_status" or "new_chat" method', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_message',
                result: faker.queueItem({
                    status: 'assigned',
                    members: [currentOperator.uid],
                }),
            });

            expect(shouldShowNewAssignedChatNotification(
                appState,
                message,
            )).toBe(false);
        });
    });

    describe('shouldShowNewOpenChatNotification', () => {
        it('returns true for "change_queue_status" message with "open" status initiated ' +
                'by non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'open',
                }),
                meta: {
                    initiator_uid: anotherOperator.uid,
                },
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message,
            )).toBe(true);
        });

        it('returns false for other than "change_queue_status" message with "open" status initiated ' +
            'by non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'switch_operator',
                result: faker.queueItem({
                    status: 'open',
                }),
                meta: {
                    initiator_uid: anotherOperator.uid,
                },
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for "change_queue_status" message with "open" status initiated ' +
            'by current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'open',
                }),
                meta: {
                    initiator_uid: currentOperator.uid,
                },
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns true for "new_chat" message with "open" status', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_chat',
                result: faker.queueItem({
                    status: 'open',
                }),
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message,
            )).toBe(true);
        });

        it('returns false for other than "new_chat" message with "open" status', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'switch_operator',
                result: faker.queueItem({
                    status: 'open',
                }),
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for with other than "open" status', () => {
            const message1 = faker.notifyingSyncStateMessage({
                method: 'change_queue_status',
                result: faker.queueItem({
                    status: 'assigned',
                }),
                meta: {
                    initiator_uid: anotherOperator.uid,
                },
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message1,
            )).toBe(false);

            const message2 = faker.notifyingSyncStateMessage({
                method: 'new_chat',
                result: faker.queueItem({
                    status: 'assigned',
                }),
            });

            expect(shouldShowNewOpenChatNotification(
                appState,
                message2,
            )).toBe(false);
        });
    });

    describe('shouldShowNewMessageNotification', () => {
        it('returns true for "new_message" message with "assigned" status initiated' +
                ' by non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_message',
                result: faker.queueItem({
                    members: [currentOperator.uid],
                    status: 'assigned',
                }),
            });

            expect(shouldShowNewMessageNotification(
                appState,
                message,
            )).toBe(true);
        });

        it('returns false for message with other than "new_message" method', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_chat',
                result: faker.queueItem({
                    members: [currentOperator.uid],
                    status: 'assigned',
                }),
            });

            expect(shouldShowNewMessageNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for message with other than "assigned" status', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_message',
                result: faker.queueItem({
                    members: [currentOperator.uid],
                    status: 'open',
                }),
            });

            expect(shouldShowNewMessageNotification(
                appState,
                message,
            )).toBe(false);
        });

        it('returns false for message initiated from chat without non-current operator', () => {
            const message = faker.notifyingSyncStateMessage({
                method: 'new_message',
                result: faker.queueItem({
                    members: [anotherOperator.uid],
                    status: 'assigned',
                }),
            });

            expect(shouldShowNewMessageNotification(
                appState,
                message,
            )).toBe(false);
        });
    });

    describe('getNewAssignedChatNotification', () => {
        it('returns "new dialog chat" text with correct operator name for message with meta', () => {
            const message = faker.notifyingSyncStateMessage({
                meta: faker.meta(anotherOperator.uid),
            });

            expect(getNewAssignedChatNotification(
                message,
                appState,
            )).toBe(
                i18n.t('notification.new_assigned_chat', {
                    name: anotherOperator.name,
                }),
            );
        });

        it('returns "new dialog chat" text with default name for message without meta', () => {
            const message = faker.notifyingSyncStateMessage({
                meta: undefined,
            });

            expect(getNewAssignedChatNotification(
                message,
                appState,
            )).toBe(
                i18n.t('notification.new_assigned_chat', {
                    name: i18n.t('notification.abstract_name'),
                }),
            );
        });
    });

    describe('getNewOpenChatNotification', () => {
        it('returns "new queue chat" text with correct last message text for message with last_message', () => {
            const lastMessage = faker.message();
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: lastMessage,
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat', { message: lastMessage.text }));
        });

        it('returns "new queue chat" text with default text for message without last_message', () => {
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: undefined,
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat_default'));
        });

        it('returns "new queue chat" text with default text for message where last_message text is null', () => {
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: { text: null },
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat_default'));
        });

        it('returns "new queue chat" text with correct message type where last_message text is null and type is image', () => {
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: { text: null, type: 'image' },
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat', {
                    message: i18n.t('message.type.image'),
                }));
        });

        it('returns "new queue chat" text with correct message type where last_message text is null and type is file', () => {
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: { text: null, type: 'file' },
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat', {
                    message: i18n.t('message.type.file'),
                }));
        });

        it('returns "new queue chat" text with correct message type where last_message text is null and type is forward', () => {
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: { text: null, type: 'forward' },
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat', {
                    message: i18n.t('message.type.forward'),
                }));
        });

        it('returns "new queue chat" text with correct message type where last_message text is null and type is sticker', () => {
            const message = faker.notifyingSyncStateMessage({
                result: faker.queueItem({
                    last_message: { text: null, type: 'sticker' },
                }),
            });

            expect(getNewOpenChatNotification(message))
                .toBe(i18n.t('notification.new_open_chat', {
                    message: i18n.t('message.type.sticker'),
                }));
        });
    });

    describe('getNewMessageNotification', () => {
        it('returns "new message" text with correct client name and last message text' +
            ' for message with last_message', () => {
            const queueItem = faker.queueItem();
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message', {
                        client: queueItem.clientName,
                        message: queueItem.last_message.text,
                    }),
                );
        });

        it('returns "new message" text with correct client name only for message without last_message', () => {
            const queueItem = faker.queueItem({
                last_message: undefined,
            });
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message_text_default', {
                        client: queueItem.clientName,
                    }),
                );
        });

        it('returns "new message" text with correct client name only for message where last_message text is null', () => {
            const queueItem = faker.queueItem({
                last_message: { text: null },
            });
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message_text_default', {
                        client: queueItem.clientName,
                    }),
                );
        });

        it('returns "new message" text with correct client name and message type where last_message text is null and type is image', () => {
            const queueItem = faker.queueItem({
                last_message: { text: null, type: 'image' },
            });
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message', {
                        client: queueItem.clientName,
                        message: i18n.t('message.type.image'),
                    }),
                );
        });

        it('returns "new message" text with correct client name and message type where last_message text is null and type is file', () => {
            const queueItem = faker.queueItem({
                last_message: { text: null, type: 'file' },
            });
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message', {
                        client: queueItem.clientName,
                        message: i18n.t('message.type.file'),
                    }),
                );
        });

        it('returns "new message" text with correct client name and message type where last_message text is null and type is forward', () => {
            const queueItem = faker.queueItem({
                last_message: { text: null, type: 'forward' },
            });
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message', {
                        client: queueItem.clientName,
                        message: i18n.t('message.type.forward'),
                    }),
                );
        });

        it('returns "new message" text with correct client name and message type where last_message text is null and type is sticker', () => {
            const queueItem = faker.queueItem({
                last_message: { text: null, type: 'sticker' },
            });
            const message = faker.notifyingSyncStateMessage({
                result: queueItem,
            });

            expect(getNewMessageNotification(message))
                .toBe(
                    i18n.t('notification.new_message', {
                        client: queueItem.clientName,
                        message: i18n.t('message.type.sticker'),
                    }),
                );
        });
    });
});
