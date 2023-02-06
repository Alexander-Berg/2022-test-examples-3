import { makeUrl } from '../makeUrl';

describe('routes/makeUrl', () => {
    it('makes url with empty value params', () => {
        expect(makeUrl('service/search', { service: 'direct' })).toBe('/service/direct/search');
    });

    it('makes url with not empty value params', () => {
        expect(makeUrl('external/idm', { service: 'direct', login: 'leka', role: 'admin' })).toBe('https://idm.yandex-team.ru/system/modadverttest/roles#rf=1,rf-role=fPB0xnrj#leka@modadvert/direct/admin');
    });

    it('makes url with query', () => {
        expect(makeUrl('service/search', { service: 'direct' }, { viewMode: 'compact' })).toBe('/service/direct/search?viewMode=compact');
    });
});
