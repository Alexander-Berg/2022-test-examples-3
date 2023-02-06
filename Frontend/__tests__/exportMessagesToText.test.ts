import { exportMessagesToText } from '../exportMessagesToText';

describe('exportMessagesToText', () => {
    it('Should return messages as text', () => {
        const messages = [{
            timestamp: 1571669149,
            date: '21 october 2019',
            time: '17:45',
            author: 'John Doe',
            text: 'Message text 1',
        }, {
            timestamp: 1572522921,
            date: '31 october 2019',
            time: '14:55',
            author: 'John Doe',
            text: 'Message text 2',
        }, {
            timestamp: 1572522921,
            date: '31 october 2019',
            time: '14:56',
            author: 'John Doe',
            text: 'Message text 3',
        }, {
            timestamp: 1572526283,
            date: '31 october 2019',
            time: '14:59',
            author: 'Jane Doe',
            text: 'Message text 4',
            forwarded: {
                author: 'John Doe',
                text: 'Message text 3',
            },
        }];

        const messagesAsText = ([
            '21 october 2019',
            '',
            'John Doe, 17:45:',
            'Message text 1',
            '',
            '31 october 2019',
            '',
            'John Doe, 14:55:',
            'Message text 2',
            '',
            'Message text 3',
            '',
            'Jane Doe, 14:59:',
            '→ John Doe: Message text 3',
            'Message text 4',
        ]).join('\n');

        expect(exportMessagesToText(messages)).toBe(messagesAsText);
    });

    it('Forward without text', () => {
        const messages = [{
            timestamp: 1572526283,
            date: '31 october 2019',
            time: '14:59',
            author: 'Jane Doe',
            text: '',
            forwarded: {
                author: 'John Doe',
                text: 'Message text 3',
            },
        }];

        const messagesAsText = ([
            '31 october 2019',
            '',
            'Jane Doe, 14:59:',
            '→ John Doe: Message text 3',
        ]).join('\n');

        expect(exportMessagesToText(messages)).toBe(messagesAsText);
    });
});
