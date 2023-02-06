import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import Slug from '../../../interfaces/Slug';
import Direction from '../../../interfaces/Direction';
import {FilterTransportType} from '../../transportType';
import IPopularDirectionPoint from '../../../interfaces/state/popularDirections/IPopularDirectionPoint';

import getCanonicalPopularDirectionUrl from '../getCanonicalPopularDirectionUrl';

const tld = Tld.ru;
const language = Lang.ru;
const slug: Slug = 'yekaterinburg' as Slug;

const point = {
    directionTitle: 'в Нижний Тагил',
    title: 'Нижний Тагил',
    id: 11168,
    key: 'c11168',
    slug: 'nizhniy-tagil',
} as IPopularDirectionPoint;

describe('getCanonicalPopularDirectionUrl', () => {
    it('Вернет ссылку на поиск всеми видами транспорта', () => {
        const params = {
            point,
            direction: Direction.from,
            slug,
            language,
            tld,
        };

        expect(getCanonicalPopularDirectionUrl(params)).toBe(
            '/all-transport/yekaterinburg--nizhniy-tagil',
        );
    });

    it('Вернет ссылку на поиск конкретным видом транспорта', () => {
        const params = {
            point: {...point, transportType: FilterTransportType.bus},
            direction: Direction.from,
            slug,
            language,
            tld,
        };

        expect(getCanonicalPopularDirectionUrl(params)).toBe(
            '/bus/yekaterinburg--nizhniy-tagil',
        );
    });

    it('Вернет ссылку с изммененным слагом', () => {
        const params = {
            point: {...point, innerSlug: 'ekaterinburg' as Slug},
            direction: Direction.from,
            slug,
            language,
            tld,
        };

        expect(getCanonicalPopularDirectionUrl(params)).toBe(
            '/all-transport/ekaterinburg--nizhniy-tagil',
        );
    });

    it('Вернет ссылку с изммененным слагом для направления "В"', () => {
        const params = {
            point: {...point, innerSlug: 'ekaterinburg' as Slug},
            direction: Direction.to,
            slug,
            language,
            tld,
        };

        expect(getCanonicalPopularDirectionUrl(params)).toBe(
            '/all-transport/nizhniy-tagil--ekaterinburg',
        );
    });
});
