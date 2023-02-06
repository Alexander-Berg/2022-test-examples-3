import moment from 'moment';

import {ROBOT, TIME} from 'utilities/dateUtils/formats';

import {
    parseTrainRoutePart,
    stringifyTrainRoutePart,
} from '../parseTrainRoutePart';

describe('parseTrainRoutePart', () => {
    test('Должен вернуть объект параметров, описывающий часть маршрута', () => {
        expect(parseTrainRoutePart('P1_020У_c213_c2_2020-12-23T00:20')).toEqual(
            {
                number: '020У',
                provider: 'P1',
                fromId: 'c213',
                toId: 'c2',
                when: moment('2020-12-23T00:20', `${ROBOT}T${TIME}`),
            },
        );
    });
});

describe('stringifyPartTrainRoute', () => {
    test('Должен вернуть строку из объекта параметров', () => {
        expect(
            stringifyTrainRoutePart({
                number: '020У',
                provider: 'P1',
                fromId: 'c213',
                toId: 'c2',
                when: moment('2020-12-23T00:20', `${ROBOT}T${TIME}`),
            }),
        ).toEqual('P1_020У_c213_c2_2020-12-23T00:20');
    });
});
