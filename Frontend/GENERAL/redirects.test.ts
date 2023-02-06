import { getRedirect } from './redirects';

describe('getRedirect()', () => {
    it('should redirect slug style prof spheres to professions query', () => {
        expect(getRedirect({ pathname: '/jobs/vacancies/testing' }))
            .toEqual('/jobs/vacancies?professions=tester&professions=quality-manager');
    });

    it('should redirect slug prof spheres and rewrite cities', () => {
        expect(getRedirect({ pathname: '/jobs/vacancies/testing', query: { cities: ['0', '213'] } }))
            .toEqual('/jobs/vacancies?professions=tester&professions=quality-manager&cities=remote&cities=moscow');
    });

    it('should redirect slug style vacancy urls', () => {
        expect(getRedirect({ pathname: '/jobs/vacancies/analytics/an_loyal_prog_team/' }))
            .toEqual('/jobs/vacancies/5066');
    });

    it('should redirect and preserve cities query', () => {
        expect(getRedirect({ pathname: '/jobs/vacancies/analytics/an_loyal_prog_team/', query: { cities: '213' } }))
            .toEqual('/jobs/vacancies/5066?cities=moscow');
    });

    it('should redirect static pages', () => {
        expect(getRedirect({ pathname: '/jobs/about' }))
            .toEqual('/jobs/pages/about');
    });

    it('should redirect static pages with slash at end', () => {
        expect(getRedirect({ pathname: '/jobs/about/' }))
            .toEqual('/jobs/pages/about');
    });

    it('should not preserve query params at all', () => {
        expect(getRedirect({ pathname: '/jobs/about', query: { a: '1', b: '2', c: ['3'] } }))
            .toEqual('/jobs/pages/about');
    });

    it('should preserve utm query params', () => {
        expect(getRedirect({ pathname: '/jobs/about', query: { utm_source: 'test' } }))
            .toEqual('/jobs/pages/about?utm_source=test');
    });

    it('should preserve multiple values for one utm key', () => {
        expect(getRedirect({ pathname: '/jobs/about', query: { utm_campaign: ['value1', 'value2'] } }))
            .toEqual('/jobs/pages/about?utm_campaign=value1&utm_campaign=value2');
    });

    it('should redirect and preserve both cities and utm query', () => {
        expect(getRedirect({
            pathname: '/jobs/vacancies/analytics/an_loyal_prog_team/',
            query: { utm_source: 'tg', cities: '213' },
        })).toEqual('/jobs/vacancies/5066?cities=moscow&utm_source=tg');
    });

    it('should redirect with omitting .xml at end of url', () => {
        expect(getRedirect({ pathname: '/jobs/vacancies/analytics/an_loyal_prog_team.xml' }))
            .toEqual('/jobs/vacancies/5066');
    });

    it('should redirect with omitting .xml and with slash at end of url', () => {
        expect(getRedirect({ pathname: '/jobs/vacancies/analytics/an_loyal_prog_team.xml/' }))
            .toEqual('/jobs/vacancies/5066');
    });

    it('should redirect /jobs/vacancies?cities=remote -> /jobs/vacancies?employment_types=remote', () => {
        expect(getRedirect({
            pathname: '/jobs/vacancies/',
            query: { foo: 'bar', cities: ['moscow', 'remote'], employment_types: ['intern'] },
        }))
            .toEqual('/jobs/vacancies?foo=bar&cities=moscow&employment_types=intern&employment_types=remote');
    });

    it('should redirect /jobs/vacancies?pro_levels=intern -> /jobs/vacancies?employment_types=intern', () => {
        expect(getRedirect({
            pathname: '/jobs/vacancies/',
            query: { foo: 'bar', pro_levels: ['intern', 'junior'], employment_types: ['remote'] },
        }))
            .toEqual('/jobs/vacancies?foo=bar&pro_levels=junior&employment_types=remote&employment_types=intern');
    });

    it('should redirect /jobs/locations/remote?cities=moscow -> /jobs/vacancies?employment_types=remote&cities=moscow', () => {
        expect(getRedirect({
            pathname: '/jobs/locations/remote',
            query: { cities: 'moscow' },
        }))
            .toEqual('/jobs/vacancies?cities=moscow&employment_types=remote');
    });
});
