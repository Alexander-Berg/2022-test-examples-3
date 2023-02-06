import {EGender} from 'types/common/document/EGender';

import {getPatchedTravelerPassengers} from 'projects/trains/lib/order/traveler/patchServerResponse/getPatchedTravelerPassengers';

describe('getPatchedTravelerPassengers', () => {
    it('Преобразованная структура пассажиров', () => {
        expect(
            getPatchedTravelerPassengers([
                {
                    id: 'fDr54xhh9',
                    documents: [],
                    bonus_cards: [],
                    title: 'Коллега второй',
                    gender: EGender.FEMALE,
                    birth_date: '1992.08.23',
                    phone: '+73232332322',
                    phone_additional: '',
                    email: 'testtest2@test.ru',
                    updated_at: '',
                    created_at: '',
                    train_notifications_enabled: false,
                    itn: null,
                },
                {
                    id: 'fDr5443hh9',
                    documents: [],
                    bonus_cards: [],
                    title: 'Коллега третий',
                    gender: EGender.MALE,
                    birth_date: '1993.01.01',
                    phone: '+73232332323',
                    phone_additional: '',
                    email: 'testtest3@test.ru',
                    updated_at: '',
                    created_at: '',
                    train_notifications_enabled: false,
                    itn: null,
                },
            ]),
        ).toEqual([
            {
                birthDate: {
                    value: '23.08.1992',
                },
                documents: [],
                loyaltyCards: [],
                email: 'testtest2@test.ru',
                gender: {
                    value: 'female',
                },
                id: 'fDr54xhh9',
                phone: '+73232332322',
                phone_additional: '',
                title: 'Коллега второй',
                updated_at: '',
                created_at: '',
                train_notifications_enabled: false,
                itn: null,
            },
            {
                birthDate: {
                    value: '01.01.1993',
                },
                documents: [],
                loyaltyCards: [],
                email: 'testtest3@test.ru',
                gender: {
                    value: 'male',
                },
                id: 'fDr5443hh9',
                phone: '+73232332323',
                phone_additional: '',
                title: 'Коллега третий',
                updated_at: '',
                created_at: '',
                train_notifications_enabled: false,
                itn: null,
            },
        ]);
    });
});
