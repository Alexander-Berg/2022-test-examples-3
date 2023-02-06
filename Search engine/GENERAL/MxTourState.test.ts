import {tourStorageReducer} from './MxTourState';

const TIMESTAMP = '2021-06-06T11:45:30.324Z';

jest.spyOn(Date.prototype, 'toISOString').mockReturnValue(TIMESTAMP);

describe('tourStorageReducer', () => {
    describe('action ADD_STEPS', () => {
        test('should correct merge tour id', () => {
            expect(
                tourStorageReducer(
                    {
                        neverShowTours: false,
                        showedSteps: {
                            step2: {showedInTour: 'someId2'},
                            step1: {showedInTour: 'someId'},
                        },
                        lastShowedStepTimeStamp: null,
                    },
                    {
                        type: 'ADD_STEPS',
                        payload: {
                            tourId: 'someId',
                            stepIds: ['step1', 'step2'],
                        },
                    },
                ),
            ).toEqual({
                showedSteps: {
                    step1: {
                        showedInTour: 'someId',
                    },
                    step2: {
                        showedInTour: 'someId',
                    },
                },
                lastShowedStepTimeStamp: null,
                neverShowTours: false,
            });
        });
        test('should return expected results on empty init state', () => {
            expect(
                tourStorageReducer(
                    {
                        showedSteps: {},
                        lastShowedStepTimeStamp: null,
                        neverShowTours: false,
                    },
                    {
                        type: 'ADD_STEPS',
                        payload: {
                            tourId: 'someId',
                            stepIds: ['step1', 'step2'],
                        },
                    },
                ),
            ).toEqual({
                showedSteps: {
                    step1: {
                        showedInTour: 'someId',
                    },
                    step2: {
                        showedInTour: 'someId',
                    },
                },
                lastShowedStepTimeStamp: null,
                neverShowTours: false,
            });
        });
        test('should correct update timestamp if option passed', () => {
            expect(
                tourStorageReducer(
                    {
                        showedSteps: {},
                        lastShowedStepTimeStamp: null,
                        neverShowTours: false,
                    },
                    {
                        type: 'ADD_STEPS',
                        payload: {
                            tourId: 'someId',
                            stepIds: ['step1'],
                            updateTimestamp: true,
                        },
                    },
                ),
            ).toEqual({
                showedSteps: {
                    step1: {
                        showedInTour: 'someId',
                    },
                },
                lastShowedStepTimeStamp: TIMESTAMP,
                neverShowTours: false,
            });
        });
    });
});
