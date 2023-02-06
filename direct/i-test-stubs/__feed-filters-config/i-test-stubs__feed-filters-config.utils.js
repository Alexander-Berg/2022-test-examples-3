(function() {
    u.register({
        'i-test-stubs__feed-filters-config': function() {
            return {
                "schema": {
                    "checkedStruct": {
                        "type": "array",
                        "typeName": "array",
                        "constraints": [
                            {
                                "parameters": 10,
                                "name": "itemsCountMax",
                                "error": "Количество условий в фильтре должно быть от 1 до 10"
                            },
                            {
                                "error": "В фильтре не задано ни одного условия",
                                "parameters": 1,
                                "name": "itemsCountMin"
                            },
                            {
                                "error": "Неверная структура условия",
                                "parameters": {
                                    "itemIdAlias": "id",
                                    "variants": "#/definitions/rule",
                                    "mapBy": "field",
                                    "allowOnlyDefined": true
                                },
                                "name": "itemAnyOf"
                            }
                        ],
                        "items": {
                            "typeName": "rule",
                            "fields": {
                                "relation": {
                                    "typeName": "enum",
                                    "type": {
                                        "enum": [
                                            ">",
                                            "<",
                                            "==",
                                            "<->",
                                            "ilike",
                                            "not ilike",
                                            "exists"
                                        ]
                                    },
                                    "translations": "#/definitions/translations/rule",
                                    "errorOnWrongType": "Недопустимое отношение в правиле №[%id%]"
                                },
                                "field": {
                                    "type": "string",
                                    "typeName": "string"
                                },
                                "value": {
                                    "typeName": "array",
                                    "type": "array",
                                    "flexible": 1,
                                    "items": {
                                        "type": "string_or_number",
                                        "typeName": "string_or_number"
                                    }
                                }
                            },
                            "type": "object",
                            "allowOptionalFields": 1,
                            "errorOnWrongType": "Неправильный формат условия: указаны не все обязательные значения"
                        },
                        "itemsList": "#/definitions/rule",
                        "itemIdAlias": "id",
                        "itemsOrder": [
                            "id",
                            "categoryId",
                            "url",
                            "name",
                            "vendor",
                            "price",
                            "model",
                            "description",
                            "typePrefix",
                            "oldprice",
                            "market_category",
                            "currencyId",
                            "store",
                            "pickup",
                            "delivery",
                            "manufacturer_warranty",
                            "downloadable",
                            "adult",
                            "sales_notes",
                            "country_of_origin",
                            "age",
                            "retail"
                        ]
                    },
                    "definitions": {
                        "rule": {
                            "oldprice": {
                                "description": "Старая цена",
                                "fields": {
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения",
                                            "type": "always_wrong",
                                            "typeName": "always_wrong"
                                        },
                                        "typeByCondition": [
                                            {
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом",
                                                    "constraints": [
                                                        {
                                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                                            "name": "minValue",
                                                            "parameters": 0
                                                        }
                                                    ],
                                                    "typeName": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "type": "string_or_number"
                                                },
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "in": [
                                                        "=="
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "type": "array",
                                                "typeName": "arrayOfNumbers"
                                            },
                                            {
                                                "constraints": [
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMax",
                                                        "error": "Допускается только одно значение аргумента"
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "typeName": "arrayOfNumbers",
                                                "type": "array",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом",
                                                    "constraints": [
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+(?:\\.\\d+)?$"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                                            "name": "minValue",
                                                            "parameters": 0
                                                        }
                                                    ],
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "typeName": "string_or_number"
                                                },
                                                "condition": {
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                                            },
                                            {
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 10,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "typeName": "arrayOfRanges",
                                                "isCompositeList": 1,
                                                "type": "array",
                                                "items": {
                                                    "typeName": "range",
                                                    "frontendPrecision": 2,
                                                    "type": "range",
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "parameters": "\\S",
                                                            "name": "patternMatch",
                                                            "break": true
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                                            "parameters": "^\\d+(\\.\\d+)?$",
                                                            "break": true,
                                                            "name": "boundaryPatternMatch"
                                                        },
                                                        {
                                                            "name": "boundaryMinValue",
                                                            "break": true,
                                                            "parameters": 0,
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                        },
                                                        {
                                                            "break": true,
                                                            "name": "leftLessRight",
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                },
                                                "condition": {
                                                    "in": [
                                                        "<->"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                                            },
                                            {
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                }
                                            }
                                        ]
                                    },
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "type": "object",
                                "typeName": "priceOrRangeOrExists"
                            },
                            "price": {
                                "description": "Цена товара",
                                "fields": {
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "items": {
                                                    "typeName": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "type": "string_or_number",
                                                    "constraints": [
                                                        {
                                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "minValue",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом"
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "=="
                                                    ]
                                                },
                                                "type": "array",
                                                "typeName": "arrayOfNumbers",
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ]
                                            },
                                            {
                                                "items": {
                                                    "frontendPrecision": 2,
                                                    "type": "string_or_number",
                                                    "typeName": "string_or_number",
                                                    "constraints": [
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                                            "name": "patternMatch",
                                                            "break": true
                                                        },
                                                        {
                                                            "name": "minValue",
                                                            "parameters": 0,
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом"
                                                },
                                                "condition": {
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "constraints": [
                                                    {
                                                        "error": "Допускается только одно значение аргумента",
                                                        "parameters": 1,
                                                        "name": "itemsCountMax"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "typeName": "arrayOfNumbers",
                                                "type": "array"
                                            },
                                            {
                                                "constraints": [
                                                    {
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                                        "name": "itemsCountMax",
                                                        "parameters": 10
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "isCompositeList": 1,
                                                "type": "array",
                                                "typeName": "arrayOfRanges",
                                                "items": {
                                                    "typeName": "range",
                                                    "type": "range",
                                                    "frontendPrecision": 2,
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "\\S"
                                                        },
                                                        {
                                                            "name": "boundaryPatternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+(\\.\\d+)?$",
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "boundaryMinValue",
                                                            "break": true,
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой",
                                                            "break": true,
                                                            "name": "leftLessRight"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                },
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "in": [
                                                        "<->"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                                            },
                                            {
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                }
                                            }
                                        ],
                                        "default": {
                                            "type": "always_wrong",
                                            "typeName": "always_wrong",
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения"
                                        }
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->"
                                            ]
                                        },
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "type": "object",
                                "typeName": "priceOrRange"
                            },
                            "categoryId": {
                                "typeName": "positiveIntOrRange",
                                "fields": {
                                    "value": {
                                        "default": {
                                            "typeName": "always_wrong",
                                            "type": "always_wrong",
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения"
                                        },
                                        "typeByCondition": [
                                            {
                                                "constraints": [
                                                    {
                                                        "error": "Количество аргументов в условии не может превышать 20 000",
                                                        "parameters": 20000,
                                                        "name": "itemsCountMax"
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "typeName": "arrayOfPositiveIntegers",
                                                "type": "array",
                                                "items": {
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 0,
                                                    "typeName": "string_or_number",
                                                    "constraints": [
                                                        {
                                                            "name": "maxLength",
                                                            "parameters": 18,
                                                            "error": "Неправильный формат условия: максимальная длина значения - 18 символов"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "parameters": "^\\d+$"
                                                        },
                                                        {
                                                            "name": "minValue",
                                                            "parameters": 0,
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                },
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "=="
                                                    ]
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                                            },
                                            {
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 0,
                                                    "typeName": "string_or_number",
                                                    "constraints": [
                                                        {
                                                            "error": "Неправильный формат условия: максимальная длина значения - 18 символов",
                                                            "parameters": 18,
                                                            "name": "maxLength"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+$"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                            "parameters": 0,
                                                            "name": "minValue"
                                                        }
                                                    ]
                                                },
                                                "type": "array",
                                                "typeName": "arrayOfPositiveIntegers",
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 1,
                                                        "error": "Допускается только одно значение аргумента"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ]
                                            },
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                "condition": {
                                                    "in": [
                                                        "<->"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "items": {
                                                    "typeName": "range",
                                                    "type": "range",
                                                    "frontendPrecision": 0,
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "parameters": "\\S"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                            "parameters": "^\\d+$",
                                                            "name": "boundaryPatternMatch",
                                                            "break": true
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "boundaryMinValue",
                                                            "break": true,
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом"
                                                        },
                                                        {
                                                            "break": true,
                                                            "name": "leftLessRight",
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение categoryId не может быть длиннее 18 символов.",
                                                            "name": "boundaryMaxValue",
                                                            "parameters": 1000000000000000000
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                },
                                                "typeName": "arrayOfPositiveRanges",
                                                "type": "array",
                                                "isCompositeList": 1,
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                                        "parameters": 10,
                                                        "name": "itemsCountMax"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ]
                                            }
                                        ]
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->"
                                            ]
                                        },
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "type": "object",
                                "description": "ID категории"
                            },
                            "store": {
                                "description": "Возможность покупки в розничном магазине",
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' допустимо только значение '1'"
                                            }
                                        ],
                                        "default": {
                                            "flexible": 1,
                                            "items": {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: в значении требуется 0/1"
                                            },
                                            "constraints": [
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                },
                                                {
                                                    "error": "Допускается только одно значение аргумента",
                                                    "parameters": 1,
                                                    "name": "itemsCountMax"
                                                }
                                            ],
                                            "typeName": "array",
                                            "type": "array"
                                        }
                                    }
                                },
                                "typeName": "booleanWithExists"
                            },
                            "id": {
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->"
                                            ]
                                        }
                                    },
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "type": "array",
                                                "typeName": "arrayOfPositiveIntegers",
                                                "items": {
                                                    "constraints": [
                                                        {
                                                            "parameters": 20,
                                                            "name": "maxLength",
                                                            "error": "Неправильный формат условия: максимальная длина значения - 20 символов"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+$"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                            "parameters": 0,
                                                            "name": "minValue"
                                                        }
                                                    ],
                                                    "typeName": "string_or_number",
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 0,
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                },
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "in": [
                                                        "=="
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                                            },
                                            {
                                                "typeName": "arrayOfPositiveIntegers",
                                                "type": "array",
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "error": "Допускается только одно значение аргумента",
                                                        "parameters": 1,
                                                        "name": "itemsCountMax"
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "items": {
                                                    "constraints": [
                                                        {
                                                            "name": "maxLength",
                                                            "parameters": 20,
                                                            "error": "Неправильный формат условия: максимальная длина значения - 20 символов"
                                                        },
                                                        {
                                                            "parameters": "^\\d+$",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                        },
                                                        {
                                                            "name": "minValue",
                                                            "parameters": 0,
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                        }
                                                    ],
                                                    "frontendPrecision": 0,
                                                    "type": "string_or_number",
                                                    "typeName": "string_or_number",
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ]
                                                },
                                                "itemIdAlias": "item_id"
                                            },
                                            {
                                                "items": {
                                                    "typeName": "range",
                                                    "frontendPrecision": 0,
                                                    "type": "range",
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "parameters": "\\S"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                            "name": "boundaryPatternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+$"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "break": true,
                                                            "name": "boundaryMinValue",
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом"
                                                        },
                                                        {
                                                            "break": true,
                                                            "name": "leftLessRight",
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона не может быть длиннее 20 символов.",
                                                            "parameters": 100000000000000000000,
                                                            "name": "boundaryMaxValue"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                },
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "<->"
                                                    ]
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                "constraints": [
                                                    {
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                                        "name": "itemsCountMax",
                                                        "parameters": 10
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "typeName": "arrayOfPositiveRanges",
                                                "isCompositeList": 1,
                                                "type": "array"
                                            }
                                        ],
                                        "default": {
                                            "type": "always_wrong",
                                            "typeName": "always_wrong",
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения"
                                        }
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    }
                                },
                                "typeName": "positiveIntOrRange",
                                "description": "ID товара"
                            },
                            "vendor": {
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike",
                                                "exists"
                                            ]
                                        },
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'"
                                            }
                                        ],
                                        "default": {
                                            "type": "array",
                                            "typeName": "arrayOfStrings",
                                            "constraints": [
                                                {
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50",
                                                    "parameters": 50,
                                                    "name": "itemsCountMax"
                                                },
                                                {
                                                    "parameters": 1,
                                                    "name": "itemsCountMin",
                                                    "error": "Аргумент не указан"
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "name": "minLength",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "name": "maxLength",
                                                        "parameters": 175
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ],
                                                "typeName": "string",
                                                "type": "string"
                                            },
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "itemIdAlias": "item_id"
                                        }
                                    }
                                },
                                "typeName": "likeableStringOrExists",
                                "description": "Производитель"
                            },
                            "manufacturer_warranty": {
                                "description": "Гарантия производителя",
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        },
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' допустимо только значение '1'",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ],
                                        "default": {
                                            "typeName": "array",
                                            "type": "array",
                                            "items": {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: в значении требуется 0/1"
                                            },
                                            "flexible": 1,
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMin",
                                                    "parameters": 1,
                                                    "error": "Аргумент не указан"
                                                },
                                                {
                                                    "error": "Допускается только одно значение аргумента",
                                                    "name": "itemsCountMax",
                                                    "parameters": 1
                                                }
                                            ]
                                        }
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    }
                                },
                                "typeName": "booleanWithExists"
                            },
                            "url": {
                                "typeName": "urlOrLikeableString",
                                "type": "object",
                                "fields": {
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "condition": {
                                                    "in": [
                                                        "=="
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: неверный URL",
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "break": true,
                                                            "name": "minLength",
                                                            "parameters": 1
                                                        },
                                                        {
                                                            "name": "correctUrl",
                                                            "error": "Неправильный формат условия: неверный URL"
                                                        },
                                                        {
                                                            "name": "maxLength",
                                                            "parameters": 175,
                                                            "error": "Слишком длинная строка"
                                                        },
                                                        {
                                                            "name": "containsDisallowedLetters",
                                                            "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                        }
                                                    ],
                                                    "type": "string",
                                                    "typeName": "string"
                                                },
                                                "typeName": "arrayOfUrls",
                                                "isCompositeList": 1,
                                                "type": "array",
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                                        "name": "itemsCountMax",
                                                        "parameters": 10
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "array",
                                                "typeName": "arrayOfStrings",
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "in": [
                                                        "ilike",
                                                        "not ilike"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                                    "type": "string",
                                                    "typeName": "string",
                                                    "constraints": [
                                                        {
                                                            "name": "minLength",
                                                            "parameters": 1,
                                                            "error": "Аргумент не указан"
                                                        },
                                                        {
                                                            "error": "Слишком длинная строка",
                                                            "parameters": 175,
                                                            "name": "maxLength"
                                                        },
                                                        {
                                                            "name": "containsDisallowedLetters",
                                                            "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                        }
                                                    ]
                                                }
                                            }
                                        ],
                                        "default": {
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50",
                                                    "name": "itemsCountMax",
                                                    "parameters": 50
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "parameters": 1,
                                                        "name": "minLength",
                                                        "error": "Аргумент не указан"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "name": "maxLength",
                                                        "parameters": 175
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ],
                                                "typeName": "string",
                                                "type": "string"
                                            },
                                            "type": "array",
                                            "typeName": "arrayOfStrings"
                                        }
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "==",
                                                "ilike",
                                                "not ilike"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "description": "URL товара"
                            },
                            "age": {
                                "fields": {
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: значение должно быть массивом целых чисел",
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "error": "Неправильный формат условия: максимальная длина значения - 20 символов",
                                                        "name": "maxLength",
                                                        "parameters": 20
                                                    },
                                                    {
                                                        "name": "patternMatch",
                                                        "break": true,
                                                        "parameters": "^\\d+$",
                                                        "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                    }
                                                ],
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1",
                                                        "2",
                                                        "3",
                                                        "4",
                                                        "5",
                                                        "6",
                                                        "7",
                                                        "8",
                                                        "9",
                                                        "10",
                                                        "11",
                                                        "12",
                                                        "16",
                                                        "18"
                                                    ]
                                                },
                                                "frontendPrecision": 0,
                                                "typeName": "ageSelect",
                                                "errorOnWrongType": "Неверное значение возрастной категории"
                                            },
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "parameters": 1,
                                                    "name": "itemsCountMax",
                                                    "error": "Допускается только одно значение возрастной категории"
                                                },
                                                {
                                                    "error": "Не задана возрастная категория",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "typeName": "arrayOfPositiveIntegers",
                                            "type": "array"
                                        },
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                }
                                            }
                                        ]
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        }
                                    }
                                },
                                "type": "object",
                                "typeName": "selectableAge",
                                "description": "Возрастная категория"
                            },
                            "adult": {
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        }
                                    },
                                    "value": {
                                        "default": {
                                            "typeName": "array",
                                            "type": "array",
                                            "items": {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: в значении требуется 0/1"
                                            },
                                            "flexible": 1,
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMin",
                                                    "parameters": 1,
                                                    "error": "Аргумент не указан"
                                                },
                                                {
                                                    "error": "Допускается только одно значение аргумента",
                                                    "name": "itemsCountMax",
                                                    "parameters": 1
                                                }
                                            ]
                                        },
                                        "typeByCondition": [
                                            {
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' допустимо только значение '1'",
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                }
                                            }
                                        ]
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    }
                                },
                                "typeName": "booleanWithExists",
                                "description": "Взрослый контент"
                            },
                            "name": {
                                "type": "object",
                                "fields": {
                                    "value": {
                                        "default": {
                                            "type": "array",
                                            "typeName": "arrayOfStrings",
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMax",
                                                    "parameters": 50,
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "name": "itemsCountMin",
                                                    "parameters": 1,
                                                    "error": "Аргумент не указан"
                                                }
                                            ],
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "parameters": 1,
                                                        "name": "minLength",
                                                        "error": "Аргумент не указан"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "name": "maxLength",
                                                        "parameters": 175
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ],
                                                "type": "string",
                                                "typeName": "string"
                                            },
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "itemIdAlias": "item_id"
                                        },
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ]
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike"
                                            ]
                                        },
                                        "typeName": "enum"
                                    }
                                },
                                "typeName": "likeableString",
                                "description": "Название товара"
                            },
                            "pickup": {
                                "description": "Самовывоз",
                                "typeName": "booleanWithExists",
                                "fields": {
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        }
                                    },
                                    "value": {
                                        "default": {
                                            "type": "array",
                                            "typeName": "array",
                                            "constraints": [
                                                {
                                                    "parameters": 1,
                                                    "name": "itemsCountMin",
                                                    "error": "Аргумент не указан"
                                                },
                                                {
                                                    "parameters": 1,
                                                    "name": "itemsCountMax",
                                                    "error": "Допускается только одно значение аргумента"
                                                }
                                            ],
                                            "items": {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: в значении требуется 0/1"
                                            },
                                            "flexible": 1
                                        },
                                        "typeByCondition": [
                                            {
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' допустимо только значение '1'"
                                            }
                                        ]
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    }
                                },
                                "type": "object"
                            },
                            "description": {
                                "description": "Описание товара",
                                "fields": {
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'"
                                            }
                                        ],
                                        "default": {
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "parameters": 1,
                                                        "name": "minLength",
                                                        "error": "Аргумент не указан"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "name": "maxLength",
                                                        "parameters": 175
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ],
                                                "type": "string",
                                                "typeName": "string"
                                            },
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "parameters": 50,
                                                    "name": "itemsCountMax",
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "typeName": "arrayOfStrings",
                                            "type": "array",
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка"
                                        }
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    }
                                },
                                "type": "object",
                                "typeName": "likeableString"
                            },
                            "typePrefix": {
                                "description": "typePrefix",
                                "fields": {
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMax",
                                                    "parameters": 50,
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "parameters": 1,
                                                        "name": "minLength"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "parameters": 175,
                                                        "name": "maxLength"
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                        "name": "containsDisallowedLetters"
                                                    }
                                                ],
                                                "typeName": "string",
                                                "type": "string"
                                            },
                                            "type": "array",
                                            "typeName": "arrayOfStrings"
                                        },
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ]
                                    },
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "type": "object",
                                "typeName": "likeableStringOrExists"
                            },
                            "market_category": {
                                "typeName": "likeableStringOrExists",
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "typeName": "arrayOfStrings",
                                            "type": "array",
                                            "items": {
                                                "type": "string",
                                                "typeName": "string",
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "parameters": 1,
                                                        "name": "minLength"
                                                    },
                                                    {
                                                        "name": "maxLength",
                                                        "parameters": 175,
                                                        "error": "Слишком длинная строка"
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                        "name": "containsDisallowedLetters"
                                                    }
                                                ]
                                            },
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMax",
                                                    "parameters": 50,
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "name": "itemsCountMin",
                                                    "parameters": 1,
                                                    "error": "Аргумент не указан"
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "itemIdAlias": "item_id"
                                        },
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ]
                                    }
                                },
                                "description": "Категория Яндекс.Маркета"
                            },
                            "model": {
                                "description": "Модель",
                                "typeName": "likeableString",
                                "fields": {
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike"
                                            ]
                                        }
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "parameters": 50,
                                                    "name": "itemsCountMax",
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "items": {
                                                "typeName": "string",
                                                "type": "string",
                                                "constraints": [
                                                    {
                                                        "name": "minLength",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "parameters": 175,
                                                        "name": "maxLength"
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                        "name": "containsDisallowedLetters"
                                                    }
                                                ]
                                            },
                                            "type": "array",
                                            "typeName": "arrayOfStrings",
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка"
                                        },
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                }
                                            }
                                        ]
                                    }
                                },
                                "type": "object"
                            }
                        },
                        "itemsOrder": [
                            "categoryId",
                            "vendor",
                            "model",
                            "url",
                            "name",
                            "price",
                            "id",
                            "typePrefix",
                            "description",
                            "adult",
                            "age",
                            "manufacturer_warranty",
                            "market_category",
                            "oldprice",
                            "pickup",
                            "store"
                        ],
                        "translations": {
                            "rule": {
                                "<->": "диапазон",
                                "<": "меньше",
                                "==": "равно",
                                "ilike": "содержит",
                                ">": "больше",
                                "not ilike": "не содержит",
                                "exists": "любое значение"
                            },
                            "boolean": {
                                "0": "False",
                                "1": "True"
                            }
                        },
                        "types": {
                            "rule": {
                                "errorOnWrongType": "Неправильный формат условия: указаны не все обязательные значения",
                                "allowOptionalFields": 1,
                                "typeName": "object",
                                "type": "object",
                                "fields": {
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "items": {
                                            "typeName": "string_or_number",
                                            "type": "string_or_number"
                                        },
                                        "flexible": 1,
                                        "typeName": "array",
                                        "type": "array"
                                    },
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->",
                                                "ilike",
                                                "not ilike",
                                                "exists"
                                            ]
                                        },
                                        "translations": "#/definitions/translations/rule",
                                        "errorOnWrongType": "Недопустимое отношение в правиле №[%id%]"
                                    }
                                }
                            },
                            "likeableStringOrExists": {
                                "typeName": "likeableString",
                                "type": "object",
                                "fields": {
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "itemIdAlias": "item_id",
                                            "type": "array",
                                            "typeName": "arrayOfStrings",
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMax",
                                                    "parameters": 50,
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "parameters": 1,
                                                        "name": "minLength"
                                                    },
                                                    {
                                                        "name": "maxLength",
                                                        "parameters": 175,
                                                        "error": "Слишком длинная строка"
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                        "name": "containsDisallowedLetters"
                                                    }
                                                ],
                                                "typeName": "string",
                                                "type": "string"
                                            }
                                        },
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'"
                                            }
                                        ]
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum"
                                    }
                                }
                            },
                            "selectableAge": {
                                "typeName": "comparableIntWithExists",
                                "fields": {
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "typeName": "arrayOfPositiveIntegers",
                                            "type": "array",
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "name": "maxLength",
                                                        "parameters": 20,
                                                        "error": "Неправильный формат условия: максимальная длина значения - 20 символов"
                                                    },
                                                    {
                                                        "name": "patternMatch",
                                                        "break": true,
                                                        "parameters": "^\\d+$",
                                                        "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                    }
                                                ],
                                                "typeName": "ageSelect",
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1",
                                                        "2",
                                                        "3",
                                                        "4",
                                                        "5",
                                                        "6",
                                                        "7",
                                                        "8",
                                                        "9",
                                                        "10",
                                                        "11",
                                                        "12",
                                                        "16",
                                                        "18"
                                                    ]
                                                },
                                                "frontendPrecision": 0,
                                                "errorOnWrongType": "Неверное значение возрастной категории"
                                            },
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "error": "Допускается только одно значение возрастной категории",
                                                    "parameters": 1,
                                                    "name": "itemsCountMax"
                                                },
                                                {
                                                    "error": "Не задана возрастная категория",
                                                    "name": "itemsCountMin",
                                                    "parameters": 1
                                                }
                                            ],
                                            "errorOnWrongType": "Неправильный формат условия: значение должно быть массивом целых чисел",
                                            "itemIdAlias": "item_id"
                                        },
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'"
                                            }
                                        ]
                                    }
                                },
                                "type": "object"
                            },
                            "urlOrLikeableString": {
                                "typeName": "likeableString",
                                "type": "object",
                                "fields": {
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "ilike",
                                                "not ilike"
                                            ]
                                        }
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "constraints": [
                                                {
                                                    "parameters": 50,
                                                    "name": "itemsCountMax",
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "name": "itemsCountMin",
                                                    "parameters": 1
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "items": {
                                                "type": "string",
                                                "typeName": "string",
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "parameters": 1,
                                                        "name": "minLength"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "parameters": 175,
                                                        "name": "maxLength"
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ]
                                            },
                                            "type": "array",
                                            "typeName": "arrayOfStrings",
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка"
                                        },
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "condition": {
                                                    "in": [
                                                        "=="
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "items": {
                                                    "typeName": "string",
                                                    "type": "string",
                                                    "constraints": [
                                                        {
                                                            "parameters": 1,
                                                            "name": "minLength",
                                                            "break": true,
                                                            "error": "Аргумент не указан"
                                                        },
                                                        {
                                                            "name": "correctUrl",
                                                            "error": "Неправильный формат условия: неверный URL"
                                                        },
                                                        {
                                                            "error": "Слишком длинная строка",
                                                            "parameters": 175,
                                                            "name": "maxLength"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                            "name": "containsDisallowedLetters"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: неверный URL"
                                                },
                                                "typeName": "arrayOfUrls",
                                                "type": "array",
                                                "isCompositeList": 1,
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 10,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ]
                                            },
                                            {
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "name": "itemsCountMin",
                                                        "parameters": 1,
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "typeName": "arrayOfStrings",
                                                "type": "array",
                                                "condition": {
                                                    "in": [
                                                        "ilike",
                                                        "not ilike"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                                    "constraints": [
                                                        {
                                                            "parameters": 1,
                                                            "name": "minLength",
                                                            "error": "Аргумент не указан"
                                                        },
                                                        {
                                                            "name": "maxLength",
                                                            "parameters": 175,
                                                            "error": "Слишком длинная строка"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                            "name": "containsDisallowedLetters"
                                                        }
                                                    ],
                                                    "typeName": "string",
                                                    "type": "string"
                                                }
                                            }
                                        ]
                                    }
                                }
                            },
                            "categoryDefinition": {
                                "typeName": "likeableString",
                                "fields": {
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ],
                                        "default": {
                                            "items": {
                                                "typeName": "string",
                                                "type": "string",
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "minLength",
                                                        "parameters": 1
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "parameters": 175,
                                                        "name": "maxLength"
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ]
                                            },
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "constraints": [
                                                {
                                                    "parameters": 50,
                                                    "name": "itemsCountMax",
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "name": "itemsCountMin",
                                                    "parameters": 1,
                                                    "error": "Аргумент не указан"
                                                }
                                            ],
                                            "typeName": "arrayOfStrings",
                                            "type": "array",
                                            "itemIdAlias": "item_id",
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка"
                                        }
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "=="
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "type": "object"
                            },
                            "arrayOfRanges": {
                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                "itemIdAlias": "item_id",
                                "items": {
                                    "typeName": "range",
                                    "type": "range",
                                    "constraints": [
                                        {
                                            "parameters": "\\S",
                                            "name": "patternMatch",
                                            "break": true,
                                            "error": "Аргумент не указан"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                            "parameters": "^\\d+(\\.\\d+)?$",
                                            "break": true,
                                            "name": "boundaryPatternMatch"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                            "break": true,
                                            "name": "boundaryMinValue",
                                            "parameters": 0
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой",
                                            "break": true,
                                            "name": "leftLessRight"
                                        }
                                    ],
                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                },
                                "typeName": "arrayWithLimits",
                                "type": "array",
                                "isCompositeList": 1,
                                "constraints": [
                                    {
                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                        "name": "itemsCountMax",
                                        "parameters": 10
                                    },
                                    {
                                        "name": "itemsCountMin",
                                        "parameters": 1,
                                        "error": "Аргумент не указан"
                                    }
                                ],
                                "frontendErrorOnWrongType": "Неправильный формат аргумента"
                            },
                            "booleanWithExists": {
                                "typeName": "object",
                                "fields": {
                                    "relation": {
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        },
                                        "typeName": "enum",
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "default": {
                                            "typeName": "array",
                                            "type": "array",
                                            "flexible": 1,
                                            "items": {
                                                "errorOnWrongType": "Неправильный формат условия: в значении требуется 0/1",
                                                "type": {
                                                    "enum": [
                                                        "0",
                                                        "1"
                                                    ]
                                                },
                                                "typeName": "enum"
                                            },
                                            "constraints": [
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                },
                                                {
                                                    "error": "Допускается только одно значение аргумента",
                                                    "parameters": 1,
                                                    "name": "itemsCountMax"
                                                }
                                            ]
                                        },
                                        "typeByCondition": [
                                            {
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' допустимо только значение '1'",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ]
                                    }
                                },
                                "type": "object"
                            },
                            "positiveIntOrRange": {
                                "typeName": "object",
                                "type": "object",
                                "fields": {
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "condition": {
                                                    "in": [
                                                        "=="
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "items": {
                                                    "constraints": [
                                                        {
                                                            "error": "Неправильный формат условия: максимальная длина значения - 20 символов",
                                                            "parameters": 20,
                                                            "name": "maxLength"
                                                        },
                                                        {
                                                            "parameters": "^\\d+$",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                        },
                                                        {
                                                            "name": "minValue",
                                                            "parameters": 0,
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                        }
                                                    ],
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 0,
                                                    "typeName": "string_or_number",
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                },
                                                "typeName": "arrayOfPositiveIntegers",
                                                "type": "array",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента"
                                            },
                                            {
                                                "typeName": "arrayOfPositiveIntegers",
                                                "type": "array",
                                                "constraints": [
                                                    {
                                                        "error": "Допускается только одно значение аргумента",
                                                        "parameters": 1,
                                                        "name": "itemsCountMax"
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "items": {
                                                    "constraints": [
                                                        {
                                                            "parameters": 20,
                                                            "name": "maxLength",
                                                            "error": "Неправильный формат условия: максимальная длина значения - 20 символов"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "parameters": "^\\d+$"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "minValue",
                                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                        }
                                                    ],
                                                    "frontendPrecision": 0,
                                                    "type": "string_or_number",
                                                    "typeName": "string_or_number",
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом"
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ]
                                                },
                                                "itemIdAlias": "item_id"
                                            },
                                            {
                                                "constraints": [
                                                    {
                                                        "parameters": 10,
                                                        "name": "itemsCountMax",
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "isCompositeList": 1,
                                                "type": "array",
                                                "typeName": "arrayOfPositiveRanges",
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "<->"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "parameters": "\\S",
                                                            "name": "patternMatch",
                                                            "break": true
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                            "parameters": "^\\d+$",
                                                            "break": true,
                                                            "name": "boundaryPatternMatch"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                                            "parameters": 0,
                                                            "name": "boundaryMinValue",
                                                            "break": true
                                                        },
                                                        {
                                                            "break": true,
                                                            "name": "leftLessRight",
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой"
                                                        },
                                                        {
                                                            "parameters": 100000000000000000000,
                                                            "name": "boundaryMaxValue",
                                                            "error": "Неправильный формат условия: значение границы диапазона не может быть длиннее 20 символов."
                                                        }
                                                    ],
                                                    "frontendPrecision": 0,
                                                    "type": "range",
                                                    "typeName": "range"
                                                }
                                            }
                                        ],
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения",
                                            "typeName": "always_wrong",
                                            "type": "always_wrong"
                                        }
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->"
                                            ]
                                        }
                                    }
                                }
                            },
                            "ageSelect": {
                                "type": {
                                    "enum": [
                                        "0",
                                        "1",
                                        "2",
                                        "3",
                                        "4",
                                        "5",
                                        "6",
                                        "7",
                                        "8",
                                        "9",
                                        "10",
                                        "11",
                                        "12",
                                        "16",
                                        "18"
                                    ]
                                },
                                "typeName": "enum"
                            },
                            "priceOrRange": {
                                "typeName": "positiveIntOrRange",
                                "fields": {
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "name": "itemsCountMax",
                                                        "parameters": 50,
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ],
                                                "typeName": "arrayOfNumbers",
                                                "type": "array",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом",
                                                    "typeName": "string_or_number",
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "constraints": [
                                                        {
                                                            "break": true,
                                                            "name": "patternMatch",
                                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "minValue",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        }
                                                    ]
                                                },
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "=="
                                                    ]
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                                            },
                                            {
                                                "constraints": [
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMax",
                                                        "error": "Допускается только одно значение аргумента"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "parameters": 1,
                                                        "name": "itemsCountMin"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "type": "array",
                                                "typeName": "arrayOfNumbers",
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом",
                                                    "constraints": [
                                                        {
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "minValue",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        }
                                                    ],
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "typeName": "string_or_number"
                                                }
                                            },
                                            {
                                                "type": "array",
                                                "isCompositeList": 1,
                                                "typeName": "arrayOfRanges",
                                                "constraints": [
                                                    {
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                                        "name": "itemsCountMax",
                                                        "parameters": 10
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "in": [
                                                        "<->"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "items": {
                                                    "frontendPrecision": 2,
                                                    "type": "range",
                                                    "typeName": "range",
                                                    "constraints": [
                                                        {
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "\\S",
                                                            "error": "Аргумент не указан"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                                            "name": "boundaryPatternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+(\\.\\d+)?$"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                                            "parameters": 0,
                                                            "name": "boundaryMinValue",
                                                            "break": true
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой",
                                                            "break": true,
                                                            "name": "leftLessRight"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                }
                                            },
                                            {
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ],
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения",
                                            "type": "always_wrong",
                                            "typeName": "always_wrong"
                                        }
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->"
                                            ]
                                        }
                                    }
                                },
                                "type": "object"
                            },
                            "selectableCurrency": {
                                "typeName": "rule",
                                "fields": {
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "value": {
                                        "typeName": "array",
                                        "type": "array",
                                        "flexible": 1,
                                        "items": {
                                            "errorOnWrongType": "Неверное значение валюты",
                                            "typeName": "currencySelect",
                                            "type": {
                                                "enum": [
                                                    "RUR",
                                                    "USD",
                                                    "UAH",
                                                    "KZT"
                                                ]
                                            }
                                        }
                                    },
                                    "relation": {
                                        "translations": "#/definitions/translations/rule",
                                        "errorOnWrongType": "Недопустимое отношение в правиле №[%id%]",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "=="
                                            ]
                                        }
                                    }
                                },
                                "type": "object",
                                "errorOnWrongType": "Неправильный формат условия: указаны не все обязательные значения",
                                "allowOptionalFields": 1
                            },
                            "arrayOfPositiveIntegers": {
                                "itemIdAlias": "item_id",
                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                "constraints": [
                                    {
                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                        "parameters": 10,
                                        "name": "itemsCountMax"
                                    },
                                    {
                                        "name": "itemsCountMin",
                                        "parameters": 1,
                                        "error": "Аргумент не указан"
                                    }
                                ],
                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                "items": {
                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть целым положительным числом",
                                    "constraints": [
                                        {
                                            "parameters": 20,
                                            "name": "maxLength",
                                            "error": "Неправильный формат условия: максимальная длина значения - 20 символов"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                            "name": "patternMatch",
                                            "break": true,
                                            "parameters": "^\\d+$"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                            "name": "minValue",
                                            "parameters": 0
                                        }
                                    ],
                                    "typeName": "string_or_number",
                                    "frontendPrecision": 0,
                                    "type": "string_or_number"
                                },
                                "type": "array",
                                "typeName": "arrayWithLimits"
                            },
                            "comparableIntWithExists": {
                                "typeName": "object",
                                "type": "object",
                                "fields": {
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "typeName": "enum"
                                            }
                                        ],
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: значение должно быть массивом целых чисел",
                                            "itemIdAlias": "item_id",
                                            "type": "array",
                                            "typeName": "arrayOfPositiveIntegers",
                                            "constraints": [
                                                {
                                                    "parameters": 50,
                                                    "name": "itemsCountMax",
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "name": "itemsCountMin",
                                                    "parameters": 1,
                                                    "error": "Аргумент не указан"
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                            "items": {
                                                "errorOnWrongType": "Неправильный формат элемента условия: значение должно быть целым числом",
                                                "type": "string_or_number",
                                                "frontendPrecision": 0,
                                                "typeName": "string_or_number",
                                                "constraints": [
                                                    {
                                                        "error": "Неправильный формат условия: максимальная длина значения - 20 символов",
                                                        "parameters": 20,
                                                        "name": "maxLength"
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                        "name": "patternMatch",
                                                        "break": true,
                                                        "parameters": "^\\d+$"
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение должно быть целым положительным числом",
                                                        "parameters": 0,
                                                        "name": "minValue"
                                                    }
                                                ]
                                            }
                                        }
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        }
                                    }
                                }
                            },
                            "priceOrRangeOrExists": {
                                "fields": {
                                    "value": {
                                        "default": {
                                            "typeName": "always_wrong",
                                            "type": "always_wrong",
                                            "errorOnWrongType": "Неправильный формат условия: для заданного отношения не определен тип значения"
                                        },
                                        "typeByCondition": [
                                            {
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом",
                                                    "typeName": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "type": "string_or_number",
                                                    "constraints": [
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+(?:\\.\\d+)?$"
                                                        },
                                                        {
                                                            "parameters": 0,
                                                            "name": "minValue",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        }
                                                    ]
                                                },
                                                "itemIdAlias": "item_id",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "=="
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "constraints": [
                                                    {
                                                        "parameters": 50,
                                                        "name": "itemsCountMax",
                                                        "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                    },
                                                    {
                                                        "parameters": 1,
                                                        "name": "itemsCountMin",
                                                        "error": "Аргумент не указан"
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "type": "array",
                                                "typeName": "arrayOfNumbers"
                                            },
                                            {
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        ">",
                                                        "<"
                                                    ]
                                                },
                                                "itemIdAlias": "item_id",
                                                "items": {
                                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом",
                                                    "type": "string_or_number",
                                                    "frontendPrecision": 2,
                                                    "typeName": "string_or_number",
                                                    "constraints": [
                                                        {
                                                            "name": "patternMatch",
                                                            "break": true,
                                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                                            "name": "minValue",
                                                            "parameters": 0
                                                        }
                                                    ]
                                                },
                                                "typeName": "arrayOfNumbers",
                                                "type": "array",
                                                "constraints": [
                                                    {
                                                        "error": "Допускается только одно значение аргумента",
                                                        "parameters": 1,
                                                        "name": "itemsCountMax"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ],
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента"
                                            },
                                            {
                                                "condition": {
                                                    "in": [
                                                        "<->"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "itemIdAlias": "item_id",
                                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                                "items": {
                                                    "typeName": "range",
                                                    "frontendPrecision": 2,
                                                    "type": "range",
                                                    "constraints": [
                                                        {
                                                            "error": "Аргумент не указан",
                                                            "parameters": "\\S",
                                                            "break": true,
                                                            "name": "patternMatch"
                                                        },
                                                        {
                                                            "parameters": "^\\d+(\\.\\d+)?$",
                                                            "name": "boundaryPatternMatch",
                                                            "break": true,
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                        },
                                                        {
                                                            "error": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                                            "break": true,
                                                            "name": "boundaryMinValue",
                                                            "parameters": 0
                                                        },
                                                        {
                                                            "break": true,
                                                            "name": "leftLessRight",
                                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой"
                                                        }
                                                    ],
                                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом"
                                                },
                                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                                "constraints": [
                                                    {
                                                        "parameters": 10,
                                                        "name": "itemsCountMax",
                                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                                    },
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "itemsCountMin",
                                                        "parameters": 1
                                                    }
                                                ],
                                                "typeName": "arrayOfRanges",
                                                "type": "array",
                                                "isCompositeList": 1
                                            },
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'"
                                            }
                                        ]
                                    },
                                    "field": {
                                        "type": "string",
                                        "typeName": "string"
                                    },
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                ">",
                                                "<",
                                                "==",
                                                "<->",
                                                "exists"
                                            ]
                                        },
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "type": "object",
                                "typeName": "priceOrRange"
                            },
                            "arrayOfUrls": {
                                "items": {
                                    "constraints": [
                                        {
                                            "error": "Аргумент не указан",
                                            "break": true,
                                            "name": "minLength",
                                            "parameters": 1
                                        },
                                        {
                                            "name": "correctUrl",
                                            "error": "Неправильный формат условия: неверный URL"
                                        },
                                        {
                                            "error": "Слишком длинная строка",
                                            "name": "maxLength",
                                            "parameters": 175
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                            "name": "containsDisallowedLetters"
                                        }
                                    ],
                                    "type": "string",
                                    "typeName": "string"
                                },
                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                "itemIdAlias": "item_id",
                                "isCompositeList": 1,
                                "type": "array",
                                "typeName": "arrayWithLimits",
                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                "constraints": [
                                    {
                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                        "parameters": 10,
                                        "name": "itemsCountMax"
                                    },
                                    {
                                        "error": "Аргумент не указан",
                                        "name": "itemsCountMin",
                                        "parameters": 1
                                    }
                                ]
                            },
                            "arrayOfStrings": {
                                "typeName": "arrayWithLimits",
                                "type": "array",
                                "items": {
                                    "typeName": "string",
                                    "type": "string",
                                    "constraints": [
                                        {
                                            "error": "Аргумент не указан",
                                            "name": "minLength",
                                            "parameters": 1
                                        },
                                        {
                                            "parameters": 175,
                                            "name": "maxLength",
                                            "error": "Слишком длинная строка"
                                        },
                                        {
                                            "name": "containsDisallowedLetters",
                                            "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                        }
                                    ]
                                },
                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                "constraints": [
                                    {
                                        "parameters": 10,
                                        "name": "itemsCountMax",
                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                    },
                                    {
                                        "parameters": 1,
                                        "name": "itemsCountMin",
                                        "error": "Аргумент не указан"
                                    }
                                ],
                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]",
                                "itemIdAlias": "item_id"
                            },
                            "arrayWithLimits": {
                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                "constraints": [
                                    {
                                        "error": "Количество аргументов в условии должно быть от 1 до 10",
                                        "parameters": 10,
                                        "name": "itemsCountMax"
                                    },
                                    {
                                        "error": "Аргумент не указан",
                                        "parameters": 1,
                                        "name": "itemsCountMin"
                                    }
                                ],
                                "typeName": "array",
                                "type": "array",
                                "itemIdAlias": "item_id",
                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                            },
                            "arrayOfPositiveRanges": {
                                "typeName": "arrayOfRanges",
                                "type": "array",
                                "isCompositeList": 1,
                                "constraints": [
                                    {
                                        "name": "itemsCountMax",
                                        "parameters": 10,
                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                    },
                                    {
                                        "error": "Аргумент не указан",
                                        "name": "itemsCountMin",
                                        "parameters": 1
                                    }
                                ],
                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                "items": {
                                    "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть положительным числом",
                                    "typeName": "range",
                                    "type": "range",
                                    "frontendPrecision": 0,
                                    "constraints": [
                                        {
                                            "error": "Аргумент не указан",
                                            "break": true,
                                            "name": "patternMatch",
                                            "parameters": "\\S"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                            "parameters": "^\\d+$",
                                            "name": "boundaryPatternMatch",
                                            "break": true
                                        },
                                        {
                                            "parameters": 0,
                                            "break": true,
                                            "name": "boundaryMinValue",
                                            "error": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом"
                                        },
                                        {
                                            "name": "leftLessRight",
                                            "break": true,
                                            "error": "Неправильный формат условия: значение правой границы диапазона должно быть больше, чем значение левой"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение границы диапазона не может быть длиннее 20 символов.",
                                            "name": "boundaryMaxValue",
                                            "parameters": 100000000000000000000
                                        }
                                    ]
                                },
                                "errorOnWrongType": "Неправильный формат условия: значение границы диапазона должно быть целым положительным числом",
                                "itemIdAlias": "item_id"
                            },
                            "likeableString": {
                                "fields": {
                                    "value": {
                                        "typeByCondition": [
                                            {
                                                "condition": {
                                                    "field": "relation",
                                                    "in": [
                                                        "exists"
                                                    ]
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'",
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                }
                                            }
                                        ],
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "itemIdAlias": "item_id",
                                            "typeName": "arrayOfStrings",
                                            "type": "array",
                                            "items": {
                                                "typeName": "string",
                                                "type": "string",
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "parameters": 1,
                                                        "name": "minLength"
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "parameters": 175,
                                                        "name": "maxLength"
                                                    },
                                                    {
                                                        "name": "containsDisallowedLetters",
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы"
                                                    }
                                                ]
                                            },
                                            "constraints": [
                                                {
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50",
                                                    "name": "itemsCountMax",
                                                    "parameters": 50
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента"
                                        }
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "relation": {
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение",
                                        "type": {
                                            "enum": [
                                                "ilike",
                                                "not ilike"
                                            ]
                                        },
                                        "typeName": "enum"
                                    }
                                },
                                "type": "object",
                                "typeName": "object"
                            },
                            "currencySelect": {
                                "type": {
                                    "enum": [
                                        "RUR",
                                        "USD",
                                        "UAH",
                                        "KZT"
                                    ]
                                },
                                "typeName": "enum"
                            },
                            "arrayOfNumbers": {
                                "constraints": [
                                    {
                                        "parameters": 10,
                                        "name": "itemsCountMax",
                                        "error": "Количество аргументов в условии должно быть от 1 до 10"
                                    },
                                    {
                                        "parameters": 1,
                                        "name": "itemsCountMin",
                                        "error": "Аргумент не указан"
                                    }
                                ],
                                "frontendErrorOnWrongType": "Неправильный формат аргумента",
                                "items": {
                                    "constraints": [
                                        {
                                            "parameters": "^\\d+(?:\\.\\d+)?$",
                                            "name": "patternMatch",
                                            "break": true,
                                            "error": "Неправильный формат условия: значение должно быть положительным числом"
                                        },
                                        {
                                            "error": "Неправильный формат условия: значение должно быть положительным числом",
                                            "name": "minValue",
                                            "parameters": 0
                                        }
                                    ],
                                    "typeName": "string_or_number",
                                    "type": "string_or_number",
                                    "errorOnWrongType": "Неправильный формат условия: значение должно быть положительным числом"
                                },
                                "type": "array",
                                "typeName": "arrayWithLimits",
                                "itemIdAlias": "item_id",
                                "errorOnWrongType": "Неправильный формат аргумента для условия №[%id%]"
                            },
                            "countryOfOrigin": {
                                "type": "object",
                                "fields": {
                                    "value": {
                                        "default": {
                                            "errorOnWrongType": "Неправильный формат условия: неверная строка",
                                            "itemIdAlias": "item_id",
                                            "typeName": "arrayOfStrings",
                                            "type": "array",
                                            "items": {
                                                "constraints": [
                                                    {
                                                        "error": "Аргумент не указан",
                                                        "name": "minLength",
                                                        "parameters": 1
                                                    },
                                                    {
                                                        "error": "Слишком длинная строка",
                                                        "name": "maxLength",
                                                        "parameters": 175
                                                    },
                                                    {
                                                        "error": "Неправильный формат условия: значение содержит недопустимые символы",
                                                        "name": "containsDisallowedLetters"
                                                    }
                                                ],
                                                "type": "string",
                                                "typeName": "string"
                                            },
                                            "constraints": [
                                                {
                                                    "name": "itemsCountMax",
                                                    "parameters": 50,
                                                    "error": "Количество аргументов в условии должно быть от 1 до 50"
                                                },
                                                {
                                                    "error": "Аргумент не указан",
                                                    "parameters": 1,
                                                    "name": "itemsCountMin"
                                                }
                                            ],
                                            "frontendErrorOnWrongType": "Неправильный формат аргумента"
                                        },
                                        "typeByCondition": [
                                            {
                                                "typeName": "enum",
                                                "type": {
                                                    "enum": [
                                                        1
                                                    ]
                                                },
                                                "condition": {
                                                    "in": [
                                                        "exists"
                                                    ],
                                                    "field": "relation"
                                                },
                                                "errorOnWrongType": "Неправильный формат условия: для отношения 'exists' значение может быть только '1'"
                                            }
                                        ]
                                    },
                                    "field": {
                                        "typeName": "string",
                                        "type": "string"
                                    },
                                    "relation": {
                                        "typeName": "enum",
                                        "type": {
                                            "enum": [
                                                "==",
                                                "exists"
                                            ]
                                        },
                                        "errorOnWrongType": "Неправильный формат условия: задано недопустимое отношение"
                                    }
                                },
                                "typeName": "likeableString"
                            }
                        },
                        "all_relations": [
                            ">",
                            "<",
                            "==",
                            "<->",
                            "ilike",
                            "not ilike",
                            "exists"
                        ],
                        "frontendExtendedFieldsFrom": 8
                    }
                }
            }
        }
    });
})();
