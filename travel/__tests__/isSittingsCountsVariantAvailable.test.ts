import {ARRANGEMENT_REQUIREMENTS} from 'projects/trains/constants/requirements';
import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';

import {ITrainsCoach} from 'reducers/trains/order/types';
import {ITrainsSchema} from 'server/api/TrainsApi/types/ITrainsDetailsApiResponse';

import isNearWindowAndPassageCountsOptionAvailable from 'projects/trains/components/TrainsOrderPage/Requirements/helpers/isNearWindowAndPassageCountsOptionAvailable';

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
            groupsInRows: [
                [1, 2],
                [3, 4],
                [5, 6],
            ],
            nearWindow: [1, 3, 5],
        },
    } as ITrainsSchema,
};

describe('isNearWindowAndPassageCountsOptionAvailable', () => {
    it('1 место у окна доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [1, 0],
                coaches,
                schemas,
                passengersCount: 1,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('2 места у окна доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [2, 0],
                coaches,
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('1 место у окна и 1 у прохода доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [1, 1],
                coaches,
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('2 места у  прохода доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [0, 2],
                coaches,
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(true);
    });

    it('1 место у окна не доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [1, 0],
                coaches: [coaches[1]],
                schemas,
                passengersCount: 1,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    it('2 места у окна не доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [2, 0],
                coaches: [coaches[1]],
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    it('1 место у окна и 1 у прохода не доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [1, 1],
                coaches: [coaches[1]],
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    it('2 места у  прохода не доступно', () => {
        expect(
            isNearWindowAndPassageCountsOptionAvailable({
                option: [0, 2],
                coaches: [coaches[2]],
                schemas,
                passengersCount: 2,
                coachType: TRAIN_COACH_TYPE.SITTING,
                requirements: {
                    arrangement: ARRANGEMENT_REQUIREMENTS.IRRELEVANT,
                },
            }),
        ).toBe(false);
    });

    describe('Места рядом', () => {
        it('2 места у окна не доступно', () => {
            expect(
                isNearWindowAndPassageCountsOptionAvailable({
                    option: [2, 0],
                    coaches: [coaches[0]],
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.SITTING,
                    requirements: {
                        arrangement: ARRANGEMENT_REQUIREMENTS.NEAREST,
                    },
                }),
            ).toBe(false);
        });

        it('2 места у прохода не доступно', () => {
            expect(
                isNearWindowAndPassageCountsOptionAvailable({
                    option: [0, 2],
                    coaches: [coaches[0]],
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.SITTING,
                    requirements: {
                        arrangement: ARRANGEMENT_REQUIREMENTS.NEAREST,
                    },
                }),
            ).toBe(false);
        });

        it('1 место у прохода и 1 у окна доступно', () => {
            expect(
                isNearWindowAndPassageCountsOptionAvailable({
                    option: [1, 1],
                    coaches: [coaches[0]],
                    schemas,
                    passengersCount: 2,
                    coachType: TRAIN_COACH_TYPE.SITTING,
                    requirements: {
                        arrangement: ARRANGEMENT_REQUIREMENTS.NEAREST,
                    },
                }),
            ).toBe(true);
        });
    });
});
