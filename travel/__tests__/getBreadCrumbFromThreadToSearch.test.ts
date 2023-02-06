import {FilterTransportType} from '../../../lib/transportType';
import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getBreadCrumbFromThreadToSearch from '../getBreadCrumbFromThreadToSearch';

const getBreadCrumbFromThreadToSearchParams = {
    transportType: FilterTransportType.train,
    tld: Tld.ru,
    language: Lang.ru,
    titleFrom: 'Москва',
    titleTo: 'Сухум',
};

describe('getBreadCrumbFromThreadToSearch', () => {
    it('Вернет хлебную крошку', () => {
        expect(
            getBreadCrumbFromThreadToSearch({
                ...getBreadCrumbFromThreadToSearchParams,

                fromSlug: 'moscow',
                toSlug: 'suhum',
            }),
        ).toEqual({
            url: '/train/moscow--suhum',
            name: 'Поезда Москва – Сухум',
        });
    });

    it('Не вернет хлебную крошку (вернет null) потому что слаги не переданы', () => {
        expect(
            getBreadCrumbFromThreadToSearch(
                getBreadCrumbFromThreadToSearchParams,
            ),
        ).toBe(null);
    });

    it('Не вернет хлебную крошку (вернет null) потому что тип транспота - водный (не поезд/электричка/автобус)', () => {
        expect(
            getBreadCrumbFromThreadToSearch({
                ...getBreadCrumbFromThreadToSearchParams,
                transportType: FilterTransportType.water,

                fromSlug: 'moscow',
                toSlug: 'suhum',
            }),
        ).toBe(null);
    });
});
