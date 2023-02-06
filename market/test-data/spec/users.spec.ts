import type {User} from 'spec/utils';

import users from '../users';

export const expectUser = (user: User) => {
    expect(user).toBeInstanceOf(Object);

    const {uid, login, password} = user;

    expect(uid).toEqual(expect.any(Number));
    expect(uid).toBeGreaterThan(0);

    expect(login).toMatch(/^[a-z.0-9-@]+$/i);

    expect(password).toMatch(/\S+/);
};

describe('users list', () => {
    it('имеет в качестве ключей только логины пользователей', () => {
        Object.keys(users).forEach(key => {
            // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
            expect(users[key].login).toBe(key);
        });
    });
    it('содержит только пользователей со всеми обязательными полями', () => {
        Object.keys(users).forEach(key => {
            // @ts-expect-error -- свойства clientId нет в типе User в ginny-helpers
            expectUser(users[key]);
        });
    });
});
