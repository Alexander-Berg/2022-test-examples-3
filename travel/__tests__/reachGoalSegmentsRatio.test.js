import {reachGoalOnce} from '../../yaMetrika';
import reachGoalSegmentsRatio from '../reachGoalSegmentsRatio';

jest.mock('../../yaMetrika', () => ({
    ...require.requireActual('../../yaMetrika'),
    reachGoalOnce: jest.fn(),
}));

const statePage = {
    location: {
        pathname: '/search/',
    },
    originUrl: 'rasp.yandex.ru',
};
const context = {
    from: {
        key: 52,
    },
    to: {
        key: 54,
    },
    when: {
        date: '12 июня',
    },
};
const query = `fromId=${context.from.key}&toId=${context.to.key}&when=${context.when.date}`;
const url = `${statePage.originUrl}${statePage.location.pathname}?${query}`;

describe('reachGoalSegmentsRatio', () => {
    it('should call reachGoal with 0% ratio for empty array', () => {
        const segments = [];

        reachGoalSegmentsRatio(segments, statePage, context);

        expect(reachGoalOnce).not.toBeCalledWith('high-segments-ratio');
        expect(reachGoalOnce).toBeCalledWith('segments-ratio', {
            'segments-ratio': {0: url},
        });
    });

    it('should call reachGoal with 0% ratio for array of static segments', () => {
        const segments = [
            {isDynamic: false},
            {isDynamic: false},
            {isDynamic: false},
        ];

        reachGoalSegmentsRatio(segments, statePage, context);

        expect(reachGoalOnce).not.toBeCalledWith('high-segments-ratio');
        expect(reachGoalOnce).toBeCalledWith('segments-ratio', {
            'segments-ratio': {0: url},
        });
    });

    it('should call reachGoal with 100% ratio for array of dynamic segments', () => {
        const segments = [
            {isDynamic: true},
            {isDynamic: true},
            {isDynamic: true},
        ];

        reachGoalSegmentsRatio(segments, statePage, context);

        expect(reachGoalOnce).toBeCalledWith('high-segments-ratio');
        expect(reachGoalOnce).toBeCalledWith('segments-ratio', {
            'segments-ratio': {100: url},
        });
    });

    it('should call reachGoal with 50% ratio for mixed (50/50) array of segments', () => {
        const segments = [
            {isDynamic: false},
            {isDynamic: false},
            {isDynamic: true},
            {isDynamic: true},
        ];

        reachGoalSegmentsRatio(segments, statePage, context);

        expect(reachGoalOnce).toBeCalledWith('high-segments-ratio');
        expect(reachGoalOnce).toBeCalledWith('segments-ratio', {
            'segments-ratio': {50: url},
        });
    });

    it('should call reachGoal with 5% ratio for mixed (1/20) array of segments', () => {
        const segments = [
            ...Array.from({length: 19}, () => ({isDynamic: false})),
            {isDynamic: true},
        ];

        reachGoalSegmentsRatio(segments, statePage, context);

        expect(reachGoalOnce).not.toBeCalledWith('high-segments-ratio');
        expect(reachGoalOnce).toBeCalledWith('segments-ratio', {
            'segments-ratio': {5: url},
        });
    });
});
