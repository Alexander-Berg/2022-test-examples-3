import { getBasePagePath } from './getWithVacanciesPage';

describe('getWithVacanciesPage', () => {
    describe('getBasePagePath', () => {
        it('should preserve path if subpage is empty', () => {
            expect(getBasePagePath('/jobs/services/hr-tech', '')).toBe('/jobs/services/hr-tech');
        });

        it('should preserve path if subpage is not equal to last path part', () => {
            expect(getBasePagePath('/jobs/services/hr-tech/test', 'hr-tech')).toBe('/jobs/services/hr-tech/test');
        });

        it('should avoid end slash in a base path', () => {
            expect(getBasePagePath('/jobs/services/hr-tech/', '')).toBe('/jobs/services/hr-tech');
        });

        it('should avoid subpage in a base path', () => {
            expect(getBasePagePath('/jobs/services/market/contacts', 'contacts')).toBe('/jobs/services/market');
        });

        it('should avoid end slash with subpage in a base path at the same time', () => {
            expect(getBasePagePath('/jobs/services/hr-tech/about/', 'about')).toBe('/jobs/services/hr-tech');
        });
    });
});
