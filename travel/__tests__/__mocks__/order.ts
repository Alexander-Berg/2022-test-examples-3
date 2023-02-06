import {TRAIN_BONUS_CARDS} from 'projects/trains/constants/bonusCards';
import {TRAINS_DOCUMENT_TYPES} from 'projects/trains/constants/documentTypes';

import {
    IStoreTrainTraveler,
    ITrainOrderContacts,
    ITrainsCoach,
} from 'reducers/trains/order/types';
import {EGender} from 'types/common/document/EGender';

export default function createOrder(): {
    coach: ITrainsCoach;
    traveler: IStoreTrainTraveler;
    contacts: ITrainOrderContacts;
} {
    return {
        coach: {
            loyaltyCards: [TRAIN_BONUS_CARDS.BONUS_CARD],
        } as ITrainsCoach,
        traveler: {
            passengers: [
                {
                    itn: '000000000000',
                    documents: [
                        {
                            id: '19cae513-c058-44de-9846-348b999cd237',
                            passengerID: '134ffb2e-7ff3-414a-afe0-ff9ae92ac215',
                            firstName: {value: 'Иван'},
                            patronymic: {value: 'Иванович'},
                            lastName: {value: 'Иванов'},
                            validDate: {value: ''},
                            type: {
                                value: TRAINS_DOCUMENT_TYPES.RUSSIAN_PASSPORT,
                            },
                            number: {value: '0000000000'},
                            country: {value: 225},
                            clicked: false,
                        },
                    ],
                    train_notifications_enabled: false,
                    title: 'ivan',
                    gender: {value: EGender.MALE},
                    created_at: '2020-01-22 12:33:08',
                    updated_at: '2020-05-14 22:35:57',
                    id: '134ffb2e-7ff3-414a-afe0-ff9ae92ac215',
                    phone_additional: null,
                    phone: '+79222222222',
                    email: 'traveler@yandex.ru',
                    loyaltyCards: [],
                    birthDate: {value: '11.11.1991'},
                },
            ],
            accountPassengers: [],
        },
        contacts: {
            email: {value: 'email@contacts.ru'},
            phone: {value: '+79999999999'},
            useContacts: {value: false},
        },
    };
}
