/*eslint-disable no-magic-numbers*/
import { Dict } from '../../../../types';
import { JSON_TAB_SIZE } from '../constants';
import { controlType, ISchemaItem, SchemaItemVisual } from '../types';
import { FormConstructorSchemaWorker } from './index';

describe('FormConstructorSchemaWorker', () => {
    describe('Construct Values By Schema', () => {

        it('Construct with default type with NO default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                name: { type: controlType.string },
            };

            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ name: null });
        });
        it('Construct with default type with default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                name: { type: controlType.string, default: 'name' },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ name: 'name' });
        });

        it('Construct with JSON type with NO default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: { type: controlType.json },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ test: null });
        });
        it('Construct with JSON type with default Object value', () => {
            const DEFAULT = { a: 1, b: 2 };
            const SCHEMA: Dict<ISchemaItem> = {
                test: { type: controlType.json, default: DEFAULT },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                .toEqual({ test: JSON.stringify(DEFAULT, null, JSON_TAB_SIZE) });
        });
        it('Construct with JSON type with default Array value', () => {
            const DEFAULT = [1, 2, 3];
            const SCHEMA: Dict<ISchemaItem> = {
                test: { type: controlType.json, default: DEFAULT },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                .toEqual({ test: JSON.stringify(DEFAULT, null, JSON_TAB_SIZE) });
        });
        it('Construct with JSON type with default wrong value', () => {
            const DEFAULT = 'notObjectValue';
            const SCHEMA: Dict<ISchemaItem> = {
                test: { type: controlType.json, default: DEFAULT },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                .toEqual({ test: JSON.stringify(DEFAULT, null, JSON_TAB_SIZE) });
        });

        it('Construct with boolean type with NO default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: { type: controlType.bool },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ test: false });
        });
        it('Construct with boolean type with default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: { type: controlType.bool, default: true },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ test: true });
        });

        describe('Construct Numeric Values By Schema', () => {
            it('Construct with numeric with no default value', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    numeric: { type: controlType.numeric },
                };
                expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ numeric: null });
            });
            it('Construct with numeric with wrong default value', () => {
                const DEFAULT_WRONG_NUMERIC = 'abc';
                const SCHEMA: Dict<ISchemaItem> = {
                    numeric: { type: controlType.numeric, default: DEFAULT_WRONG_NUMERIC },
                };
                expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ numeric: null });
            });
            it('Construct with numeric with zero default value', () => {
                const DEFAULT_ZERO_NUMERIC = 0;
                const SCHEMA: Dict<ISchemaItem> = {
                    numeric: { type: controlType.numeric, default: DEFAULT_ZERO_NUMERIC },
                };
                expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                    .toEqual({ numeric: DEFAULT_ZERO_NUMERIC });
            });
            it('Construct with numeric with default value', () => {
                const DEFAULT_NUMERIC = 123;
                const SCHEMA: Dict<ISchemaItem> = {
                    numeric: { type: controlType.numeric, default: DEFAULT_NUMERIC },
                };
                expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                    .toEqual({ numeric: DEFAULT_NUMERIC });
            });

            describe('Construct Numeric Timestamp Values By Schema', () => {
                it('Construct with numeric timestamp type with NO default value', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        date: { type: controlType.numeric, visual: SchemaItemVisual.TIMESTAMP },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ date: null });
                });
                it('Construct with numeric timestamp type with valid default value', () => {
                    const DEFAULT_VALID_TIMESTAMP = 1583843078;
                    const SCHEMA: Dict<ISchemaItem> = {
                        date: {
                            type: controlType.numeric,
                            visual: SchemaItemVisual.TIMESTAMP,
                            default: DEFAULT_VALID_TIMESTAMP,
                        },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                        .toEqual({ date: DEFAULT_VALID_TIMESTAMP });
                });
                it('Construct with numeric timestamp type with wrong default value', () => {
                    const DEFAULT_WRONG_TIMESTAMP = 'abc';
                    const SCHEMA: Dict<ISchemaItem> = {
                        date: {
                            type: controlType.numeric,
                            visual: SchemaItemVisual.TIMESTAMP,
                            default: DEFAULT_WRONG_TIMESTAMP,
                        },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ date: null });
                });
            });

            describe('Construct Numeric Rubs Values By Schema', () => {
                it('Construct with numeric rubs with no default value', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        rubs: { type: controlType.numeric, visual: SchemaItemVisual.RUBS },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ rubs: null });
                });

                it('Construct with numeric rubs with wrong default value', () => {
                    const DEFAULT_WRONG_RUBS = 'abc';
                    const SCHEMA: Dict<ISchemaItem> = {
                        rubs: { type: controlType.numeric, visual: SchemaItemVisual.RUBS, default: DEFAULT_WRONG_RUBS },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ rubs: null });
                });

                it('Construct with numeric rubs with zero default value', () => {
                    const DEFAULT_ZERO_RUBS = 0;
                    const SCHEMA: Dict<ISchemaItem> = {
                        rubs: { type: controlType.numeric, visual: SchemaItemVisual.RUBS, default: DEFAULT_ZERO_RUBS },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                        .toEqual({ rubs: DEFAULT_ZERO_RUBS });
                });

                it('Construct with numeric rubs with zero default value', () => {
                    const DEFAULT_FULL_RUBS = 100;
                    const SCHEMA: Dict<ISchemaItem> = {
                        rubs: { type: controlType.numeric, visual: SchemaItemVisual.RUBS, default: DEFAULT_FULL_RUBS },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ rubs: 1.00 });
                });

                it('Construct with numeric rubs with default integer value', () => {
                    const DEFAULT_RUBS = 12345;
                    const SCHEMA: Dict<ISchemaItem> = {
                        rubs: { type: controlType.numeric, visual: SchemaItemVisual.RUBS, default: DEFAULT_RUBS },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ rubs: 123.45 });
                });

                it('Construct with numeric rubs with default float value', () => {
                    const DEFAULT_RUBS = 123.45;
                    const SCHEMA: Dict<ISchemaItem> = {
                        rubs: { type: controlType.numeric, visual: SchemaItemVisual.RUBS, default: DEFAULT_RUBS },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ rubs: 1.23 });
                });

            });

            describe('Construct Numeric Money Values By Schema', () => {
                it('Construct with numeric money with no default value', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        Money: { type: controlType.numeric, visual: SchemaItemVisual.MONEY },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ Money: null });
                });

                it('Construct with numeric money with wrong default value', () => {
                    const DEFAULT_WRONG_MONEY = 'abc';
                    const SCHEMA: Dict<ISchemaItem> = {
                        money: {
                            type: controlType.numeric,
                            visual: SchemaItemVisual.MONEY,
                            default: DEFAULT_WRONG_MONEY,
                        },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ money: null });
                });

                it('Construct with numeric money with zero default value', () => {
                    const DEFAULT_ZERO_MONEY = 0;
                    const SCHEMA: Dict<ISchemaItem> = {
                        money: {
                            type: controlType.numeric,
                            visual: SchemaItemVisual.MONEY,
                            default: DEFAULT_ZERO_MONEY,
                        },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                        .toEqual({ money: DEFAULT_ZERO_MONEY });
                });

                it('Construct with numeric money with zero default value', () => {
                    const DEFAULT_FULL_MONEY = 100;
                    const SCHEMA: Dict<ISchemaItem> = {
                        money: {
                            type: controlType.numeric,
                            visual: SchemaItemVisual.MONEY,
                            default: DEFAULT_FULL_MONEY,
                        },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ money: 1.00 });
                });

                it('Construct with numeric money with default integer value', () => {
                    const DEFAULT_MONEY = 12345;
                    const SCHEMA: Dict<ISchemaItem> = {
                        money: { type: controlType.numeric, visual: SchemaItemVisual.MONEY, default: DEFAULT_MONEY },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ money: 123.45 });
                });

                it('Construct with numeric money with default float value', () => {
                    const DEFAULT_MONEY = 123.45;
                    const SCHEMA: Dict<ISchemaItem> = {
                        money: { type: controlType.numeric, visual: SchemaItemVisual.MONEY, default: DEFAULT_MONEY },
                    };
                    expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ money: 1.23 });
                });

            });

        });

        it('Construct with array type with NO default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: {
                    type: controlType.array_types,
                    array_type: {
                        type: controlType.string,
                    },
                },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ test: [] });
        });
        it('Construct with array type with default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: {
                    type: controlType.array_types,
                    default: ['a', 'b'],
                    array_type: {
                        type: controlType.string,
                    },
                },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ test: ['a', 'b'] });
        });

        it('Construct with structure type with NO default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: {
                    type: controlType.structure,
                    structure: {
                        a: {
                            type: controlType.string,
                        },
                        b: {
                            type: controlType.string,
                        },
                    },
                },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA))
                .toEqual({ test: { a: null, b: null } });
        });

        it('Construct with structure type with default value', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                test: {
                    type: controlType.structure,
                    structure: {
                        a: {
                            default: 'a',
                            type: controlType.string,
                        },
                        b: {
                            default: 'b',
                            type: controlType.string,
                        },
                    },
                },
            };
            expect(FormConstructorSchemaWorker.constructValuesBySchema(SCHEMA)).toEqual({ test: { a: 'a', b: 'b' } });
        });
    });

    describe('Get schema item', () => {

        it('Get null item from empty schema', () => {
            const SCHEMA: Dict<ISchemaItem> = {};
            expect(FormConstructorSchemaWorker.getSchemaItem({ schema: SCHEMA, key: '' })).toEqual(null);
        });

        describe('Get item from primitive schemas without parents', () => {
            it('Get item from string schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    string_field: { type: controlType.string },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'string_field',
                })).toEqual({ type: controlType.string });
            });
            it('Get item from text schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    text_field: { type: controlType.text },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'text_field',
                })).toEqual({ type: controlType.text });
            });
            it('Get item from json schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    json_field: { type: controlType.json },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'json_field',
                })).toEqual({ type: controlType.json });
            });

            it('Get item from json schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    bool_field: { type: controlType.bool },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'bool_field',
                })).toEqual({ type: controlType.bool });
            });
            it('Get item from json schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    numeric_field: { type: controlType.numeric },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'numeric_field',
                })).toEqual({ type: controlType.numeric });
            });
            it('Get item from separator schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    separator_field: { type: controlType.separator },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'separator_field',
                })).toEqual({ type: controlType.separator });
            });
            it('Get item from ignore schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    ignore_field: { type: controlType.ignore },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'ignore_field',
                })).toEqual({ type: controlType.ignore });
            });

        });

        describe('Get item from not primitive schemas without parents', () => {
            it('Get item from array_types schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    array_types_field: { type: controlType.array_types },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'array_types_field',
                })).toEqual({ type: controlType.array_types });
            });

            it('Get item from variants schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variants_field: { type: controlType.variants },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'variants_field',
                })).toEqual({ type: controlType.variants });
            });

            it('Get item from structure schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    structure_field: { type: controlType.structure },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'structure_field',
                })).toEqual({ type: controlType.structure });
            });

            it('Get item from variable schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: { type: controlType.variable },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'variable_field',
                })).toEqual({ type: controlType.variable });
            });

            it('Get item from string_vector schema', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    string_vector_field: { type: controlType.string_vector },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'string_vector_field',
                })).toEqual({ type: controlType.string_vector });
            });
        });

        describe('Get primitive item from schemas with one parent', () => {
            //test only with controlType.string because this test check work with parent, work with primitives in testы above
            it('Get primitive item from array_types schema with one parent', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    array_types_field: {
                        type: controlType.array_types,
                        array_type: {
                            type: controlType.string,
                        },
                    },
                };
                expect(FormConstructorSchemaWorker
                    .getSchemaItem({ schema: SCHEMA, key: '0', parents: ['array_types_field'] }))
                    .toEqual({ type: controlType.string });
            });

            it('Get primitive item from structure schema with one parent', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            str: { type: controlType.string },
                        },
                    },
                };
                expect(FormConstructorSchemaWorker
                    .getSchemaItem({ schema: SCHEMA, key: 'str', parents: ['structure_field'] }))
                    .toEqual({ type: controlType.string });
            });

            it('Get primitive item from variable schema without default_fields with one parent (not founded)', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            string_field: { type: controlType.string },
                        },
                        variants_fields: {},
                    },
                };
                expect(FormConstructorSchemaWorker
                    .getSchemaItem({ schema: SCHEMA, key: 'test', parents: ['variable_field'] }))
                    .toEqual(null);
            });

            it('Get primitive item from variable schema without default_fields with one parent (control_field)', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            string_field: { type: controlType.string },
                        },
                        variants_fields: {},
                    },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'string_field',
                    parents: ['variable_field'],
                }))
                    .toEqual({ type: controlType.string });
            });

            it('Get primitive item from variable schema without default_fields with one parent (variants_fields)',
                () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        variable_field: {
                            type: controlType.variable,
                            control_field: {
                                string_field: { type: controlType.string },
                            },
                            variants_fields: {
                                a: {
                                    string_variants_field: { type: controlType.string },
                                },
                            },
                        },
                    };
                    expect(FormConstructorSchemaWorker.getSchemaItem({
                        schema: SCHEMA,
                        key: 'string_variants_field',
                        parents: ['variable_field'],
                        controlsValues: ['a'],
                    }))
                        .toEqual({ type: controlType.string });
                });

            it('Get primitive item from variable schema with default_fields with one parent (default_fields)', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {},
                        variants_fields: {},
                        default_fields: {
                            string_default_field: { type: controlType.string },
                        },
                    },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'string_default_field',
                    parents: ['variable_field'],
                }))
                    .toEqual({ type: controlType.string });
            });

        });

        describe('Get not primitive item from schemas with one parent', () => {
            //test only with controlType.structure because this test check work with parent, work with тще primitives in testы above
            it('Get structure item from array_types schema with one parent', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    array_types_field: {
                        type: controlType.array_types,
                        array_type: {
                            structure_field: {
                                type: controlType.structure,
                                structure: {},
                            },
                        },
                    },
                };

                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'structure_field',
                    parents: ['array_types_field'],
                }))
                    .toEqual({
                        structure_field: {
                            type: controlType.structure,
                            structure: {},
                        },
                    });
            });

            it('Get structure item from structure schema with one parent', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    structure_field: {
                        type: controlType.structure,
                        structure: {
                            structure_field_nested: {
                                type: controlType.structure,
                                structure: {},
                            },
                        },
                    },
                };

                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'structure_field_nested',
                    parents: ['structure_field'],
                }))
                    .toEqual({
                        type: controlType.structure,
                        structure: {},
                    });
            });

            it('Get structure item from variable schema without default_fields with one parent (not founded)', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {},
                        variants_fields: {},
                    },
                };
                expect(FormConstructorSchemaWorker
                    .getSchemaItem({ schema: SCHEMA, key: 'test', parents: ['variable_field'] }))
                    .toEqual(null);
            });

            it('Get structure item from variable schema without default_fields with one parent (control_field)', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            structure_field: {
                                type: controlType.structure,
                                structure: {},
                            },
                        },
                        variants_fields: {},
                    },
                };
                expect(FormConstructorSchemaWorker.getSchemaItem({
                    schema: SCHEMA,
                    key: 'structure_field',
                    parents: ['variable_field'],
                }))
                    .toEqual({
                        type: controlType.structure,
                        structure: {},
                    });
            });

            it('Get structure item from variable schema without default_fields with one parent (variants_field)',
                () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        variable_field: {
                            type: controlType.variable,
                            control_field: {},
                            variants_fields: {
                                a: {
                                    structure_field: {
                                        type: controlType.structure,
                                        structure: {},
                                    },
                                },
                            },
                        },
                    };
                    expect(FormConstructorSchemaWorker.getSchemaItem({
                        schema: SCHEMA,
                        key: 'structure_field',
                        parents: ['variable_field'],
                        controlsValues: ['a'],
                    }))
                        .toEqual({
                            type: controlType.structure,
                            structure: {},
                        });
                });

            it('Get structure item from variable schema without default_fields with one parent (default_fields)',
                () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        variable_field: {
                            type: controlType.variable,
                            control_field: {},
                            variants_fields: {},
                            default_fields: {
                                structure_field: {
                                    type: controlType.structure,
                                    structure: {},
                                },
                            },
                        },
                    };
                    expect(FormConstructorSchemaWorker.getSchemaItem({
                        schema: SCHEMA,
                        key: 'structure_field',
                        parents: ['variable_field'],
                    }))
                        .toEqual({
                            type: controlType.structure,
                            structure: {},
                        });
                });
        });

        it('Get primitive item from nested variable schema without default_fields (variants_field)', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                variable_field: {
                    type: controlType.variable,
                    control_field: {},
                    variants_fields: {
                        a: {
                            variable_field_nested_1: {
                                type: controlType.variable,
                                control_field: {},
                                variants_fields: {
                                    b: {
                                        variable_field_nested_2: {
                                            type: controlType.variable,
                                            control_field: {},
                                            variants_fields: {
                                                c: {
                                                    string_field: {
                                                        type: controlType.string,
                                                    },
                                                },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            };
            expect(FormConstructorSchemaWorker.getSchemaItem({
                schema: SCHEMA,
                key: 'string_field',
                parents: ['variable_field', 'variable_field_nested_1', 'variable_field_nested_2'],
                controlsValues: ['a', 'b', 'c'],
            }))
                .toEqual({
                    type: controlType.string,
                });
        });
    });
});
