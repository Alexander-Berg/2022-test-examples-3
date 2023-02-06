import type {RawCategoryConfig} from 'shared/types/autogenApi';
import {RECEIVING_OPTIONS} from 'shared/types/autogenApi';

export const CATEGORY_CONFIG_MOCK: RawCategoryConfig = {
    categoryId: '91491',
    name: 'Мобильные телефоны',
    importantParams: [],
    fullCategoryName: 'Все товары/Электроника/Телефоны/Мобильные телефоны',
    contentReceivingOption: RECEIVING_OPTIONS.AVAILABLE,
    pictureRequired: false,
    parameterList: [
        {
            parameterId: 19172750,
            parameterName: 'Комплектация',
            parameterType: 'ENUM',
            description: '',
            multivalue: false,
            partnerValue: true,
            options: [
                {id: 19172933, value: 'кабель', default: true},
                {id: 19172932, value: 'наушники, кабель, адаптер', default: true},
            ],
            skuMode: 'SKU_DEFINING',
        },
        {
            parameterId: 14871214,
            parameterName: 'Цвет товара для карточки',
            parameterType: 'ENUM',
            description: '',
            multivalue: false,
            partnerValue: true,
            options: [
                {id: 17788122, value: ' Желтый', default: false},
                {id: 17325547, value: ' красный', default: false},
                {id: 17367592, value: ' розовый', default: false},
                {id: 17656293, value: ' серый', default: false},
                {id: 15461095, value: 'aegean blue', default: false},
                {id: 14896466, value: 'almond', default: false},
                {id: 15696109, value: 'alpine white', default: false},
                {id: 15317076, value: 'amazing silver', default: false},
                {id: 18050408, value: 'Amazon Moss зеленый', default: false},
                {id: 17584840, value: 'amber brown', default: false},
                {id: 16313124, value: 'amber gold', default: false},
                {id: 16781738, value: 'amber sunrise', default: false},
                {id: 14898180, value: 'aqua blue', default: false},
                {id: 18833730, value: 'aquamarine green', default: false},
                {id: 21513190, value: 'arctic sky', default: false},
                {id: 15878704, value: 'Arctic White', default: false},
            ],
            skuMode: 'SKU_DEFINING',
        },
        {
            parameterId: 13887626,
            parameterName: 'Цвет товара для фильтра',
            parameterType: 'ENUM',
            description: 'Соответствующий цвет из базовой палитры.',
            multivalue: true,
            partnerValue: true,
            options: [
                {id: 13887677, value: 'бежевый', default: true},
                {id: 13887686, value: 'белый', default: true},
                {id: 15688289, value: 'бесцветный', default: true},
                {id: 13887703, value: 'голубой', default: true},
                {id: 13891805, value: 'желтый', default: true},
                {id: 13891809, value: 'зеленый', default: true},
                {id: 13891828, value: 'золотистый', default: true},
                {id: 13891836, value: 'коричневый', default: true},
                {id: 13891866, value: 'красный', default: true},
                {id: 13891871, value: 'оранжевый', default: true},
                {id: 13891903, value: 'розовый', default: true},
                {id: 13898623, value: 'серебристый', default: true},
                {id: 13898641, value: 'серый', default: true},
                {id: 13898977, value: 'синий', default: true},
                {id: 13898990, value: 'фиолетовый', default: true},
                {id: 13899071, value: 'черный', default: true},
            ],
            skuMode: 'SKU_INFORMATIONAL',
        },
        {
            parameterId: 7351753,
            parameterName: 'Алиасы',
            parameterType: 'TEXT',
            description: '',
            multivalue: false,
            partnerValue: true,
            skuMode: 'SKU_INFORMATIONAL',
        },
    ],
    layout: {
        sections: [
            {
                id: 'sku',
                groups: [
                    {
                        title: 'Характеристики варианта',
                        description:
                            'Обязательно укажите основные характеристики, чтобы на витрине ваши варианты товаров можно было легко отличить',
                        parameterReferences: [
                            {paramId: 19172750},
                            {paramId: 14871214},
                            {paramId: 13887626},
                            {paramId: 7351753},
                        ],
                    },
                ],
            },
        ],
    },
    missingParameterFormParserData: false,
    skuRatingFormula: {
        dictionary: {
            '7351771': 'name',
            '7893318': 'vendor',
            '15341921': 'description',
            '-12321': 'pictures',
        },
        formula: {
            operation: 'sum',
            params: [
                {
                    operation: 'multiply',
                    params: [
                        3,
                        {
                            operation: 'ifExists',
                            params: [7351771],
                        },
                    ],
                },
                {
                    operation: 'multiply',
                    params: [
                        3,
                        {
                            operation: 'ifExists',
                            params: [7893318],
                        },
                    ],
                },
                {
                    operation: 'multiply',
                    params: [
                        4,
                        {
                            operation: 'ifExists',
                            params: [15341921],
                        },
                    ],
                },
                {
                    operation: 'min',
                    params: [
                        20,
                        {
                            operation: 'multiply',
                            params: [
                                5,
                                {
                                    operation: 'count',
                                    params: [-12321],
                                },
                            ],
                        },
                    ],
                },
                {
                    operation: 'min',
                    params: [
                        40,
                        {
                            operation: 'sum',
                            params: [
                                {
                                    operation: 'multiply',
                                    params: [
                                        8,
                                        {
                                            operation: 'ifExists',
                                            params: [16308920],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        8,
                                        {
                                            operation: 'ifExists',
                                            params: [16308711],
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
                {
                    operation: 'min',
                    params: [
                        30,
                        {
                            operation: 'sum',
                            params: [
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [15086295],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [19168490],
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            ],
        },
    },
    specialParams: [],
};
