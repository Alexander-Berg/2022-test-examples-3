import {EFormKey} from 'constants/form/EFormKey';
import {IFormValidationInfo} from 'types/common/validation/form';
import {EValidationType} from 'types/common/validation/validation';

const passengerGroupName = 'passenger';
const contactsGroupName = 'contacts';

export const groupFormValues = {
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
        phone: '+79998887766',
    },
};

export const groupFormValidationInfo: IFormValidationInfo = {
    id: EFormKey.AVIA_BOOK,
    fieldGroups: [
        {
            id: passengerGroupName,
            fields: [
                {
                    name: 'lastName',
                    validation: {
                        blur: [
                            {
                                type: EValidationType.regex,
                                params: '^[А-ЯЁа-яё]+$',
                                errorMessage: 'Ошибка',
                            },
                        ],
                    },
                    dependentValidations: [
                        {
                            conditions: [
                                {
                                    path: {fieldName: 'firstName'},
                                    value: [
                                        {
                                            type: EValidationType.regex,
                                            params: '^[А-ЯЁа-яё]+$',
                                        },
                                    ],
                                },
                                {
                                    path: {fieldName: 'document.documentType'},
                                    value: [
                                        {
                                            type: EValidationType.oneOf,
                                            params: [
                                                'ru_passport',
                                                'ru_foreign_passport',
                                            ],
                                        },
                                    ],
                                },
                            ],
                            validation: {
                                blur: [
                                    {
                                        type: EValidationType.maxLength,
                                        params: 20,
                                        errorMessage:
                                            'Ошибка зависимой валидации',
                                    },
                                ],
                            },
                        },
                    ],
                },
                {
                    name: 'document.documentType',
                    validation: {
                        blur: [
                            {
                                type: EValidationType.oneOf,
                                params: ['ru_passport', 'ru_foreign_passport'],
                                errorMessage: 'Ошибка',
                            },
                        ],
                    },
                },
                {
                    name: 'document.documentNumber',
                    validation: {
                        blur: [
                            {
                                type: EValidationType.regex,
                                params: '^\\d+$',
                                errorMessage: 'Ошибка',
                            },
                        ],
                    },
                    dependentValidations: [
                        {
                            conditions: [
                                {
                                    path: {fieldName: 'firstName'},
                                    value: [
                                        {
                                            type: EValidationType.regex,
                                            params: '^[А-ЯЁа-яё]+$',
                                        },
                                    ],
                                },
                            ],
                            validation: {
                                blur: [
                                    {
                                        type: EValidationType.maxLength,
                                        params: 15,
                                        errorMessage:
                                            'Ошибка зависимой валидации',
                                    },
                                ],
                            },
                        },
                    ],
                },
            ],
        },
        {
            id: contactsGroupName,
            fields: [
                {
                    name: 'phone',
                    validation: {
                        blur: [
                            {
                                type: EValidationType.regex,
                                params: '^(\\+7|8)\\d{10}$',
                                errorMessage: 'Ошибка',
                            },
                        ],
                    },
                },
            ],
        },
    ],
};
