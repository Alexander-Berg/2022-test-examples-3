import Slug from '../../../interfaces/Slug';

import getLastochkaUrl from '../getLastochkaUrl';

const from = 'slug1' as Slug;
const to = 'slug2' as Slug;

describe('getLastochkaUrl', () => {
    it('Должна вернуться ссылка соответствующая параметрам', () => {
        expect(getLastochkaUrl(from, to)).toBe(`/lastochka/${from}--${to}`);

        expect(getLastochkaUrl(from, to, 'yandex.ru')).toBe(
            `yandex.ru/lastochka/${from}--${to}`,
        );
    });
});
