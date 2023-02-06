'use strict';

const { messageStatus, MESSAGE_STATUS } = require('./message-status.js');

const fakeLabels = [
    {
        lid: 'FAKE_SEEN_LBL',
        symbolicName: 'seen_label'
    },
    {
        lid: 'FAKE_ANSWERED_LBL',
        symbolicName: 'answered_label'
    },
    {
        lid: 4,
        symbolicName: 'forwarded_label'
    }
];

let message;

beforeEach(() => {
    message = {
        labels: []
    };
});

describe('отдает статусы из лейблов письма', () => {
    it('-> READ для seen_label', () => {
        message.labels = [ 'FAKE_SEEN_LBL' ];

        const result = messageStatus(message, fakeLabels);
        expect(result).toEqual([ MESSAGE_STATUS.READ ]);
    });

    it('-> UNREAD для !seen_label', () => {
        message.labels = [];

        const result = messageStatus(message, fakeLabels);
        expect(result).toEqual([ MESSAGE_STATUS.UNREAD ]);
    });

    it('-> ANSWERED для answered_label', () => {
        message.labels = [ 'FAKE_ANSWERED_LBL' ];
        expect(messageStatus(message, fakeLabels)).toEqual([
            MESSAGE_STATUS.UNREAD,
            MESSAGE_STATUS.ANSWERED
        ]);
    });

    it('-> FORWARDED для forwarded_label', () => {
        message.labels = [ 4 ];

        expect(messageStatus(message, fakeLabels)).toEqual([
            MESSAGE_STATUS.UNREAD,
            MESSAGE_STATUS.FORWARDED
        ]);
    });

    it('-> все кроме READ', () => {
        message.labels = [ 4, 'FAKE_SEEN_LBL', 'FAKE_ANSWERED_LBL' ];

        expect(messageStatus(message, fakeLabels)).toEqual([
            MESSAGE_STATUS.READ,
            MESSAGE_STATUS.ANSWERED,
            MESSAGE_STATUS.FORWARDED
        ]);
    });
});
