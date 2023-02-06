import {RU_GEO_ID} from 'constants/common';
import {TRAINS_DOCUMENT_TYPES} from 'projects/trains/constants/documentTypes';

import {IDocumentDTO} from 'server/api/TravelersApi/types/IDocumentDTO';

import {CHAR_DASH} from 'utilities/strings/charCodes';
import {DOCUMENTS_TRAVELERS_KEYS} from 'projects/trains/lib/order/passengers/documentTypes';
import {getPatchedTravelerPassengerDocuments} from 'projects/trains/lib/order/traveler/patchServerResponse/getPatchedTravelerPassengerDocuments';

const travelerData = {
    passenger_id: 'adDee21da2',
    first_name: 'Коллега',
    middle_name: 'Колегович',
    last_name: 'Коллегов',
    type: DOCUMENTS_TRAVELERS_KEYS[TRAINS_DOCUMENT_TYPES.RUSSIAN_PASSPORT],
    number: '1111111112',
    citizenship: RU_GEO_ID,
} as IDocumentDTO;

const patchedData = {
    country: {
        value: RU_GEO_ID,
    },
    firstName: {
        value: 'Коллега',
    },
    lastName: {
        value: 'Коллегов',
    },
    number: {
        value: '1111111112',
    },
    passengerID: 'adDee21da2',
    patronymic: {
        value: 'Колегович',
    },
    validDate: {value: undefined},
    type: {
        value: TRAINS_DOCUMENT_TYPES.RUSSIAN_PASSPORT,
    },
    clicked: false,
};

describe('getPatchedTravelerPassengerDocuments', () => {
    it('Преобразованная структура документов', () => {
        expect(getPatchedTravelerPassengerDocuments([travelerData])).toEqual([
            patchedData,
        ]);
    });

    it('Если в данных отсутствует отчество, но заданы имя и фамилия - заменяем дефисом', () => {
        expect(
            getPatchedTravelerPassengerDocuments([
                {
                    ...travelerData,
                    middle_name: undefined,
                },
            ]),
        ).toEqual([
            {
                ...patchedData,
                patronymic: {
                    value: CHAR_DASH,
                },
            },
        ]);
    });

    it('Вернет поля для латиницы, если поля для кириллицы пусты', () => {
        expect(
            getPatchedTravelerPassengerDocuments([
                {
                    ...travelerData,
                    first_name: undefined,
                    first_name_en: 'Abc',
                    middle_name: undefined,
                    middle_name_en: 'Xyz',
                    last_name: undefined,
                    last_name_en: 'Dfr',
                },
            ]),
        ).toEqual([
            {
                ...patchedData,
                firstName: {
                    value: 'Abc',
                },
                lastName: {
                    value: 'Dfr',
                },
                patronymic: {
                    value: 'Xyz',
                },
            },
        ]);
    });

    it('Если ФИО не заполнены ни на одном языке - вернёт дефолтные значения', () => {
        expect(
            getPatchedTravelerPassengerDocuments([
                {
                    ...travelerData,
                    first_name: undefined,
                    first_name_en: undefined,
                    middle_name: undefined,
                    middle_name_en: undefined,
                    last_name: undefined,
                    last_name_en: undefined,
                },
            ]),
        ).toEqual([
            {
                ...patchedData,
                firstName: {
                    value: '',
                },
                lastName: {
                    value: '',
                },
                patronymic: {
                    value: '',
                },
            },
        ]);
    });
});
