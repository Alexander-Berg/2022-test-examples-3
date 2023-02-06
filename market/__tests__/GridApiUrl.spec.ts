import GridApiUrl from '../api/GridApiUrl';

declare let global: NodeJS.Global & { window: Window };

describe('parse', () => {
    it('должен парсить пустую строку', () => {
        expect(GridApiUrl.parse('')).toEqual(
            new GridApiUrl('', {
                page: 0,
                size: 5,
            }),
        );
    });
    it('должен парсить строку содержащую только хост', () => {
        expect(GridApiUrl.parse('http://localhost:3000/')).toEqual(
            new GridApiUrl('', {
                page: 0,
                size: 5,
            }),
        );
    });
    it('должен парсить строку содержащую только путь', () => {
        expect(GridApiUrl.parse('/lms/partner-capacity')).toEqual(
            new GridApiUrl('/lms/partner-capacity', {
                page: 0,
                size: 5,
            }),
        );
    });
    it('должен подставлять дефолтные значения параметров запроса, если они не были переданы в строке', () => {
        const url = GridApiUrl.parse('http://localhost:3000/admin/lms/partner-capacity');
        expect(url).toHaveProperty('page', 0);
        expect(url).toHaveProperty('size', 5);
        expect(url).toHaveProperty('orderBy', undefined);
        expect(url).toHaveProperty('directOrder', undefined);
        expect(url.params).toEqual({ page: 0, size: 5 });
    });
    it('должен правильно парсить строку запроса', () => {
        expect(GridApiUrl.parse('http://localhost:3000/admin/lms/partner-capacity?page=1&size=5&partner=51')).toEqual(
            new GridApiUrl('/lms/partner-capacity', {
                page: 1,
                size: 5,
                partner: '51',
            }),
        );
    });
});

describe('fullUrl', () => {
    const { host } = window.location;

    beforeAll(() => {
        global.window = Object.create(window);
        Object.defineProperty(window, 'location', {
            value: {
                host: '',
            },
        });
    });

    afterAll(() => {
        window.location.host = host;
    });

    it('должен выдавать корректную ссылку, если передать путём пустую строку', () => {
        const url = new GridApiUrl('');
        expect(url.full).toBe('/admin?page=0&size=5');
    });
    it('должен одинаково обрабатывать путь с базой для апи и без', () => {
        const url = new GridApiUrl('/lms/partner-capacity');
        const url2 = new GridApiUrl('/admin/lms/partner-capacity');
        expect(url.full).toBe(url2.full);
    });
    it('должен подставлять дефолтные значения для полей page и size в запросе', () => {
        const url = new GridApiUrl('/lms/partner-capacity');
        expect(url.full).toBe('/admin/lms/partner-capacity?page=0&size=5');
    });
    it('должен формировать корректную строку запроса', () => {
        const url = new GridApiUrl('/lms/partner-capacity', { page: 1, size: 10, orderBy: 'partner' });
        expect(url.full).toBe('/admin/lms/partner-capacity?page=1&size=10&sort=partner%2Cdesc');
    });
});
