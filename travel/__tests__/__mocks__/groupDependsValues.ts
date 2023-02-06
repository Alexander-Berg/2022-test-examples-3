import {EFormKey} from 'constants/form/EFormKey';
import {IFormValidationInfo} from 'types/common/validation/form';
import {
    EValidationType,
    EDependentConditionType,
} from 'types/common/validation/validation';

const passengerGroupName = 'passenger';
const contactsGroupName = 'contacts';
const bonusGroupName = 'bonusCard';

const phoneFieldName = 'phone';
const bonusGroupFieldName = 'bonusCardValue';

export const groupFormFalues = {
    [passengerGroupName]: [
        {
            firstName: 'Иван',
            lastName: 'Иванов',
            patronymicName: 'Иванович',
            document: {
                documentType: 'ru_passport',
                documentNumber: '1234567890',
            },
        },
        {
            firstName: 'Петр',
            lastName: 'Петров',
            patronymicName: 'Петрович',
            document: {
                documentType: 'ru_foreign_passport',
                documentNumber: '0987654321',
            },
        },
    ],
    [contactsGroupName]: {
        email: 'test@test.ru',
        [phoneFieldName]: '+79998887766',
    },
    [bonusGroupName]: {
        [bonusGroupFieldName]: 'superPrizNaBarabane',
    },
};

export const groupFormValidationInfo: IFormValidationInfo = {
    id: EFormKey.AVIA_BOOK,
    fieldGroups: [
        {
            id: contactsGroupName,
            fields: [
                {
                    name: phoneFieldName,
                    dependentValidations: [
                        {
                            conditions: [
                                {
                                    path: {
                                        fieldGroupId: bonusGroupName,
                                        fieldName: bonusGroupFieldName,
                                    },
                                    value: [
                                        {
                                            type: EValidationType.oneOf,
                                            params: ['superPrizNaBarabane'],
                                        },
                                    ],
                                },
                            ],
                            validation: {
                                blur: [
                                    {
                                        type: EValidationType.required,
                                        params: true,
                                        errorMessage:
                                            'Ошибка зависимой валидации со ссылкой (Если есть бонусная карта, то должен быть указан телефон)',
                                    },
                                ],
                            },
                        },
                        {
                            conditions: [
                                {
                                    path: {
                                        fieldGroupId: passengerGroupName,
                                        fieldName: 'document.documentType',
                                        type: EDependentConditionType.SOME,
                                    },
                                    value: [
                                        {
                                            type: EValidationType.oneOf,
                                            params: ['ru_passport'],
                                        },
                                    ],
                                },
                            ],
                            validation: {
                                blur: [
                                    {
                                        type: EValidationType.regex,
                                        params: '^(\\+7).+$',
                                        errorMessage:
                                            'Ошибка зависимой валидации типа Some (Хоть у одного паспорт РФ, то телефон должен начинаться с +7)',
                                    },
                                ],
                            },
                        },
                        {
                            conditions: [
                                {
                                    path: {
                                        fieldGroupId: passengerGroupName,
                                        fieldName: 'document.documentType',
                                        type: EDependentConditionType.EVERY,
                                    },
                                    value: [
                                        {
                                            type: EValidationType.oneOf,
                                            params: ['ru_passport'],
                                        },
                                    ],
                                },
                            ],
                            validation: {
                                blur: [
                                    {
                                        type: EValidationType.regex,
                                        params: '^(\\+7900)\\d+$',
                                        errorMessage:
                                            'Ошибка зависимой валидации типа Every (Хоть у всех пассажиров паспорт РФ, то телефон должен начинаться с +7900)',
                                    },
                                ],
                            },
                        },
                    ],
                },
            ],
        },
    ],
};
