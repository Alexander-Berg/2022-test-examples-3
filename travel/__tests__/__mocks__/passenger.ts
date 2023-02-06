import {GENDER_TYPE} from 'projects/trains/constants/genders';
import {TRAINS_DOCUMENT_TYPES} from 'projects/trains/constants/documentTypes';
import {PASSENGERS_TYPES} from 'projects/trains/constants/passengersTypes';

import {ITrainPassenger} from 'reducers/trains/order/types';

export default function createPassenger(): ITrainPassenger {
    return {
        firstName: {value: 'Иван'},
        lastName: {value: 'Иванов'},
        patronymic: {value: 'Иванович'},
        birthDate: {value: '11.11.1991'},
        gender: {value: GENDER_TYPE.MALE},
        hasDiscountDocument: false,
        passengerDocument: {
            type: {value: TRAINS_DOCUMENT_TYPES.RUSSIAN_PASSPORT},
            number: {value: '0000000000'},
            country: {value: 225},
            validDate: {value: ''},
        },
        bonusCards: {},
        ageGroup: PASSENGERS_TYPES.ADULTS,
        emailOrPhone: {value: ''},
        isNonRefundableTariff: false,
    };
}
