const users = require('../users');

const expectUser = user => {
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
            expect(users[key].login).toBe(key);
        });
    });

    it('содержит только пользователей со всеми обязательными полями', () => {
        Object.keys(users).forEach(key => {
            expectUser(users[key]);
        });
    });
});

module.exports = {
    expectUser,
};
