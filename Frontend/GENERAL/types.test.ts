import { isStVersionConstructorParams, isStVersionClientParams, isIssueData } from './types';

describe('st-api types', () => {
    it('isIssueData()', async() => {
        try {
            isIssueData('string');
        } catch (e) {
            expect(e.message).toContain('not object.');
        }

        try {
            isIssueData({});
        } catch (e) {
            expect(e.message).toContain('should have key.');
        }
    });

    it('isStVersionConstructorParams()', async() => {
        try {
            isStVersionConstructorParams('string');
        } catch (e) {
            expect(e.message).toContain('not object.');
        }

        try {
            isStVersionConstructorParams({});
        } catch (e) {
            expect(e.message).toContain('should have token.');
        }

        try {
            isStVersionConstructorParams({ token: 'token' });
        } catch (e) {
            expect(e.message).toContain('should have queue.');
        }
    });

    it('isStVersionClientParams()', async() => {
        try {
            isStVersionClientParams('string');
        } catch (e) {
            expect(e.message).toContain('not object.');
        }

        try {
            isStVersionClientParams({});
        } catch (e) {
            expect(e.message).toContain('should have issues.');
        }

        try {
            isStVersionClientParams({ issues: [] });
        } catch (e) {
            expect(e.message).toContain('should have npmName.');
        }

        try {
            isStVersionClientParams({ issues: [], npmName: '' });
        } catch (e) {
            expect(e.message).toContain('should have npmVersion.');
        }

        try {
            isStVersionClientParams({ issues: [], npmName: '', npmVersion: '' });
        } catch (e) {
            expect(e.message).toContain('should have npmVersion.');
        }
    });
});
