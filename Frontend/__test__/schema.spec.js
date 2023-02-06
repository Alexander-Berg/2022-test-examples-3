const {
    getDependenciesAndCollectArrayTypes,
    processSchema,
} = require('../schema');

const testSchema = {
    $schema: 'http://json-schema.org/draft-07/schema#',
    definitions: {
        'SearchData.Nested': {
            additionalProperties: false,
            properties: {
                complex: {
                    $ref: '#/definitions/SearchData.Complex',
                },
            },
            required: [
                'complex',
            ],
            type: 'object',
        },
        'SearchData.DiverseArray': {
            additionalProperties: false,
            properties: {
                children: {
                    items: {
                        anyOf: [
                            {
                                $ref: '#/definitions/SearchData.Basic',
                            },
                            {
                                $ref: '#/definitions/SearchData.ComplexObject',
                            },
                        ],
                    },
                    type: 'array',
                },
                id: {
                    $ref: '#/definitions/SearchData.BasicId',
                },
            },
            required: [
                'children',
                'id',
            ],
            type: 'object',
        },
        'SearchData.ComplexObject': {
            additionalProperties: false,
            properties: {
                rich_info: {
                    additionalProperties: false,
                    properties: {
                        vh_meta: {
                            additionalProperties: false,
                            properties: {
                                content_groups: {
                                    items: {
                                        $ref: '#/definitions/SearchData.BasicDuration',
                                    },
                                    type: 'array',
                                },
                            },
                            type: 'object',
                        },
                        basic: {
                            $ref: '#/definitions/SearchData.Basic',
                        },
                    },
                    type: 'object',
                },
            },
            required: [
                'properties',
            ],
            type: 'object',
        },
        'SearchData.Complex': {
            additionalProperties: false,
            properties: {
                basic: {
                    $ref: '#/definitions/SearchData.Basic',
                },
            },
            required: [
                'basic',
            ],
            type: 'object',
        },
        'SearchData.Basic': {
            additionalProperties: false,
            properties: {
                avatar: {
                    description: 'URL картинки для отображения в заголовке карусели',
                    type: 'string',
                },
            },
            required: [
                'avatar',
            ],
            type: 'object',
        },
        'SearchData.BasicDuration': {
            additionalProperties: false,
            properties: {
                duration: {
                    type: 'number',
                },
            },
            required: [
                'duration',
            ],
            type: 'object',
        },
        'SearchData.BasicId': {
            additionalProperties: false,
            properties: {
                id: {
                    type: 'string',
                },
            },
            required: [
                'id',
            ],
            type: 'object',
        },
    },
};

const proseccedSchema = JSON.parse(JSON.stringify(testSchema));
proseccedSchema.definitions['SearchData.ComplexObject'].additionalProperties = true;
proseccedSchema.definitions['SearchData.Basic'].additionalProperties = true;
proseccedSchema.definitions['SearchData.BasicDuration'].additionalProperties = true;

describe('Функция getDependenciesAndCollectArrayTypes', () => {
    it('Правильно получает зависимости первого уровня вложенности', () => {
        let anyOf = new Set();

        expect(
            getDependenciesAndCollectArrayTypes(testSchema.definitions['SearchData.Basic'], anyOf),
        ).toEqual([]);
        expect(anyOf.size).toEqual(0);

        expect(
            getDependenciesAndCollectArrayTypes(testSchema.definitions['SearchData.Complex'], anyOf),
        ).toEqual(['SearchData.Basic']);
        expect(anyOf.size).toEqual(0);

        expect(
            getDependenciesAndCollectArrayTypes(testSchema.definitions['SearchData.Nested'], anyOf),
        ).toEqual(['SearchData.Complex']);
        expect(anyOf.size).toEqual(0);
    });

    it('Правильно получает зависимости во вложенных объектах и массивах', () => {
        let anyOf = new Set();

        expect(
            getDependenciesAndCollectArrayTypes(testSchema.definitions['SearchData.ComplexObject'], anyOf)
                .sort(),
        ).toEqual(['SearchData.Basic', 'SearchData.BasicDuration'].sort());
        expect(anyOf.size).toEqual(0);
    });

    it('Правильно заполняет типы anyOf для разнородных массивов', () => {
        let anyOf = new Set();

        expect(
            getDependenciesAndCollectArrayTypes(testSchema.definitions['SearchData.DiverseArray'], anyOf)
                .sort(),
        ).toEqual(['SearchData.Basic', 'SearchData.ComplexObject', 'SearchData.BasicId'].sort());
        expect([...anyOf.values()].sort()).toEqual(['SearchData.Basic', 'SearchData.ComplexObject'].sort());
    });
});

describe('Функция processSchema', () => {
    it('Правильно преобразует схему', () => {
        const schemaCopy = JSON.parse(JSON.stringify(testSchema));
        processSchema(schemaCopy);

        expect(schemaCopy).toEqual(proseccedSchema);
    });
});
