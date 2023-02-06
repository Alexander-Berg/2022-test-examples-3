jest.disableAutomock();

import {ALL_TYPE, TRAIN_TYPE, SUBURBAN_TYPE} from '../../../transportType';

import {needUseUrlWithSlug} from '../../searchUrl';
import {getWhenForAllDays, getWhenForToday} from '../../../search/contextUtils';

const from = {
    key: 's2006004',
    slug: 'moscow-oktyabrskaya',
    title: 'Москва (Ленинградский вокзал)',
};
const originalFrom = {
    key: 'c213',
    slug: 'moscow',
    title: 'Москва',
};
const to = {
    key: 's9603093',
    slug: 'tver-train-station',
    title: 'Тверь',
};
const originalTo = {
    key: 'c14',
    slug: 'tver',
    title: 'Тверь',
};
const language = 'ru';
const whenTomorrow = {
    text: 'завтра',
    special: 'tomorrow',
};
const whenToday = getWhenForToday(language);
const whenAllDays = getWhenForAllDays(language);
const searchContext = {
    language,
    from,
    originalFrom,
    to,
    originalTo,
};

describe('needUseUrlWithSlug', () => {
    it('Поиск всеми видами транспорта на завтра должен быть в старом формате', () => {
        expect(
            needUseUrlWithSlug({
                ...searchContext,
                transportType: ALL_TYPE,
                when: whenTomorrow,
            }),
        ).toBe(false);
    });

    it('Поиск всеми видами транспорта на все дни должен быть со слагами', () => {
        expect(
            needUseUrlWithSlug({
                ...searchContext,
                transportType: ALL_TYPE,
                when: whenAllDays,
            }),
        ).toBe(true);
    });

    it('Поиск поездами на все дни должен быть со слагами', () => {
        expect(
            needUseUrlWithSlug({
                ...searchContext,
                transportType: TRAIN_TYPE,
                when: whenAllDays,
            }),
        ).toBe(true);
    });

    it('Поиск электричками на завтра должен быть в старом формате', () => {
        expect(
            needUseUrlWithSlug({
                ...searchContext,
                transportType: SUBURBAN_TYPE,
                when: whenTomorrow,
            }),
        ).toBe(false);
    });

    it('Поиск электричками на сегодня должен быть со слагами', () => {
        expect(
            needUseUrlWithSlug({
                ...searchContext,
                transportType: SUBURBAN_TYPE,
                when: whenToday,
            }),
        ).toBe(true);
    });
});
