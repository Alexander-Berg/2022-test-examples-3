'use strict';

const getAvatarData = require('./get-avatar-data.js');

test('getAvatarData', () => {
    const contacts = [
        { avatarId: 'a', email: 'e@m' },
        { avatarId: 'a', email: 'skip@me' },
        { avatarId: 'b' },
        { email: 'skip@me' }
    ];
    expect(getAvatarData(contacts)).toEqual({
        parsed: JSON.stringify([ 'a', 'b' ]),
        items: {
            a: { local: 'e', domain: 'm' }
        }
    });
});

test('getAvatarData not array', () => {
    expect(getAvatarData()).toEqual({
        parsed: '[]',
        items: {}
    });
});
