import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';
import {ARRANGEMENT_REQUIREMENTS} from 'projects/trains/constants/requirements';

import {ITrainsCoach} from 'reducers/trains/order/types';
import {ITrainsSchema} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';

import isUpperAndBottomCountsOptionAvailable from 'projects/trains/components/TrainsOrderPage/Requirements/helpers/isUpperAndBottomCountsOptionAvailable';

const coaches = [
    {
        schemaId: 1,
        places: [
            {
                number: 1,
            },
            {
                number: 2,
            },
            {
                number: 3,
            },
            {
                number: 4,
            },
        ],
    },
    {
        schemaId: 1,
        places: [
            {
                number: 2,
            },
            {
                number: 4,
            },
        ],
    },
    {
        schemaId: 1,
        places: [
            {
                number: 1,
            },
            {
                number: 2,
            },
        ],
    },
] as ITrainsCoach[];

const schemas = {
    1: {
        placeFlags: {
            compartments: [
                [1, 2, 3, 4],
                [5, 6, 7, 8],
            ],
            upper: [1, 3, 5, 7],
        },
    } as ITrainsSchema,
};

describe('isUpperAndBottomCountsOptionAvailable', () => {
    it('1 верхнее место доступно', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [1, 0],
                coaches,
                schemas,
                passengersCount: 1,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('2 верхних места доступны', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [2, 0],
                coaches,
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('1 верхнее и 1 нижнее место доступны', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [1, 1],
                coaches,
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('2 нижних места доступны', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [0, 2],
                coaches,
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('1 верхнее место не доступно', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [1, 0],
                coaches: [coaches[1]],
                schemas,
                passengersCount: 1,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    it('2 верхних места не доступны', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [2, 0],
                coaches: [coaches[1]],
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    it('1 верхнее и 1 нежнее место не доступны', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [1, 1],
                coaches: [coaches[1]],
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    it('2 нижних места не доступны', () => {
        expect(
            isUpperAndBottomCountsOptionAvailable({
                option: [0, 2],
                coaches: [coaches[2]],
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    describe('Купейные отсеки', () => {
        const blockCoaches = [
            {
                schemaId: 1,
                places: [
                    {
                        number: 1,
                    },
                    {
                        number: 2,
                    },
                    {
                        number: 5,
                    },
                    {
                        number: 6,
                    },
                ],
            },
        ] as ITrainsCoach[];

        it('2 верхних места не доступны', () => {
            expect(
                isUpperAndBottomCountsOptionAvailable({
                    option: [2, 0],
                    coaches: blockCoaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        arrangement: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                    },
                }),
            ).toBe(false);
        });

        it('2 нижних места не доступны', () => {
            expect(
                isUpperAndBottomCountsOptionAvailable({
                    option: [0, 2],
                    coaches: blockCoaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        arrangement: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                    },
                }),
            ).toBe(false);
        });

        it('1 верхнее и 1 нижнее место доступны', () => {
            expect(
                isUpperAndBottomCountsOptionAvailable({
                    option: [1, 1],
                    coaches: blockCoaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        arrangement: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                    },
                }),
            ).toBe(true);
        });
    });
});
