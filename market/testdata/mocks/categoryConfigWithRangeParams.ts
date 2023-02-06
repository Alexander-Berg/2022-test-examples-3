export const CATEGORY_CONFIG_WITH_RANGE_PARAMS_MOCK = {
    categoryId: '7811903',
    name: 'Брюки',
    fullCategoryName: 'Все товары/Одежда, обувь и аксессуары/Женская одежда/Брюки',
    pictureRequired: false,
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
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17896311],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17862552],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [7912816],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17862203],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [20628790],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17631777],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17861790],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [22127070],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [8430324],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17631800],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [7912815],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [13887626],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17862220],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [17862349],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        5,
                                        {
                                            operation: 'ifExists',
                                            params: [14885181],
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
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978745],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978747],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15060326],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978746],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978749],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978748],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978751],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978750],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [24516990],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14885227],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [19742630],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15168617],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14885235],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14885239],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [16156142],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [24516972],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [13869903],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17577233],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [23535610],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [19908750],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17446443],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15756760],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14020987],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15086295],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [19264150],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14202862],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [13341555],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7351753],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [24516815],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7351754],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15168861],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [24516813],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7351757],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17862447],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474268],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17937233],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [20639610],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15728430],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474270],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474264],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [16378175],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474267],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17631818],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14871214],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [8217776],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [16824144],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978920],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17456351],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17551827],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [20639590],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [8231714],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [8307102],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [8432992],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [17863026],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [15180930],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [8307097],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [24517016],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [24517017],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978763],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474408],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474409],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978764],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978753],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474405],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978752],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978755],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978754],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [23670290],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978757],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [20642630],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7979140],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474273],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978756],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [14474274],
                                        },
                                    ],
                                },
                                {
                                    operation: 'multiply',
                                    params: [
                                        0.79,
                                        {
                                            operation: 'ifExists',
                                            params: [7978758],
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
    importantParams: [
        17631800,
        17862203,
        17862349,
        13887626,
        17862552,
        8430324,
        7912815,
        17861790,
        17631777,
        17896311,
        7912816,
        20628790,
        17862220,
    ],
    parameterList: [
        {
            parameterId: 14474267,
            parameterName: 'Размеры: ж - обхват талии',
            parameterType: 'ENUM',
            description: '',
            multivalue: false,
            partnerValue: true,
            validation: {
                presence: true,
            },
            dependsOnVendor: true,
            options: [],
            skuMode: 'SKU_DEFINING',
        },
        {
            parameterId: 24516813,
            parameterName: 'Обхват талии (min)',
            parameterType: 'NUMERIC',
            description: '',
            multivalue: false,
            partnerValue: true,
            validation: {
                presence: true,
            },
            unit: 'см',
            dependsOnVendor: true,
            skuMode: 'SKU_INFORMATIONAL',
            minValue: 0,
            maxValue: 1000,
        },
        {
            parameterId: 24516815,
            parameterName: 'Обхват талии (max)',
            parameterType: 'NUMERIC',
            description: '',
            multivalue: false,
            partnerValue: true,
            unit: 'см',
            dependsOnVendor: true,
            skuMode: 'SKU_INFORMATIONAL',
            minValue: 0,
            maxValue: 1000,
        },
        {
            parameterId: 14474273,
            parameterName: 'Размеры: ж - рост',
            parameterType: 'ENUM',
            description: '',
            multivalue: false,
            partnerValue: true,
            validation: {
                presence: true,
            },
            dependsOnVendor: true,
            options: [],
            skuMode: 'SKU_DEFINING',
        },
        {
            parameterId: 24517016,
            parameterName: 'Рост (min)',
            parameterType: 'NUMERIC',
            description: '',
            multivalue: false,
            partnerValue: true,
            validation: {
                presence: true,
            },
            unit: 'см',
            dependsOnVendor: true,
            skuMode: 'SKU_INFORMATIONAL',
            minValue: 0,
            maxValue: 1000,
        },
        {
            parameterId: 24517017,
            parameterName: 'Рост (max)',
            parameterType: 'NUMERIC',
            description: '',
            multivalue: false,
            partnerValue: true,
            unit: 'см',
            dependsOnVendor: true,
            skuMode: 'SKU_INFORMATIONAL',
            minValue: 0,
            maxValue: 1000,
        },
        {
            parameterId: 24516972,
            parameterName: 'Обхват бедер (min)',
            parameterType: 'NUMERIC',
            description: '',
            multivalue: false,
            partnerValue: true,
            unit: 'см',
            skuMode: 'SKU_INFORMATIONAL',
            minValue: 0,
            maxValue: 1000,
        },
        {
            parameterId: 24516990,
            parameterName: 'Обхват бедер (max)',
            parameterType: 'NUMERIC',
            description: '',
            multivalue: false,
            partnerValue: true,
            unit: 'см',
            skuMode: 'SKU_INFORMATIONAL',
            minValue: 0,
            maxValue: 1000,
        },
    ],
    layout: {
        sections: [
            {
                id: 'sku',
                groups: [
                    {
                        title: 'Характеристики варианта',
                        description: '',
                        parameterReferences: [
                            {
                                paramId: 14474267,
                                range: {
                                    fromParamId: 24516813,
                                    toParamId: 24516815,
                                },
                            },
                            {
                                paramId: 14474273,
                                range: {
                                    fromParamId: 24517016,
                                    toParamId: 24517017,
                                },
                            },
                            {
                                range: {
                                    fromParamId: 24516972,
                                    toParamId: 24516990,
                                },
                            },
                        ],
                    },
                ],
            },
            {
                id: 'model',
                groups: [],
            },
        ],
    },
    missingParameterFormParserData: false,
};
