import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';
import {ARRANGEMENT_REQUIREMENTS} from 'projects/trains/constants/requirements';

import {ITrainsCoach} from 'reducers/trains/order/types';
import {ITrainsSchema} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';

import isArrangementOptionAvailable from 'projects/trains/components/TrainsOrderPage/Requirements/helpers/isArrangementOptionAvailable';

const schemas = {
    1: {
        placeFlags: {
            compartments: [
                [1, 2, 3, 4],
                [5, 6, 7, 8],
            ],
            sections: [
                [1, 2, 3, 4, 56, 57],
                [5, 6, 7, 8, 54, 55],
            ],
            upper: [1, 3, 5, 7, 55, 57],
            side: [57],
        },
    } as ITrainsSchema,
};

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
                number: 4,
            },
            {
                number: 5,
            },
            {
                number: 57,
            },
        ],
    },
] as ITrainsCoach[];

describe('isArrangementOptionAvailable', () => {
    describe('Купейные отсеки', () => {
        it('2 верхних не доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                    coaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 2,
                            bottom: 0,
                        },
                    },
                }),
            ).toBe(false);
        });

        it('2 нижних доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                    coaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 0,
                            bottom: 2,
                        },
                    },
                }),
            ).toBe(true);
        });

        it('1 нижнее и 1 верхнее доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.COMPARTMENT,
                    coaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 1,
                            bottom: 1,
                        },
                    },
                }),
            ).toBe(true);
        });
    });

    describe('Секции', () => {
        it('3 верхних не доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.SECTION,
                    coaches,
                    schemas,
                    passengersCount: 3,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 3,
                            bottom: 0,
                        },
                    },
                }),
            ).toBe(false);
        });

        it('3 нижних не доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.SECTION,
                    coaches,
                    schemas,
                    passengersCount: 3,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 0,
                            bottom: 3,
                        },
                    },
                }),
            ).toBe(false);
        });

        it('1 нижнее и 1 верхнее доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.SECTION,
                    coaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 1,
                            bottom: 1,
                        },
                    },
                }),
            ).toBe(true);
        });

        it('2 нижних и 2 верхних доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.SECTION,
                    coaches,
                    schemas,
                    passengersCount: 4,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 2,
                            bottom: 2,
                        },
                    },
                }),
            ).toBe(true);
        });
    });

    describe('Не боковые', () => {
        it('3 нижних не доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.NOT_SIDE,
                    coaches,
                    schemas,
                    passengersCount: 3,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 0,
                            bottom: 3,
                        },
                    },
                }),
            ).toBe(false);
        });

        it('2 верхних доступны', () => {
            expect(
                isArrangementOptionAvailable({
                    option: ARRANGEMENT_REQUIREMENTS.NOT_SIDE,
                    coaches,
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.PLATZKARTE,
                    requirements: {
                        count: {
                            upper: 0,
                            bottom: 2,
                        },
                    },
                }),
            ).toBe(true);
        });
    });
});
