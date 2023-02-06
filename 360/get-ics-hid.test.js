'use strict';

const getIcsHid = require('./get-ics-hid.js');

test('работает без параметров', () => {
    expect(getIcsHid()).toBe(false);
});

test('работает без аттачей', () => {
    expect(getIcsHid({
        info: {}
    })).toBe(false);
});

test('работает с аттачами', () => {
    expect(getIcsHid({
        info: {
            attachments: []
        }
    })).toBe(false);
});

test('находит ics аттач', () => {
    expect(getIcsHid({
        info: {
            attachments: [
                {
                    hid: '1.2',
                    display_name: 'calendar.ics',
                    class: 'general',
                    narod: false,
                    size: 1111,
                    mime_type: 'text/calendar',
                    download_url: '',
                    is_inline: false,
                    content_id: ''
                },
                {
                    hid: '1.3',
                    display_name: 'image.jpg',
                    class: 'image',
                    narod: false,
                    size: 2222,
                    mime_type: 'image/jpeg',
                    download_url: '',
                    is_inline: false,
                    content_id: ''
                }
            ]
        }
    })).toBe('1.2');
});
