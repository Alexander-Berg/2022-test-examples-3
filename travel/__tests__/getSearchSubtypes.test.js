jest.disableAutomock();

jest.mock('../../groupFilteredSegments', () => {
    return jest.fn(({segments}) => {
        const result = [];

        segments.forEach(segment => {
            if (result[segment.key]) {
                result[segment.key].segments.push(segment);
            } else {
                result[segment.key] = {
                    title: segment.type,
                    segments: [segment],
                    key: segment.key,
                };
            }
        });

        return result;
    });
});

import getSearchSubtypes from '../getSearchSubtypes';
import {TRAIN_TYPE, SUBURBAN_TYPE} from '../../../transportType';

const trainContext = {transportType: TRAIN_TYPE};
const suburbanContext = {transportType: SUBURBAN_TYPE};
const badContext = {};

const sapsanSegment = {title: 'Поезд-Сапсан', key: '0', type: 'Сапсан'};
const lastochkaSegment = {title: 'Поезд-Ласточка', key: '1', type: 'Ласточка'};

const trainSegments = [sapsanSegment, lastochkaSegment];
const extendedTrainSegments = [
    sapsanSegment,
    lastochkaSegment,
    lastochkaSegment,
];

const suburbanSegmentLastochka = {
    transport: {subtype: {code: 'last', title: 'Ласточка'}},
};
const suburbanSegmentNormal = {transport: {}};
const suburbanSegmentNoTransport = {};

const suburbanSubtypes = [
    {title: 'Ласточка', segments: [suburbanSegmentLastochka], key: 'last'},
];
const suburbanSubtypesExtended = [
    {
        title: 'Ласточка',
        segments: [suburbanSegmentLastochka, suburbanSegmentLastochka],
        key: 'last',
    },
];

const trainSubtypes = [
    {title: 'Сапсан', segments: [sapsanSegment], key: '0'},
    {title: 'Ласточка', segments: [lastochkaSegment], key: '1'},
];

const extendedTrainSubtypes = [
    {title: 'Сапсан', segments: [sapsanSegment], key: '0'},
    {
        title: 'Ласточка',
        segments: [lastochkaSegment, lastochkaSegment],
        key: '1',
    },
];

describe('getSearchSubtypes', () => {
    it('Должен вернуть пустой массив при некорректном контексте.', () => {
        expect(getSearchSubtypes(trainSegments, badContext)).toEqual([]);
    });

    it('Должен вернуть массив с сегментом-ласточкой для контекста электричек.', () => {
        expect(
            getSearchSubtypes(
                [suburbanSegmentLastochka, suburbanSegmentNormal],
                suburbanContext,
            ),
        ).toEqual(suburbanSubtypes);
    });

    it('Должен вернуть массив с сегментами-ласточками для контекста электричек.', () => {
        expect(
            getSearchSubtypes(
                [suburbanSegmentLastochka, suburbanSegmentLastochka],
                suburbanContext,
            ),
        ).toEqual(suburbanSubtypesExtended);
    });

    it('Должен вернуть пустой массив для контекста электричек если нет сегментов с подтипами.', () => {
        expect(
            getSearchSubtypes(
                [suburbanSegmentNormal, suburbanSegmentNoTransport],
                suburbanContext,
            ),
        ).toEqual([]);
    });

    it('Должен вернуть массив с объектами для ласточек и сапсанов для поездов.', () => {
        expect(getSearchSubtypes(trainSegments, trainContext)).toEqual(
            trainSubtypes,
        );
    });

    it('Должен вернуть массив с объектами для ласточек и сапсанов для поездов - несколько сегментов одного типа.', () => {
        expect(getSearchSubtypes(extendedTrainSegments, trainContext)).toEqual(
            extendedTrainSubtypes,
        );
    });
});
