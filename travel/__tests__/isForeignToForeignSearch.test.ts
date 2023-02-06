import {BY, RU} from '../../countries';

import Slug from '../../../interfaces/Slug';
import Point from '../../../interfaces/Point';
import IPoint from '../../../interfaces/state/searchForm/IPoint';

import {isForeignToForeignSearch} from '../isForeignToForeignSearch';

const RUSSIAN_POINT: IPoint = {
    title: 'Москва',
    key: 'c213' as Point,
    slug: 'moscow' as Slug,
    country: {
        railwayTimezone: 'Europe/Moscow',
        code: RU,
        title: 'Россия',
    },
};

const FOREIGN_POINT_WITHOUT_COUNTRY: IPoint = {
    title: 'Минск',
    key: 'c157' as Point,
    slug: 'minsk' as Slug,
};

const FOREIGN_POINT: IPoint = {
    ...FOREIGN_POINT_WITHOUT_COUNTRY,
    country: {
        railwayTimezone: 'Europe/Minsk',
        code: BY,
        title: 'Беларусь',
    },
};

describe('isForeignToForeignSearch', () => {
    it('Если информации недостаточно - вернёт false', () => {
        expect(
            isForeignToForeignSearch(
                FOREIGN_POINT_WITHOUT_COUNTRY,
                FOREIGN_POINT,
            ),
        ).toBe(false);
        expect(
            isForeignToForeignSearch(
                FOREIGN_POINT,
                FOREIGN_POINT_WITHOUT_COUNTRY,
            ),
        ).toBe(false);
        expect(
            isForeignToForeignSearch(
                FOREIGN_POINT_WITHOUT_COUNTRY,
                FOREIGN_POINT_WITHOUT_COUNTRY,
            ),
        ).toBe(false);
    });

    it('Если хотя бы один пункт находится в РФ - вернёт false', () => {
        expect(isForeignToForeignSearch(RUSSIAN_POINT, FOREIGN_POINT)).toBe(
            false,
        );
        expect(isForeignToForeignSearch(FOREIGN_POINT, RUSSIAN_POINT)).toBe(
            false,
        );
        expect(isForeignToForeignSearch(RUSSIAN_POINT, RUSSIAN_POINT)).toBe(
            false,
        );
    });

    it('Если оба пункта находятся за пределами РФ - вернёт true', () => {
        expect(isForeignToForeignSearch(FOREIGN_POINT, FOREIGN_POINT)).toBe(
            true,
        );
    });
});
