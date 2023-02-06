jest.disableAutomock();

import {TRAIN_TYPE, PLANE_TYPE, ALL_TYPE} from '../../../transportType';

import {composeSearchPath} from '../../searchUrl';

describe('composeSearchPath', () => {
    it('Поиск поездами не на все дни', () => {
        expect(composeSearchPath(TRAIN_TYPE)).toBe('/search/train/');
    });

    it('Поиск поездами ближайших рейсов', () => {
        expect(composeSearchPath(TRAIN_TYPE, true)).toBe('/search/train/next/');
    });

    it('Поиск самолетами не на все дни', () => {
        expect(composeSearchPath(PLANE_TYPE)).toBe('/search/plane/');
    });

    it('Поиск всеми видами транспорта не на все дни', () => {
        expect(composeSearchPath(ALL_TYPE)).toBe('/search/');
    });

    it('Поиск всеми видами транспорта ближайших рейсов', () => {
        expect(composeSearchPath(ALL_TYPE, true)).toBe('/search/next/');
    });
});
