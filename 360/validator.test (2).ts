import * as validator from './validator';

describe('validator name', () => {
    it('checks undefined/null', () => {
        expect(validator.name({})).toBe(undefined);
        expect(validator.name({ name: null })).toBe(undefined);
    });

    it('checks name', () => {
        const name = {
            first: 'f',
            last: 'l',
            middle: 'm',
        };
        const res = validator.name({ name });

        expect(res).toBe(name);
    });

    it('fails non object', () => {
        expect(() => validator.name({ name: 0 })).toThrow();
    });

    it('fails without first name', () => {
        expect(() => validator.name({ name: { last: 'l' } })).toThrow();
    });

    it('fails without last name', () => {
        expect(() => validator.name({ name: { first: 'f' } })).toThrow();
    });
});

describe('validator gender', () => {
    it('checks undefined/null', () => {
        expect(validator.gender({})).toBe(undefined);
        expect(validator.gender({ gender: null })).toBe(null);
    });

    it('checks empty string', () => {
        expect(validator.gender({ gender: '' })).toBe(null);
    });

    it('checks valid gender', () => {
        expect(validator.gender({ gender: 'male' })).toBe('male');
        expect(validator.gender({ gender: 'female' })).toBe('female');
    });

    it('fails non string', () => {
        expect(() => validator.gender({ gender: 0 })).toThrow();
    });

    it('fails invalid value', () => {
        expect(() => validator.gender({ gender: 'other' })).toThrow();
    });
});

describe('validator birthday', () => {
    it('checks undefined/null', () => {
        expect(validator.birthday({})).toBe(undefined);
        expect(validator.birthday({ birthday: null })).toBe(null);
    });

    it('checks empty string', () => {
        expect(validator.birthday({ birthday: '' })).toBe(null);
    });

    it('checks valid birthday format', () => {
        expect(validator.birthday({ birthday: '2021-02-01' })).toBe('2021-02-01');
    });

    it('fails non string', () => {
        expect(() => validator.birthday({ birthday: 0 })).toThrow();
    });

    it('fails invalid value', () => {
        expect(() => validator.birthday({ birthday: 'other' })).toThrow();
    });
});

describe('validator contacts', () => {
    it('checks undefined/null', () => {
        expect(validator.contacts({})).toBe(undefined);
        expect(validator.contacts({ contacts: null })).toBe(undefined);
    });

    it('checks empty array', () => {
        expect(validator.contacts({ contacts: [] })).toBe(undefined);
    });

    it('checks valid contacts format', () => {
        expect(validator.contacts({ contacts: [{
            type: 'phone',
            value: '+7123',
        }] })).toEqual([{
            type: 'phone',
            value: '+7123',
        }]);
    });

    it('fails non array', () => {
        expect(() => validator.contacts({ contacts: 0 })).toThrow();
    });

    it('fails invalid value', () => {
        expect(() => validator.contacts({ contacts: [{ a: 42 }] })).toThrow();
    });
});
