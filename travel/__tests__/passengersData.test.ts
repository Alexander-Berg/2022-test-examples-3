import {TRAIN_BONUS_CARDS} from 'projects/trains/constants/bonusCards';
import {PASSENGERS_TYPES} from 'projects/trains/constants/passengersTypes';
import {TRAINS_DOCUMENT_TYPES} from 'projects/trains/constants/documentTypes';
import {GENDER_TYPE} from 'projects/trains/constants/genders';

import {ITrainPassenger} from 'reducers/trains/order/types';

import {setCount} from 'reducers/trains/order/actions/passengers';
import {setBonusCard} from 'reducers/trains/order/actions/bonusCards';
import {
    setDocumentCountry,
    setDocumentNumber,
    setDocumentType,
} from 'reducers/trains/order/actions/passengerDocument';
import {
    setBirthDate,
    setEmailOrPhone,
    setFirstName,
    setGender,
    setLastName,
    setPatronymic,
} from 'reducers/trains/order/actions/singlePassengerData';

import passengersData from '../passengersData';

const passenger: ITrainPassenger = {
    ageGroup: PASSENGERS_TYPES.ADULTS,
    firstName: {value: ''},
    lastName: {value: ''},
    patronymic: {value: ''},
    birthDate: {value: ''},
    emailOrPhone: {value: ''},
    gender: {value: null},
    hasDiscountDocument: false,
    passengerDocument: {
        type: {value: null},
        number: {value: ''},
        country: {value: null},
        validDate: {value: ''},
    },
    bonusCards: {},
    isNonRefundableTariff: false,
};

const adultPassenger = passenger;
const childPassenger = {...passenger, ageGroup: PASSENGERS_TYPES.CHILDREN};
const babyPassenger = {...passenger, ageGroup: PASSENGERS_TYPES.BABIES};

describe('passengersData', () => {
    test('should return an array with single adult passenger by default', () => {
        expect(passengersData()).toEqual([adultPassenger]);
    });

    describe('adding and removing passengers', () => {
        test('add passengers to order', () => {
            const state: ITrainPassenger[] = [];

            expect(
                passengersData(state, setCount[PASSENGERS_TYPES.ADULTS](2)),
            ).toEqual([adultPassenger, adultPassenger]);
        });

        test('remove passengers from order', () => {
            const state: ITrainPassenger[] = [
                adultPassenger,
                adultPassenger,
                adultPassenger,
                childPassenger,
            ];

            expect(
                passengersData(state, setCount[PASSENGERS_TYPES.ADULTS](1)),
            ).toEqual([adultPassenger, childPassenger]);
        });

        test('remove passengers from order', () => {
            const state: ITrainPassenger[] = [
                adultPassenger,
                adultPassenger,
                childPassenger,
                babyPassenger,
            ];

            expect(
                passengersData(state, setCount[PASSENGERS_TYPES.CHILDREN](1)),
            ).toBe(state);
        });
    });

    describe('changing passengers data', () => {
        test('should handle `SET_FIRST_NAME` action', () => {
            const state = [adultPassenger, babyPassenger];
            const action = setFirstName('Пётр', 1);

            expect(passengersData(state, action)).toEqual([
                adultPassenger,
                {
                    ...babyPassenger,
                    firstName: {value: 'Пётр'},
                },
            ]);
        });

        test('should handle `SET_LAST_NAME` action', () => {
            const state = [adultPassenger, childPassenger];
            const action = setLastName('Петрова', 0);

            expect(passengersData(state, action)).toEqual([
                {
                    ...adultPassenger,
                    lastName: {value: 'Петрова'},
                },
                childPassenger,
            ]);
        });

        test('should handle `SET_PATRONYMIC` action', () => {
            const state = [adultPassenger, childPassenger, babyPassenger];
            const action = setPatronymic('Петровна', 2);

            expect(passengersData(state, action)).toEqual([
                adultPassenger,
                childPassenger,
                {
                    ...babyPassenger,
                    patronymic: {value: 'Петровна'},
                },
            ]);
        });

        test('should handle `SET_BIRTH_DATE` action', () => {
            const state = [adultPassenger, childPassenger, babyPassenger];
            const action = setBirthDate('01.01.1990', 0);

            expect(passengersData(state, action)).toEqual([
                {
                    ...adultPassenger,
                    birthDate: {value: '01.01.1990'},
                },
                childPassenger,
                babyPassenger,
            ]);
        });

        test('should handle `SET_GENDER` action', () => {
            const state = [adultPassenger, childPassenger, babyPassenger];
            const action = setGender(GENDER_TYPE.FEMALE, 0);

            expect(passengersData(state, action)).toEqual([
                {
                    ...adultPassenger,
                    gender: {value: 'female'},
                },
                childPassenger,
                babyPassenger,
            ]);
        });

        test('should handle `SET_DOCUMENT_TYPE` action', () => {
            const state = [adultPassenger, childPassenger, babyPassenger];
            const action = setDocumentType(
                TRAINS_DOCUMENT_TYPES.FOREIGN_DOCUMENT,
                0,
            );

            expect(passengersData(state, action)).toEqual([
                {
                    ...adultPassenger,
                    passengerDocument: {
                        type: {value: TRAINS_DOCUMENT_TYPES.FOREIGN_DOCUMENT},
                        number: {value: ''},
                        country: {value: null},
                        validDate: {value: ''},
                    },
                },
                childPassenger,
                babyPassenger,
            ]);
        });

        test('should handle `SET_DOCUMENT_NUMBER` action', () => {
            const state = [adultPassenger, childPassenger, babyPassenger];
            const action = setDocumentNumber('IV-ЫЪ №123123', 2);

            expect(passengersData(state, action)).toEqual([
                adultPassenger,
                childPassenger,
                {
                    ...babyPassenger,
                    passengerDocument: {
                        ...babyPassenger.passengerDocument,
                        number: {value: 'IV-ЫЪ №123123'},
                    },
                },
            ]);
        });

        test('should handle `SET_DOCUMENT_COUNTRY` action', () => {
            const state = [adultPassenger, childPassenger, babyPassenger];
            const countryCode = 222;
            const action = setDocumentCountry(countryCode, 1);

            expect(passengersData(state, action)).toEqual([
                adultPassenger,
                {
                    ...childPassenger,
                    passengerDocument: {
                        ...childPassenger.passengerDocument,
                        country: {value: countryCode},
                    },
                },
                babyPassenger,
            ]);
        });

        test('should handle `SET_EMAIL_OR_PHONE` action', () => {
            const state = [adultPassenger, babyPassenger];
            const action = setEmailOrPhone('test@test.ru', 1);

            expect(passengersData(state, action)).toEqual([
                adultPassenger,
                {
                    ...babyPassenger,
                    emailOrPhone: {value: 'test@test.ru'},
                },
            ]);
        });

        test('should handle `SET_BONUS_CARD` action', () => {
            const action1 = setBonusCard(
                {type: TRAIN_BONUS_CARDS.BONUS_CARD, number: '123'},
                0,
            );
            const action2 = setBonusCard(
                {type: TRAIN_BONUS_CARDS.BONUS_CARD, number: ''},
                0,
            );

            const state1 = passengersData(
                [adultPassenger, childPassenger, babyPassenger],
                action1,
            );
            const state2 = passengersData(state1, action2);

            expect(state1).toEqual([
                {
                    ...adultPassenger,
                    bonusCards: {
                        [TRAIN_BONUS_CARDS.BONUS_CARD]: {value: '123'},
                    },
                },
                childPassenger,
                babyPassenger,
            ]);

            expect(state2).toEqual([
                adultPassenger,
                childPassenger,
                babyPassenger,
            ]);
        });
    });
});
