const {getCurrentBranchName} = require('../utils');

const {
    assertPackages,
    assertVersion,
    assertTicket,
    assertBranchNameByName,
} = require('.');

describe('Assertions', () => {
    describe('Args', () => {
        test('ticket', () => {
            expect(assertTicket('MARKETFRAMEWORK-123')).toBe();
            expect(() => assertTicket()).toThrow();
            expect(() => assertTicket('MARKETFRONT-123')).toThrow();
        });

        test('version', () => {
            expect(assertVersion(['canary'])).toBe();
            expect(() => assertVersion()).toThrow();
            expect(() => assertVersion(['canary1'])).toThrow();
        });

        test('packages', () => {
            expect(assertPackages(['b2b'])).toBe();
            expect(() => assertPackages()).toThrow();
            expect(() => assertPackages(['b2c'])).toThrow();
        });
    });

    describe('Repo', () => {
        test('trunk', () => {
            const currentBranchName = getCurrentBranchName();
            if (currentBranchName === 'trunk') {
                expect(assertBranchNameByName('trunk')).toBe();
                expect(() => assertBranchNameByName('hoho')).toThrow();
            } else {
                expect(assertBranchNameByName(currentBranchName)).toBe();
                expect(() => assertBranchNameByName('hoho')).toThrow();
            }
        });
    });
});
