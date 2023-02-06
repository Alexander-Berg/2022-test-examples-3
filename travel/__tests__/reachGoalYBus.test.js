import {BUS_TYPE, TRAIN_TYPE} from '../../transportType';
import {YBUS} from '../../segments/tariffSources';

import reachGoalYBus from '../reachGoalYBus';
import {params} from '../../yaMetrika';

jest.mock('../../yaMetrika');

describe('reachGoalYBus', () => {
    const context = {
        from: {title: 'Москва'},
        to: {title: 'Воронеж'},
    };
    const segments = [
        {
            transport: {code: BUS_TYPE},
            source: YBUS,
            canPayOffline: true,
        },
        {
            transport: {code: BUS_TYPE},
            source: YBUS,
            canPayOffline: false,
        },
        {
            transport: {code: BUS_TYPE},
            source: YBUS,
        },
        {
            transport: {code: BUS_TYPE},
            source: 'other',
        },
        {
            transport: {code: TRAIN_TYPE},
        },
        {
            transport: {code: BUS_TYPE},
            source: YBUS,
            thread: {uid: 'xxxxx_x_xxxxxxxxxx_861'},
        },
        {
            transport: {code: BUS_TYPE},
            source: 'other',
            thread: {uid: 'xxxxx_x_xxxxxxxxxx_168'},
        },
        {
            transport: {code: BUS_TYPE},
            source: 'other',
            isTransfer: true,
            segments: [
                {
                    transport: {code: TRAIN_TYPE},
                    thread: {uid: 'xxxxx_x_xxxxxxxxxx_123'},
                },
                {
                    transport: {code: TRAIN_TYPE},
                    thread: {uid: 'xxxxx_x_xxxxxxxxxx_456'},
                },
                {
                    transport: {code: TRAIN_TYPE},
                    thread: {uid: 'xxxxx_x_xxxxxxxxxx_789'},
                },
            ],
        },
    ];

    const SEGMENTS_COUNT = segments.length;
    const BUS_SEGMENTS_COUNT = segments.filter(
        segment => segment.transport.code === BUS_TYPE,
    ).length;
    const YBUS_SEGMENTS_COUNT = segments.filter(
        segment => segment.source === YBUS,
    ).length;
    const OTHERS_BUS_SEGMENTS_COUNT = segments.filter(
        segment =>
            segment.transport.code === BUS_TYPE && segment.source !== YBUS,
    ).length;
    const OFFLINE_YBUS_SEGMENTS_COUNT = segments.filter(
        segment => segment.source === YBUS && segment.canPayOffline,
    ).length;

    reachGoalYBus(segments, context);

    const paramsArgument = params.mock.calls[0][0];
    const countCalls = params.mock.calls.length;

    it('should call yaMetrika.params one time', () => {
        expect(countCalls).toBe(1);
    });

    it('should properly extract rideName', () => {
        expect(paramsArgument).toHaveProperty('rideName', 'Москва — Воронеж');
    });

    it('should properly calculate segmentsCount', () => {
        expect(paramsArgument).toHaveProperty('segmentsCount', SEGMENTS_COUNT);
    });

    it('should properly calculate busSegmentsCount', () => {
        expect(paramsArgument).toHaveProperty(
            'busSegmentsCount',
            BUS_SEGMENTS_COUNT,
        );
    });

    it('should properly calculate yandexBusSegmentsCount', () => {
        expect(paramsArgument).toHaveProperty(
            'yandexBusSegmentsCount',
            YBUS_SEGMENTS_COUNT,
        );
    });

    it('should properly calculate othersBusSegmentsCount', () => {
        expect(paramsArgument).toHaveProperty(
            'othersBusSegmentsCount',
            OTHERS_BUS_SEGMENTS_COUNT,
        );
    });

    it('should properly calculate yandexBusOfflineSegmentsCount', () => {
        expect(paramsArgument).toHaveProperty(
            'yandexBusOfflineSegmentsCount',
            OFFLINE_YBUS_SEGMENTS_COUNT,
        );
    });

    it('should properly extract data providers', () => {
        expect(paramsArgument).toHaveProperty('othersBusSegments', [
            {dataProviders: ['']},
            {dataProviders: ['168']},
            {dataProviders: ['123', '456', '789']},
        ]);
    });
});
