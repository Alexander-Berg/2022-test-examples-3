import * as validator from './validator';

describe('validator admins', () => {
    it('checks undefined/null', () => {
        expect(validator.admins({})).toBe(undefined);
        expect(validator.admins({ adminIds: null })).toBe(undefined);
    });

    it('checks empty array', () => {
        expect(validator.admins({ adminIds: [] })).toBe(undefined);
    });

    it('checks valid admins format', () => {
        expect(validator.admins({ adminIds: [1, '2', 100500] })).toEqual([1, 2, 100500]);
    });

    it('fails non array', () => {
        expect(() => validator.admins({ adminIds: 0 })).toThrow();
    });

    it('fails invalid value', () => {
        expect(() => validator.admins({ adminIds: ['string'] })).toThrow();
        expect(() => validator.admins({ adminIds: [3.14] })).toThrow();
        expect(() => validator.admins({ adminIds: [1, 2, 0] })).toThrow();
        expect(() => validator.admins({ adminIds: [-3] })).toThrow();
    });
});

describe('validator members', () => {
    it('checks undefined/null', () => {
        expect(validator.members({})).toBe(undefined);
        expect(validator.members({ members: null })).toBe(undefined);
    });

    it('checks empty array', () => {
        expect(validator.members({ members: [] })).toBe(undefined);
    });

    it('checks valid members format', () => {
        expect(validator.members({ members: [{ type: 'user', id: '1' }] })).toEqual([{ type: 'user', id: 1 }]);
    });

    it('fails non array', () => {
        expect(() => validator.members({ members: 0 })).toThrow();
    });

    it('fails invalid value', () => {
        expect(() => validator.members({ members: [{ type: 'wtf', id: 1 }] })).toThrow();
        expect(() => validator.members({ members: [{ type: 'user', id: -1 }] })).toThrow();
        expect(() => validator.members({ members: [{ a: 1 }] })).toThrow();
    });
});
