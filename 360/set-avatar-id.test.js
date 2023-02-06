'use strict';

jest.mock('../../_helpers/get-avatar-id.js', () => (contact) => `id-${contact.name}`);

const setAvatarId = require('./set-avatar-id.js');

test('setAvatarId', () => {
    const contacts = [ { name: 'x' } ];
    setAvatarId(contacts);
    expect(contacts).toEqual([ {
        name: 'x',
        avatarId: 'id-x'
    } ]);
});

test('setAvatarId no contacts', () => {
    expect(() => setAvatarId()).not.toThrow();
});
