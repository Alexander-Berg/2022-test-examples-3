/*eslint-disable no-magic-numbers*/
import { COINS_PER_RUBLE } from '../../../constants';
import { controlType, SchemaItemVisual } from '../types';
import { FormConstructorValuesWorker } from './index';

describe('FormConstructorValuesWorker', () => {

    describe('Format values', () => {
        describe('Format string values', () => {
            it('Format null string value', () => {
                const SCHEMA = {
                    string_field: { type: controlType.string },
                };

                const VALUES = {
                    string_field: null,
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format empty string value', () => {
                const SCHEMA = {
                    string_field: { type: controlType.string, default: '' },
                };

                const VALUES = {
                    string_field: '',
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format string value', () => {
                const SCHEMA = {
                    string_field: { type: controlType.string, default: 'string_value' },
                };

                const VALUES = { string_field: 'string_value' };

                const RESULT = { string_field: 'string_value' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Format json values', () => {
            it('Format null json value', () => {
                const SCHEMA = {
                    json_field: { type: controlType.json },
                };

                const VALUES = {
                    json_field: null,
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format empty json value', () => {
                const SCHEMA = {
                    json_field: { type: controlType.json, default: '' },
                };

                const VALUES = {
                    json_field: '',
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format invalid json value', () => {
                const SCHEMA = {
                    json_field: { type: controlType.json, default: '(}' },
                };

                const VALUES = { json_field: '(}' };

                const RESULT = { json_field: '(}' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format valid json value', () => {
                const SCHEMA = {
                    json_field: { type: controlType.json, default: '{}' },
                };

                const VALUES = { json_field: '{}' };

                const RESULT = { json_field: {} };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Format numeric values', () => {
            it('Format null numeric value', () => {
                const SCHEMA = {
                    numeric_field: { type: controlType.numeric },
                };

                const VALUES = {
                    numeric_field: null,
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format empty string-numeric value', () => {
                const SCHEMA = {
                    numeric_field: { type: controlType.numeric, default: '' },
                };

                const VALUES = {
                    numeric_field: '',
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format zero numeric value', () => {
                const SCHEMA = {
                    numeric_field: { type: controlType.numeric, default: 0 },
                };

                const VALUES = { numeric_field: 0 };

                const RESULT = { numeric_field: 0 };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format numeric value', () => {
                const DEFAULT_VALUE = 5;
                const SCHEMA = {
                    numeric_field: { type: controlType.numeric, default: DEFAULT_VALUE },
                };
                const VALUES = { numeric_field: DEFAULT_VALUE };
                const RESULT = { numeric_field: DEFAULT_VALUE };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format rubs numeric value', () => {
                const DEFAULT_VALUE = 5;
                const SCHEMA = {
                    numeric_field: { type: controlType.numeric, visual: SchemaItemVisual.RUBS, default: DEFAULT_VALUE },
                };
                const VALUES = { numeric_field: DEFAULT_VALUE };
                const RESULT = { numeric_field: Math.floor(Math.fround(+DEFAULT_VALUE * COINS_PER_RUBLE)) };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format money numeric value', () => {
                const DEFAULT_VALUE = 5;
                const SCHEMA = {
                    numeric_field: {
                        type: controlType.numeric,
                        visual: SchemaItemVisual.MONEY,
                        default: DEFAULT_VALUE,
                    },
                };
                const VALUES = { numeric_field: DEFAULT_VALUE };
                const RESULT = { numeric_field: Math.floor(Math.fround(+DEFAULT_VALUE * COINS_PER_RUBLE)) };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Format array values', () => {
            it('Format null array value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.string } },
                };

                const VALUES = {
                    array_field: null,
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format empty array value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.string } },
                };

                const VALUES = {
                    array_field: [],
                };

                const RESULT = { array_field: [] };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element string value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.string } },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: ['a', 'c'],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element string value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            string_field: { type: controlType.string },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { string_field: 'a' },
                        { string_field: '' },
                        { string_field: 'b' },
                        { string_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [{ string_field: 'a' }, {}, { string_field: 'b' }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element numeric value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.numeric } },
                };

                const VALUES = {
                    array_field: [0, '', 1, null],
                };

                const RESULT = {
                    array_field: [0, 1],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element numeric value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            numeric_field: { type: controlType.numeric },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { numeric_field: 0 },
                        { numeric_field: '' },
                        { numeric_field: 1 },
                        { numeric_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [{ numeric_field: 0 }, {}, { numeric_field: 1 }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element text value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.text } },
                };

                const VALUES = {
                    array_field: ['some text', '', 'another text', null],
                };

                const RESULT = {
                    array_field: ['some text', 'another text'],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element text value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            text_field: { type: controlType.text },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { text_field: 'some text' },
                        { text_field: '' },
                        { text_field: 'some text' },
                        { text_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [{ text_field: 'some text' }, {}, { text_field: 'some text' }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element bool value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.bool } },
                };

                const VALUES = {
                    array_field: [true, '', false, null],
                };

                const RESULT = {
                    array_field: [true, false],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element bool value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            bool_field: {
                                type: controlType.bool,
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { bool_field: true },
                        { bool_field: '' },
                        { bool_field: false },
                        { bool_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [{ bool_field: true }, {}, { bool_field: false }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element json value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.json } },
                };

                const VALUES = {
                    array_field: ['{}', '', null, '{"a": 1}'],
                };

                const RESULT = {
                    array_field: [{}, { a: 1 }],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element json value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            json_field: { type: controlType.json },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { json_field: '{}' },
                        { json_field: '' },
                        { json_field: null },
                        { json_field: '{"a": 1}' },
                    ],
                };

                const RESULT = {
                    array_field: [{ json_field: {} }, {}, {}, { json_field: { a: 1 } }],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element variants value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.variants } },
                };

                const VALUES = {
                    array_field: ['first', '', 'second', null],
                };

                const RESULT = {
                    array_field: ['first', 'second'],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element variants value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            variants_field: { type: controlType.variants },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { variants_field: 'first' },
                        { variants_field: '' },
                        { variants_field: 'second' },
                        { variants_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [{ variants_field: 'first' }, {}, { variants_field: 'second' }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array with one-element nested array value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.array_types,
                            array_type: { type: controlType.string },
                        },
                    },
                };

                const VALUES = {
                    array_field: [['a', '', 'b', null], ['c', null, 'd'], ['e']],
                };

                const RESULT = {
                    array_field: [['a', 'b'], ['c', 'd'], ['e']],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array with structure-element nested array value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            array_field_nested: {
                                type: controlType.array_types,
                                array_type: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { array_field_nested: ['a', '', 'b', null] },
                        { array_field_nested: ['c', null, 'd'] },
                        { array_field_nested: ['e'] },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { array_field_nested: ['a', 'b'] },
                        { array_field_nested: ['c', 'd'] },
                        { array_field_nested: ['e'] },
                    ],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element structure value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.structure, structure: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { string_field: 'a' },
                        { string_field: '' },
                        { string_field: 'b' },
                        { string_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [{ string_field: 'a' }, {}, { string_field: 'b' }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element structure value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            structure_field: {
                                type: controlType.structure,
                                structure: {
                                    string_field: { type: controlType.string },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { structure_field: { string_field: 'a' } },
                        { structure_field: { string_field: '' } },
                        { structure_field: { string_field: 'b' } },
                        { structure_field: { string_field: null } },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { structure_field: { string_field: 'a' } },
                        { structure_field: {} },
                        { structure_field: { string_field: 'b' } },
                        { structure_field: {} },
                    ],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format array one-element variable value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.variable,
                            control_field: {
                                control_variants_fields: {
                                    type: controlType.variants,
                                    variants: ['simple'],
                                },
                            },
                            variants_fields: {
                                simple: {
                                    string_field: { type: controlType.string },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { control_variants_fields: 'simple' },
                        { control_variants_fields: '' },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { control_variants_fields: 'simple' },
                        {},
                    ],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Format array structure-element variable value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            variable_field: {
                                type: controlType.variable,
                                control_field: {
                                    control_variants_fields: {
                                        type: controlType.variants,
                                        variants: ['simple'],
                                    },
                                },
                                variants_fields: {
                                    simple: {
                                        string_field: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{
                        variable_field: {
                            control_variants_fields: 'simple',
                            string_field: 'string',
                        },
                    },
                    { variable_field: { control_variants_fields: '', string_field: '' } },
                    ],
                };

                const RESULT = {
                    array_field: [{
                        control_variants_fields: 'simple',
                        string_field: 'string',
                    }, {}],
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Format structure values', () => {
            it('Format structure string value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            string_field: { type: controlType.string },
                            string_field_2: { type: controlType.string },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { string_field: 'a', string_field_2: null },
                };

                const RESULT = {
                    structure_field: { string_field: 'a' },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure numeric value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            numeric_field: { type: controlType.numeric },
                            numeric_field_2: { type: controlType.numeric },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { numeric_field: 1, numeric_field_2: null },
                };

                const RESULT = {
                    structure_field: { numeric_field: 1 },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure text value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            text_field: { type: controlType.text },
                            text_field_2: { type: controlType.text },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { text_field: 'some_text', text_field_2: null },
                };

                const RESULT = {
                    structure_field: { text_field: 'some_text' },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure json value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            json_field: { type: controlType.json },
                            json_field_2: { type: controlType.json },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { json_field: '{}', json_field_2: null },
                };

                const RESULT = {
                    structure_field: { json_field: {} },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure bool value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            bool_field: { type: controlType.bool },
                            bool_field_2: { type: controlType.bool },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { bool_field: true, bool_field_2: '' },
                };

                const RESULT = {
                    structure_field: { bool_field: true },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure variants value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            variants_field: { type: controlType.variants },
                            variants_field_2: { type: controlType.variants },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { variants_field: 'variant', variants_field_2: '' },
                };

                const RESULT = {
                    structure_field: { variants_field: 'variant' },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure with nested structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            structure_field_nested: {
                                type: controlType.structure,
                                structure: {
                                    string_field: { type: controlType.string },
                                    string_field_2: { type: controlType.string },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { structure_field_nested: { string_field: 'a', string_field_2: null } },
                };

                const RESULT = {
                    structure_field: { structure_field_nested: { string_field: 'a' } },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure array value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            array_field: {
                                type: controlType.array_types,
                                array_type: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    structure_field: { array_field: ['a', '', 'b', null] },
                };

                const RESULT = {
                    structure_field: { array_field: ['a', 'b'] },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format structure variable value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            variable_field: {
                                type: controlType.variable,
                                control_field: {
                                    control_variants_fields: {
                                        type: controlType.variants,
                                        variants: ['simple'],
                                    },
                                },
                                variants_fields: {
                                    simple: {
                                        string_field: { type: controlType.string },
                                        string_field_2: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    structure_field: {
                        variable_field: {
                            control_variants_fields: 'simple',
                            string_field: 'string_value',
                            string_field_2: '',
                        },
                    },
                };

                const RESULT = {
                    structure_field: {
                        control_variants_fields: 'simple',
                        string_field: 'string_value',
                    },
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Format variable values', () => {

            it('Format variable with empty control field value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple'],
                            },
                        },

                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: null },
                };

                const RESULT = {};

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable with control field value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple'],
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable with empty primitive variants field value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple'],
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: '' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable with primitive variants field value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple'],
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: 'string_value' },
                };

                const RESULT = { control_variants_fields: 'simple', string_field: 'string_value' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable with empty primitive default field value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple'],
                            },
                        },
                        default_fields: {
                            string_field: { type: controlType.string },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: '' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable with primitive default field value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple'],
                            },
                        },
                        default_fields: {
                            string_field: { type: controlType.string },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: 'string_value' },
                };

                const RESULT = { control_variants_fields: 'simple', string_field: 'string_value' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid string value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: '' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid string value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: 'string_value' },
                };

                const RESULT = { control_variants_fields: 'simple', string_field: 'string_value' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid numeric value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                numeric_field: { type: controlType.numeric },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', numeric_field: '' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid numeric value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                numeric_field: { type: controlType.numeric },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', numeric_field: 1 },
                };

                const RESULT = { control_variants_fields: 'simple', numeric_field: 1 };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid text value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                text_field: { type: controlType.text },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', text_field: '' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid numeric value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                text_field: { type: controlType.text },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', text_field: 'some text' },
                };

                const RESULT = { control_variants_fields: 'simple', text_field: 'some text' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid bool value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                bool_field: { type: controlType.bool },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', bool_field: null },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid bool value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                bool_field: { type: controlType.bool },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', bool_field: true },
                };

                const RESULT = { control_variants_fields: 'simple', bool_field: true };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid json value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                json_field: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', json_field: null },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid json value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                json_field: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', json_field: '{}' },
                };

                const RESULT = { control_variants_fields: 'simple', json_field: {} };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid variants value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variants_field: { type: controlType.variants },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', variants_field: '' },
                };

                const RESULT = { control_variants_fields: 'simple' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid variants value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variants_field: { type: controlType.variants },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', variants_field: 'variant' },
                };

                const RESULT = { control_variants_fields: 'simple', variants_field: 'variant' };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable array value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                array_field: {
                                    type: controlType.array_types,
                                    array_type: { type: controlType.string },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', array_field: ['a', '', 'b', null] },
                };

                const RESULT = { control_variants_fields: 'simple', array_field: ['a', 'b'] };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable invalid structure value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                structure_field: {
                                    type: controlType.structure,
                                    structure: {
                                        string_field: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', structure_field: { string_field: '' } },
                };

                const RESULT = { control_variants_fields: 'simple', structure_field: {} };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable valid structure value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                structure_field: {
                                    type: controlType.structure,
                                    structure: {
                                        string_field: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: {
                        control_variants_fields: 'simple',
                        structure_field: { string_field: 'some string' },
                    },
                };

                const RESULT = { control_variants_fields: 'simple', structure_field: { string_field: 'some string' } };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Format variable with nested variable value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variable_nested_field: {
                                    type: controlType.variable,
                                    control_field: {
                                        control_variants_nested_fields: {
                                            type: controlType.variants,
                                        },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: {
                        control_variants_fields: 'simple',
                        variable_nested_field: { control_variants_nested_fields: 'simple nested' },
                    },
                };

                const RESULT = {
                    control_variants_fields: 'simple',
                    control_variants_nested_fields: 'simple nested',
                };

                expect(FormConstructorValuesWorker.formatValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });
    });

    describe('Validate values', () => {
        describe('Validate string value', () => {
            it('Validate not required string value', () => {
                const SCHEMA = { string_field: { type: controlType.string } };

                const VALUES = { string_field: '' };

                const RESULT = { string_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required string value', () => {
                const SCHEMA = { string_field: { type: controlType.string, required: true } };

                const VALUES = { string_field: null };

                const RESULT = { string_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Validate text value', () => {
            it('Validate not required text value', () => {
                const SCHEMA = { text_field: { type: controlType.text } };

                const VALUES = { text_field: '' };

                const RESULT = { text_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required text value', () => {
                const SCHEMA = { text_field: { type: controlType.text, required: true } };

                const VALUES = { text_field: null };

                const RESULT = { text_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Validate json value', () => {
            it('Validate not required null json value', () => {
                const SCHEMA = { json_field: { type: controlType.json } };

                const VALUES = { json_field: null };

                const RESULT = { json_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required null json value', () => {
                const SCHEMA = { json_field: { type: controlType.json, required: true } };

                const VALUES = { json_field: null };

                const RESULT = { json_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty-string json value', () => {
                const SCHEMA = { json_field: { type: controlType.json } };

                const VALUES = { json_field: '' };

                const RESULT = { json_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required empty-string json value', () => {
                const SCHEMA = { json_field: { type: controlType.json, required: true } };

                const VALUES = { json_field: '' };

                const RESULT = { json_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required invalid json value', () => {
                const SCHEMA = { json_field: { type: controlType.json } };

                const VALUES = { json_field: '{(' };

                const RESULT = { json_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required invalid json value', () => {
                const SCHEMA = { json_field: { type: controlType.json, required: true } };

                const VALUES = { json_field: '{(' };

                const RESULT = { json_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required valid json value', () => {
                const SCHEMA = { json_field: { type: controlType.json } };

                const VALUES = { json_field: '{}' };

                const RESULT = { json_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required valid json value', () => {
                const SCHEMA = { json_field: { type: controlType.json, required: true } };

                const VALUES = { json_field: '{}' };

                const RESULT = { json_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            // it('Validate required json value', () => {
            //     const SCHEMA = {json_field: {type: controlType.json, required: true}};
            //
            //     const VALUES = {json_field: null};
            //
            //     const RESULT = {json_field: false};
            //
            //     expect(FormConstructorValuesWorker.validateValues({
            //         valuesFull: VALUES,
            //         schemaFull: SCHEMA,
            //         parents: [],
            //     })).toEqual(RESULT);
            // });
        });

        describe('Validate bool value', () => {
            it('Validate not required null bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool } };

                const VALUES = { bool_field: null };

                const RESULT = { bool_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required null bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool, required: true } };

                const VALUES = { bool_field: null };

                const RESULT = { bool_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty-string bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool } };

                const VALUES = { bool_field: '' };

                const RESULT = { bool_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required empty-string bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool, required: true } };

                const VALUES = { bool_field: '' };

                const RESULT = { bool_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required true bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool } };

                const VALUES = { bool_field: true };

                const RESULT = { bool_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required true bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool, required: true } };

                const VALUES = { bool_field: true };

                const RESULT = { bool_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required false bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool } };

                const VALUES = { bool_field: false };

                const RESULT = { bool_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required false bool value', () => {
                const SCHEMA = { bool_field: { type: controlType.bool, required: true } };

                const VALUES = { bool_field: false };

                const RESULT = { bool_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Validate numeric value', () => {
            it('Validate not required null numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: null };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required null numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: null };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: '' };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required empty-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: '' };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required invalid-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: 'a' };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required invalid-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: 'a' };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required zero-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: '0' };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required zero-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: '0' };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required one-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: '1' };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required one-string numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: '1' };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required zero numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: 0 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required zero numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: 0 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required one numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required one numeric value', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required numeric value less than min', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, min: 5 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required numeric value less than min', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true, min: 5 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required numeric value equal min', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, min: 1 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required numeric value equal min', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true, min: 1 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required numeric value between min and max', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, min: 0, max: 3 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required numeric value between min and max', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true, min: 0, max: 3 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required numeric value equal max', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, max: 1 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required numeric value equal max', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true, max: 1 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required numeric value more than max', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, max: 0 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate required numeric value more than max', () => {
                const SCHEMA = { numeric_field: { type: controlType.numeric, required: true, max: 0 } };

                const VALUES = { numeric_field: 1 };

                const RESULT = { numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Validate structure value', () => {
            it('Validate not required null structure value', () => {
                const SCHEMA = { structure_field: { type: controlType.structure, structure: {} } };

                const VALUES = { structure_field: null };

                const RESULT = { structure_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required null structure value', () => {
                const SCHEMA = { structure_field: { type: controlType.structure, structure: {}, required: true } };

                const VALUES = { structure_field: null };

                const RESULT = { structure_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate not required empty structure value', () => {
                const SCHEMA = { structure_field: { type: controlType.structure, structure: {} } };

                const VALUES = { structure_field: {} };

                const RESULT = { structure_field: {} };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty structure value', () => {
                const SCHEMA = { structure_field: { type: controlType.structure, structure: {}, required: true } };

                const VALUES = { structure_field: {} };

                const RESULT = { structure_field: {} };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty string structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            string_field: { type: controlType.string },
                        },
                    },
                };

                const VALUES = { structure_field: { string_field: '' } };

                const RESULT = { structure_field: { string_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty string structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            string_field: { type: controlType.string, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { string_field: '' } };

                const RESULT = { structure_field: { string_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty numeric structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            numeric_field: { type: controlType.numeric },
                        },
                    },
                };

                const VALUES = { structure_field: { numeric_field: '' } };

                const RESULT = { structure_field: { numeric_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty numeric structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            numeric_field: { type: controlType.numeric, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { numeric_field: '' } };

                const RESULT = { structure_field: { numeric_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty text structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            text_field: { type: controlType.text },
                        },
                    },
                };

                const VALUES = { structure_field: { text_field: '' } };

                const RESULT = { structure_field: { text_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty text structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            text_field: { type: controlType.text, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { text_field: '' } };

                const RESULT = { structure_field: { text_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty text structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            text_field: { type: controlType.text },
                        },
                    },
                };

                const VALUES = { structure_field: { text_field: '' } };

                const RESULT = { structure_field: { text_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty text structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            text_field: { type: controlType.text, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { text_field: '' } };

                const RESULT = { structure_field: { text_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty json structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            json_field: { type: controlType.json },
                        },
                    },
                };

                const VALUES = { structure_field: { json_field: null } };

                const RESULT = { structure_field: { json_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty json structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            json_field: { type: controlType.json, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { json_field: null } };

                const RESULT = { structure_field: { json_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty bool structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            bool_field: { type: controlType.bool },
                        },
                    },
                };

                const VALUES = { structure_field: { bool_field: null } };

                const RESULT = { structure_field: { bool_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty bool structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            bool_field: { type: controlType.bool, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { bool_field: null } };

                const RESULT = { structure_field: { bool_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate not required bool structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            bool_field: { type: controlType.bool },
                        },
                    },
                };

                const VALUES = { structure_field: { bool_field: false } };

                const RESULT = { structure_field: { bool_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required bool structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            bool_field: { type: controlType.bool, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { bool_field: false } };

                const RESULT = { structure_field: { bool_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required empty variants structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            variants_field: { type: controlType.variants },
                        },
                    },
                };

                const VALUES = { structure_field: { variants_field: null } };

                const RESULT = { structure_field: { variants_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty variants structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            variants_field: { type: controlType.variants, required: true },
                        },
                    },
                };

                const VALUES = { structure_field: { variants_field: null } };

                const RESULT = { structure_field: { variants_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required nested structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            structure_nested_field: {
                                type: controlType.structure,
                                structure: { string_field: { type: controlType.string } },
                            },
                        },
                    },
                };

                const VALUES = { structure_field: { structure_nested_field: { string_field: null } } };

                const RESULT = { structure_field: { structure_nested_field: { string_field: true } } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required nested structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            structure_nested_field: {
                                type: controlType.structure,
                                structure: { string_field: { type: controlType.string, required: true } },
                            },
                        },
                    },
                };

                const VALUES = { structure_field: { structure_nested_field: { string_field: null } } };

                const RESULT = { structure_field: { structure_nested_field: { string_field: false } } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required array structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            array_field: {
                                type: controlType.array_types,
                                array_type: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = { structure_field: { array_field: ['a', ''] } };

                const RESULT = { structure_field: { array_field: [true, true] } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required array structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            array_field: {
                                type: controlType.array_types,
                                array_type: { type: controlType.string, required: true },
                            },
                        },
                    },
                };

                const VALUES = { structure_field: { array_field: ['a', ''] } };

                const RESULT = { structure_field: { array_field: [true, false] } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate not required variable structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            variable_field: {
                                type: controlType.variable,
                                control_field: {
                                    control_variants_fields: {
                                        type: controlType.variants,
                                    },
                                },
                                variants_fields: {
                                    simple: {
                                        string_field: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    structure_field: {
                        variable_field: {
                            control_variants_fields: 'simple',
                            string_field: '',
                        },
                    },
                };

                const RESULT = { structure_field: { control_variants_fields: true, string_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required variable structure value', () => {
                const SCHEMA = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            variable_field: {
                                type: controlType.variable,
                                control_field: {
                                    control_variants_fields: {
                                        type: controlType.variants, required: true,
                                    },
                                },
                                variants_fields: {
                                    simple: {
                                        string_field: { type: controlType.string, required: true },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    structure_field: {
                        variable_field: {
                            control_variants_fields: 'simple',
                            string_field: '',
                        },
                    },
                };

                const RESULT = { structure_field: { control_variants_fields: true, string_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Validate array value', () => {
            it('Validate not required null array value', () => {
                const SCHEMA = { array_field: { type: controlType.array_types, array_type: {} } };

                const VALUES = { array_field: null };

                const RESULT = { array_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required null array value', () => {
                const SCHEMA = { array_field: { type: controlType.array_types, array_type: {}, required: true } };

                const VALUES = { array_field: null };

                const RESULT = { array_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate not required empty array value', () => {
                const SCHEMA = { array_field: { type: controlType.array_types, array_type: {} } };

                const VALUES = { array_field: [] };

                const RESULT = { array_field: [] };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate required empty array value', () => {
                const SCHEMA = { array_field: { type: controlType.array_types, array_type: {}, required: true } };

                const VALUES = { array_field: [] };

                const RESULT = { array_field: [] };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element string value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.string } },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: [true, true, true, true],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element string value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.string, required: true },
                    },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: [true, false, true, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element string value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            string_field: { type: controlType.string },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { string_field: 'a' },
                        { string_field: '' },
                        { string_field: 'b' },
                        { string_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { string_field: true },
                        { string_field: true },
                        { string_field: true },
                        { string_field: true },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element string value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            string_field: { type: controlType.string, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { string_field: 'a' },
                        { string_field: '' },
                        { string_field: 'b' },
                        { string_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { string_field: true },
                        { string_field: false },
                        { string_field: true },
                        { string_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element text value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.text } },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: [true, true, true, true],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element text value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.text, required: true },
                    },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: [true, false, true, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element text value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            text_field: { type: controlType.text },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ text_field: 'a' }, { text_field: '' }, { text_field: 'b' }, { text_field: null }],
                };

                const RESULT = {
                    array_field: [
                        { text_field: true },
                        { text_field: true },
                        { text_field: true },
                        { text_field: true },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element text value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            text_field: { type: controlType.text, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ text_field: 'a' }, { text_field: '' }, { text_field: 'b' }, { text_field: null }],
                };

                const RESULT = {
                    array_field: [
                        { text_field: true },
                        { text_field: false },
                        { text_field: true },
                        { text_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element json value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.json } },
                };

                const VALUES = {
                    array_field: ['{}', '{(', null],
                };

                const RESULT = {
                    array_field: [true, false, true],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element json value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.json, required: true },
                    },
                };

                const VALUES = {
                    array_field: ['{}', '{(', null],
                };

                const RESULT = {
                    array_field: [true, false, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element json value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            json_field: { type: controlType.json },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ json_field: '{(' }, { json_field: '{}' }, { json_field: null }],
                };

                const RESULT = {
                    array_field: [{ json_field: false }, { json_field: true }, { json_field: true }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element json value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            json_field: { type: controlType.json, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ json_field: '{(' }, { json_field: '{}' }, { json_field: null }],
                };

                const RESULT = {
                    array_field: [{ json_field: false }, { json_field: true }, { json_field: false }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element bool value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.bool } },
                };

                const VALUES = {
                    array_field: [true, false, '', null],
                };

                const RESULT = {
                    array_field: [true, true, false, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element bool value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.bool, required: true },
                    },
                };

                const VALUES = {
                    array_field: [true, false, '', null],
                };

                const RESULT = {
                    array_field: [true, true, false, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element bool value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            bool_field: { type: controlType.bool },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { bool_field: true },
                        { bool_field: false },
                        { bool_field: '' },
                        { bool_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { bool_field: true },
                        { bool_field: true },
                        { bool_field: false },
                        { bool_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element bool value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            bool_field: { type: controlType.bool, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { bool_field: true },
                        { bool_field: false },
                        { bool_field: '' },
                        { bool_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { bool_field: true },
                        { bool_field: true },
                        { bool_field: false },
                        { bool_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element numeric value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.numeric } },
                };

                const VALUES = {
                    array_field: ['', null, '1', 0, 'a'],
                };

                const RESULT = {
                    array_field: [true, true, true, true, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element numeric value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.numeric, required: true },
                    },
                };

                const VALUES = {
                    array_field: ['', null, '1', 0, 'a'],
                };

                const RESULT = {
                    array_field: [false, false, true, true, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element numeric value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            numeric_field: { type: controlType.numeric },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { numeric_field: '' },
                        { numeric_field: null },
                        { numeric_field: '1' },
                        { numeric_field: 0 },
                        { numeric_field: 'a' },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { numeric_field: true },
                        { numeric_field: true },
                        { numeric_field: true },
                        { numeric_field: true },
                        { numeric_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element numeric value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            numeric_field: { type: controlType.numeric, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { numeric_field: '' },
                        { numeric_field: null },
                        { numeric_field: '1' },
                        { numeric_field: 0 },
                        { numeric_field: 'a' },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { numeric_field: false },
                        { numeric_field: false },
                        { numeric_field: true },
                        { numeric_field: true },
                        { numeric_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element variants value', () => {
                const SCHEMA = {
                    array_field: { type: controlType.array_types, array_type: { type: controlType.variants } },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: [true, true, true, true],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element variants value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.variants, required: true },
                    },
                };

                const VALUES = {
                    array_field: ['a', '', 'c', null],
                };

                const RESULT = {
                    array_field: [true, false, true, false],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element variants value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            variants_field: { type: controlType.variants },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { variants_field: 'a' },
                        { variants_field: '' },
                        { variants_field: 'b' },
                        { variants_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { variants_field: true },
                        { variants_field: true },
                        { variants_field: true },
                        { variants_field: true },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element variants value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            variants_field: { type: controlType.variants, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { variants_field: 'a' },
                        { variants_field: '' },
                        { variants_field: 'b' },
                        { variants_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { variants_field: true },
                        { variants_field: false },
                        { variants_field: true },
                        { variants_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element nested array value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: { type: controlType.array_types, array_type: { type: controlType.string } },
                    },
                };

                const VALUES = {
                    array_field: [['a', ''], ['c', null]],
                };

                const RESULT = {
                    array_field: [[true, true], [true, true]],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element nested array value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.array_types,
                            array_type: { type: controlType.string, required: true },
                        },
                    },
                };

                const VALUES = {
                    array_field: [['a', ''], ['c', null]],
                };

                const RESULT = {
                    array_field: [[true, false], [true, false]],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element nested array value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            array_nested_field: {
                                type: controlType.array_types,
                                array_type: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ array_nested_field: ['a', ''] }, { array_nested_field: ['b', null] }],
                };

                const RESULT = {
                    array_field: [{ array_nested_field: [true, true] }, { array_nested_field: [true, true] }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element nested array value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            array_nested_field: {
                                type: controlType.array_types,
                                array_type: { type: controlType.string, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ array_nested_field: ['a', ''] }, { array_nested_field: ['b', null] }],
                };

                const RESULT = {
                    array_field: [{ array_nested_field: [true, false] }, { array_nested_field: [true, false] }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element structure value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.structure,
                            structure: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { string_field: 'a' },
                        { string_field: '' },
                        { string_field: 'c' },
                        { string_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { string_field: true },
                        { string_field: true },
                        { string_field: true },
                        { string_field: true },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element structure value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.structure,
                            structure: { string_field: { type: controlType.string, required: true } },
                        },
                    },
                };

                const VALUES = {
                    array_field: [
                        { string_field: 'a' },
                        { string_field: '' },
                        { string_field: 'c' },
                        { string_field: null },
                    ],
                };

                const RESULT = {
                    array_field: [
                        { string_field: true },
                        { string_field: false },
                        { string_field: true },
                        { string_field: false },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element structure value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            structure_field: {
                                type: controlType.structure,
                                structure: { string_field: { type: controlType.string } },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ structure_field: { string_field: 'a' } }, { structure_field: { string_field: '' } },
                        { structure_field: { string_field: 'b' } }, { structure_field: { string_field: null } }],
                };

                const RESULT = {
                    array_field: [
                        { structure_field: { string_field: true } },
                        { structure_field: { string_field: true } },
                        { structure_field: { string_field: true } },
                        { structure_field: { string_field: true } },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element structure value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types, array_type: {
                            structure_field: {
                                type: controlType.structure,
                                structure: { string_field: { type: controlType.string, required: true } },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ structure_field: { string_field: 'a' } }, { structure_field: { string_field: '' } },
                        { structure_field: { string_field: 'b' } }, { structure_field: { string_field: null } }],
                };

                const RESULT = {
                    array_field: [
                        { structure_field: { string_field: true } },
                        { structure_field: { string_field: false } },
                        { structure_field: { string_field: true } },
                        { structure_field: { string_field: false } },
                    ],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate array not required one-element variable value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.variable,
                            control_field: {
                                control_variants_fields: {
                                    type: controlType.variants,
                                },
                            },
                            variants_fields: {
                                simple: {
                                    string_field: { type: controlType.string },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{
                        control_variants_fields: 'simple',
                        string_field: 'string',
                    }, { control_variants_fields: 'simple', string_field: '' }],
                };

                const RESULT = {
                    array_field: [{ control_variants_fields: true, string_field: true }, {
                        control_variants_fields: true,
                        string_field: true,
                    }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required one-element variable value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.variable,
                            control_field: {
                                control_variants_fields: {
                                    type: controlType.variants,
                                },
                            },
                            variants_fields: {
                                simple: {
                                    string_field: { type: controlType.string, required: true },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{
                        control_variants_fields: 'simple',
                        string_field: 'string',
                    }, { control_variants_fields: 'simple', string_field: '' }],
                };

                const RESULT = {
                    array_field: [{ control_variants_fields: true, string_field: true }, {
                        control_variants_fields: true,
                        string_field: false,
                    }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array not required structure-element variable value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            variable_field: {
                                type: controlType.variable,
                                control_field: {
                                    control_variants_fields: {
                                        type: controlType.variants,
                                    },
                                },
                                variants_fields: {
                                    simple: {
                                        string_field: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ variable_field: {
                        control_variants_fields: 'simple',
                        string_field: 'string',
                    } }, { variable_field: { control_variants_fields: 'simple', string_field: '' } }],
                };

                const RESULT = {
                    array_field: [{ control_variants_fields: true, string_field: true }, {
                        control_variants_fields: true,
                        string_field: true,
                    }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate array required structure-element variable value', () => {
                const SCHEMA = {
                    array_field: {
                        type: controlType.array_types,
                        array_type: {
                            variable_field: {
                                type: controlType.variable,
                                control_field: {
                                    control_variants_fields: {
                                        type: controlType.variants,
                                    },
                                },
                                variants_fields: {
                                    simple: {
                                        string_field: { type: controlType.string, required: true },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    array_field: [{ variable_field: {
                        control_variants_fields: 'simple',
                        string_field: 'string',
                    } }, { variable_field: { control_variants_fields: 'simple', string_field: '' } }],
                };

                const RESULT = {
                    array_field: [{ control_variants_fields: true, string_field: true }, {
                        control_variants_fields: true,
                        string_field: false,
                    }],
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });

        describe('Validate variable value', () => {
            it('Validate empty variable value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        required: true,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: null,
                };

                const RESULT = { variable_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required string value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: '' },
                };

                const RESULT = { control_variants_fields: true, string_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required string value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', string_field: '' },
                };

                const RESULT = { control_variants_fields: true, string_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required text value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                text_field: { type: controlType.text },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', text_field: '' },
                };

                const RESULT = { control_variants_fields: true, text_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required text value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                text_field: { type: controlType.text, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', text_field: '' },
                };

                const RESULT = { control_variants_fields: true, text_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required json value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                json_field: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', json_field: '' },
                };

                const RESULT = { control_variants_fields: true, json_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required json value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                json_field: { type: controlType.json, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', json_field: '' },
                };

                const RESULT = { control_variants_fields: true, json_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required bool value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                bool_field: { type: controlType.bool },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', bool_field: '' },
                };

                const RESULT = { control_variants_fields: true, bool_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required bool value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                bool_field: { type: controlType.bool, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', bool_field: '' },
                };

                const RESULT = { control_variants_fields: true, bool_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required numeric value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                numeric_field: { type: controlType.numeric },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', numeric_field: '' },
                };

                const RESULT = { control_variants_fields: true, numeric_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required numeric value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                numeric_field: { type: controlType.numeric, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', numeric_field: '' },
                };

                const RESULT = { control_variants_fields: true, numeric_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required variants value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variants_field: { type: controlType.variants },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', variants_field: '' },
                };

                const RESULT = { control_variants_fields: true, variants_field: true };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required variants value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variants_field: { type: controlType.variants, required: true },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', variants_field: '' },
                };

                const RESULT = { control_variants_fields: true, variants_field: false };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required array value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                array_field: {
                                    type: controlType.array_types,
                                    array_type: { type: controlType.string },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', array_field: ['a', ''] },
                };

                const RESULT = { control_variants_fields: true, array_field: [true, true] };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required array value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                array_field: {
                                    type: controlType.array_types,
                                    array_type: { type: controlType.string, required: true },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', array_field: ['a', ''] },
                };

                const RESULT = { control_variants_fields: true, array_field: [true, false] };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required structure value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                structure_field: {
                                    type: controlType.structure, structure: {
                                        string_field: { type: controlType.string },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', structure_field: { string_field: '' } },
                };

                const RESULT = { control_variants_fields: true, structure_field: { string_field: true } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required structure value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                structure_field: {
                                    type: controlType.structure, structure: {
                                        string_field: { type: controlType.string, required: true },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: { control_variants_fields: 'simple', structure_field: { string_field: '' } },
                };

                const RESULT = { control_variants_fields: true, structure_field: { string_field: false } };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });

            it('Validate variable invalid not required variable value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variable_nested_field: {
                                    type: controlType.variable,
                                    control_field: {
                                        control_nested_variants_fields: {
                                            type: controlType.variants,
                                        },
                                    },
                                    variants_fields: {
                                        simple: {
                                            string_field: { type: controlType.string },
                                        },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: {
                        control_variants_fields: 'simple',
                        variable_nested_field: {
                            control_nested_variants_fields: 'simple',
                            string_field: '',
                        },
                    },
                };

                const RESULT = {
                    control_variants_fields: true,
                    control_nested_variants_fields: true,
                    string_field: true,
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
            it('Validate variable invalid required variable value', () => {
                const SCHEMA = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                            },
                        },
                        variants_fields: {
                            simple: {
                                variable_nested_field: {
                                    type: controlType.variable,
                                    control_field: {
                                        control_nested_variants_fields: {
                                            type: controlType.variants,
                                        },
                                    },
                                    variants_fields: {
                                        simple: {
                                            string_field: { type: controlType.string, required: true },
                                        },
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = {
                    variable_field: {
                        control_variants_fields: 'simple',
                        variable_nested_field: {
                            control_nested_variants_fields: 'simple',
                            string_field: '',
                        },
                    },
                };

                const RESULT = {
                    control_variants_fields: true,
                    control_nested_variants_fields: true,
                    string_field: false,
                };

                expect(FormConstructorValuesWorker.validateValues({
                    valuesFull: VALUES,
                    schemaFull: SCHEMA,
                    parents: [],
                })).toEqual(RESULT);
            });
        });
    });

    // describe('Validate full values object (validateValues())', () => {
    //
    //     it('Validate schema right', () => {
    //         const SCHEMA: Dict<ISchemaItem> = {
    //             test_1: {
    //                 type: controlType.string,
    //                 default: 'test_1'
    //             },
    //             test_2: {
    //                 type: controlType.array_types,
    //                 array_type: {
    //                     type: controlType.text,
    //                     required: true
    //                 },
    //                 default: [null, 'test_2']
    //             },
    //             test_3: {
    //                 type: controlType.structure,
    //                 structure: {
    //                     a: {
    //                         default: 'a',
    //                         type: controlType.string,
    //                     },
    //                     b: {
    //                         type: controlType.string,
    //                     }
    //                 }
    //             }
    //         };
    //         const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
    //
    //         expect(FormConstructorValuesWorker.validateValues(wrapper.instance().state.values, [], SCHEMA))
    //             .toEqual({test_1: true, test_2: [false, true], test_3: {a: true, b: null}});
    //     });
    // });
    //
    // describe('Validate Array values (checkValidatedValue())', () => {
    //     it('Validate empty array as empty array', () => {
    //         const SCHEMA: Dict<ISchemaItem> = {
    //             test: {
    //                 type: controlType.array_types,
    //                 array_type: {
    //                     type: controlType.string,
    //                 }
    //             }
    //         };
    //         expect(FormConstructorValuesWorker.checkValidatedValue([], 'test', [], SCHEMA)).toEqual([]);
    //     });
    //
    //     it('Validate NOT empty array as false-array', () => {
    //         const SCHEMA: Dict<ISchemaItem> = {
    //             test: {
    //                 type: controlType.array_types,
    //                 array_type: {
    //                     type: controlType.string,
    //                     required: true
    //                 }
    //             }
    //         };
    //         expect(FormConstructorValuesWorker.checkValidatedValue([null, 'test'], 'test', [], SCHEMA)).toEqual([false, true]);
    //     });
    // });
    //
    // describe('Validate Structure values (checkValidatedValue())', () => {
    //     const SCHEMA: Dict<ISchemaItem> = {
    //         test: {
    //             type: controlType.array_types,
    //             array_type: {
    //                 a: {
    //                     type: controlType.string,
    //                 },
    //                 b: {
    //                     type: controlType.string,
    //                 }
    //             }
    //         }
    //     };
    //
    //     it('Validate empty structure as null-structure', () => {
    //         expect(FormConstructorValuesWorker.checkValidatedValue({a: null, b: null}, 'test', [], SCHEMA))
    //             .toEqual({a: null, b: null});
    //     });
    //
    //     it('Validate NOT empty structure as structure', () => {
    //         expect(FormConstructorValuesWorker.checkValidatedValue({a: 'a', b: 'b'}, 'test', [], SCHEMA))
    //             .toEqual({a: true, b: true});
    //     });
    // });
    //
    // describe('Validate values', () => {
    //     describe('Validate primitive values (validateValue())', () => {
    //         it('Validate default required empty value as FALSE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 name: {type: controlType.string, required: true}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue(null, 'name', [], SCHEMA)).toEqual(false);
    //         });
    //         it('Validate default required NOT empty value as TRUE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 name: {type: controlType.string, default: 'name', required: true}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue('name', 'name', [], SCHEMA)).toEqual(true);
    //         });
    //         it('Validate default NOT required empty value as NULL', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 name: {type: controlType.string}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue(null, 'name', [], SCHEMA)).toEqual(null);
    //         });
    //         it('Validate default NOT required NOT empty value as TRUE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 name: {type: controlType.string, default: 'name'}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue('name', 'name', [], SCHEMA)).toEqual(true);
    //         });
    //         it('Validate default NOT required NOT empty boolean as TRUE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 name: {type: controlType.bool, default: true}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue(true, 'name', [], SCHEMA)).toEqual(true);
    //         });
    //         it('Validate default NOT required NOT empty boolean as FALSE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 name: {type: controlType.bool, default: false}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue(false, 'name', [], SCHEMA)).toEqual(true);
    //         });
    //         it('Validate default NOT required NOT empty numeric as TRUE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 test: {type: controlType.numeric, default: 10, min: 0}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue(10, 'test', [], SCHEMA)).toEqual(true);
    //         });
    //         it('Validate default NOT required NOT empty numeric as FALSE', () => {
    //             const SCHEMA: Dict<ISchemaItem> = {
    //                 test: {type: controlType.numeric, default: 0, min: 10}
    //             };
    //             expect(FormConstructorValuesWorker.validateValue(0, 'test', [], SCHEMA)).toEqual(false);
    //         });
    //     });
    // });
});
