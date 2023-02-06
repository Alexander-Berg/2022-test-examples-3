import {ALL_TYPE, TRAIN_TYPE, SUBURBAN_TYPE} from '../../../transportType';

import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';

import {getCanonicalUrl} from '../../searchUrl';

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
const whenTomorrow = {
    text: 'завтра',
    special: 'tomorrow',
};
const language = Lang.ru;
const tld = Tld.ru;

describe('getCanonicalUrl', () => {
    it('Есть информация о канонической ссылке с сервера', () => {
        const searchContext = {
            language,
            transportType: ALL_TYPE,
            from,
            originalFrom,
            to,
            originalTo,
            when: whenTomorrow,
            canonical: {
                transportType: TRAIN_TYPE,
                pointTo: 'tver',
                pointFrom: 'moscow',
            },
        };

        expect(getCanonicalUrl(searchContext, tld, language)).toBe(
            '/train/moscow--tver',
        );
    });

    it('Нет информации о канонической ссылке с сервера', () => {
        const searchContext = {
            language,
            transportType: ALL_TYPE,
            from,
            originalFrom,
            to,
            originalTo,
            when: whenTomorrow,
            canonical: null,
        };

        expect(getCanonicalUrl(searchContext, tld, language)).toBe('');
    });

    it('Для электричек канонической страницей является страница поиска на сегодня', () => {
        const searchContext = {
            language,
            transportType: SUBURBAN_TYPE,
            from,
            originalFrom,
            to,
            originalTo,
            when: whenTomorrow,
            canonical: {
                transportType: TRAIN_TYPE,
                pointTo: 'tver',
                pointFrom: 'moscow',
            },
        };

        expect(getCanonicalUrl(searchContext, tld, language)).toBe(
            '/suburban/moscow--tver/today',
        );
    });

    it('Параметры из контекста лишние в каноникле не должны попадать в него', () => {
        const searchContext = {
            language,
            transportType: SUBURBAN_TYPE,
            from,
            originalFrom,
            to,
            originalTo,
            when: whenTomorrow,
            canonical: {
                transportType: TRAIN_TYPE,
                pointTo: 'tver',
                pointFrom: 'moscow',
            },
            plan: 'g19',
        };

        expect(getCanonicalUrl(searchContext, tld, language)).toBe(
            '/suburban/moscow--tver/today',
        );
    });
});
