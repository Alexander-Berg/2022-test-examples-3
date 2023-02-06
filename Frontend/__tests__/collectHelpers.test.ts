import { cleanUrl } from '../collectHelpers';

describe('collectHelpers', () => {
    describe('cleanUrl', () => {
        it('should preserve url if cleaning is unnecessary', () => {
            expect(cleanUrl('/jobs/vacancies/developer'))
                .toEqual('/jobs/vacancies/developer');
        });

        it('should clean query string', () => {
            expect(cleanUrl('/jobs/vacancies?test=123'))
                .toEqual('/jobs/vacancies');
        });

        it('should clean slash at the end', () => {
            expect(cleanUrl('/jobs/vacancies/'))
                .toEqual('/jobs/vacancies');
        });

        it('should clean xml extension', () => {
            expect(cleanUrl('/jobs/vacancies/developer.xml'))
                .toEqual('/jobs/vacancies/developer');
        });

        it('should clean url from slash, query string and xml extension at the same time', () => {
            expect(cleanUrl('/jobs/vacancies/developer.xml/?test=123'))
                .toEqual('/jobs/vacancies/developer');
        });
    });
});
