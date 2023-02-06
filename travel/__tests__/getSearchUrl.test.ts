import getSearchUrl from '../getSearchUrl';
import {FilterTransportType} from '../../transportType';
import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import Point from '../../../interfaces/Point';
import DateSpecialValue from '../../../interfaces/date/DateSpecialValue';
import DateRobot from '../../../interfaces/date/DateRobot';
import Slug from '../../../interfaces/Slug';

describe('getSearchUrl', () => {
    it('ссылки с использованием Point', () => {
        expect(
            getSearchUrl({
                transportType: FilterTransportType.all,
                fromPoint: 's1' as Point,
                toPoint: 's2' as Point,
                tld: Tld.ru,
                language: Lang.ru,
            }),
        ).toBe('/search/?fromId=s1&toId=s2');

        expect(
            getSearchUrl({
                transportType: FilterTransportType.suburban,
                fromPoint: 's1' as Point,
                toPoint: 's2' as Point,
                tld: Tld.ru,
                language: Lang.ru,
            }),
        ).toBe('/search/suburban/?fromId=s1&toId=s2');

        expect(
            getSearchUrl({
                transportType: FilterTransportType.suburban,
                fromPoint: 's1' as Point,
                toPoint: 's2' as Point,
                tld: Tld.ru,
                language: Lang.ru,
                when: DateSpecialValue.today,
            }),
        ).toBe(
            '/search/suburban/?fromId=s1&toId=s2&when=%D1%81%D0%B5%D0%B3%D0%BE%D0%B4%D0%BD%D1%8F',
        );

        expect(
            getSearchUrl({
                transportType: FilterTransportType.suburban,
                fromPoint: 's1' as Point,
                toPoint: 's2' as Point,
                tld: Tld.ru,
                language: Lang.ru,
                when: '2020-01-21' as DateRobot,
            }),
        ).toBe('/search/suburban/?fromId=s1&toId=s2&when=2020-01-21');
    });

    it('Ссылки с использованием slug', () => {
        expect(
            getSearchUrl({
                transportType: FilterTransportType.all,
                fromSlug: 'moscow' as Slug,
                toSlug: 'ryazan' as Slug,
                tld: Tld.ru,
                language: Lang.ru,
                when: DateSpecialValue.allDays,
            }),
        ).toBe('/all-transport/moscow--ryazan');

        expect(
            getSearchUrl({
                transportType: FilterTransportType.train,
                fromSlug: 'moscow' as Slug,
                toSlug: 'ryazan' as Slug,
                tld: Tld.ru,
                language: Lang.ru,
                when: DateSpecialValue.allDays,
            }),
        ).toBe('/train/moscow--ryazan');

        expect(
            getSearchUrl({
                transportType: FilterTransportType.suburban,
                fromSlug: 'moscow' as Slug,
                toSlug: 'ryazan' as Slug,
                tld: Tld.ru,
                language: Lang.ru,
                when: DateSpecialValue.today,
            }),
        ).toBe('/suburban/moscow--ryazan/today');

        expect(
            getSearchUrl({
                transportType: FilterTransportType.suburban,
                fromSlug: 'moscow' as Slug,
                toSlug: 'ryazan' as Slug,
                fromPoint: 's1' as Point,
                toPoint: 's2' as Point,
                tld: Tld.ru,
                language: Lang.ru,
                when: DateSpecialValue.today,
            }),
        ).toBe('/suburban/moscow--ryazan/today');

        // today есть только у электричек
        expect(
            getSearchUrl({
                transportType: FilterTransportType.train,
                fromSlug: 'moscow' as Slug,
                toSlug: 'ryazan' as Slug,
                fromPoint: 's1' as Point,
                toPoint: 's2' as Point,
                tld: Tld.ru,
                language: Lang.ru,
                when: DateSpecialValue.today,
            }),
        ).toBe(
            '/search/train/?fromId=s1&toId=s2&when=%D1%81%D0%B5%D0%B3%D0%BE%D0%B4%D0%BD%D1%8F',
        );
    });
});
