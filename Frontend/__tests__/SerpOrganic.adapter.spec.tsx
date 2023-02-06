import SerpOrganicAdapter from '../SerpOrganic.adapter';

describe('Адаптер SerpOrganicAdapter', () => {
    it('clearPathname не падает получая невалидный урл', () => {
        expect(SerpOrganicAdapter.clearPathname('%привет')).toEqual('%привет');
    });
});
