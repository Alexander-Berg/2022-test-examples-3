import { shallow } from 'enzyme';
import * as React from 'react';

import { Dict } from '../../../types';
import { COINS_LENGTH, COINS_PER_RUBLE } from '../../constants';
import { JSON_TAB_SIZE } from './constants';
import { FormConstructorInputs } from './FormConstructorInputs';
import { controlType, ISchemaItem, SchemaItemVisual } from './types';

describe('FormConstructor component', () => {
    it('Renders with empty schema', () => {
        const wrapper = shallow(<FormConstructorInputs schema={{}}/>);
        expect(wrapper.exists()).toEqual(true);
    });

    describe('Renders with string schema', () => {
        const SCHEMA: Dict<ISchemaItem> = {
            name: {
                type: controlType.string,
            },
        };

        it('Renders with simple string schema', () => {
            const wrapper = shallow(<FormConstructorInputs schema={SCHEMA}/>);

            expect(wrapper.exists()).toEqual(true);
        });
    });

    describe('Set initial Data', () => {

        it('Doesn\'t change value if no initial Data ', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                name: { type: controlType.string, default: 'abc' },
            };

            const VALUES = { name: 'abc' };
            const INITIAL_DATA = {};
            const PARENTS: string[] = [];

            const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
            expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ name: 'abc' });
        });

        it('Change value if initial Data ', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                name: { type: controlType.string, default: 'abc' },
            };
            const VALUES = { name: 'abc' };
            const INITIAL_DATA = { name: 'def' };
            const PARENTS: string[] = [];

            const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
            expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ name: 'def' });
        });

        describe('Work with string', () => {
            it('Doesn\'t change empty string value if no initial data ', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    name: { type: controlType.string, default: null },
                };
                const VALUES = { name: null };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ name: null });
            });

            it('Doesn\'t change not empty string value if no initial data ', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    name: { type: controlType.string, default: 'abc' },
                };
                const VALUES = { name: 'abc' };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ name: 'abc' });
            });

            it('Change string value if initial data ', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    name: { type: controlType.string, default: 'abc' },
                };
                const VALUES = { name: 'abc' };
                const INITIAL_DATA = { name: 'dfe' };
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ name: 'dfe' });
            });

            it('Doesn\'t change empty color string value if no initial data ', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    color: { type: controlType.string, visual: SchemaItemVisual.COLOR, default: null },
                };
                const VALUES = { color: null };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ color: null });
            });

            it('Doesn\'t change not empty color string value if no initial data ', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    color: { type: controlType.string, visual: SchemaItemVisual.COLOR, default: '#FF0000' },
                };
                const VALUES = { color: '#FF0000' };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ color: '#FF0000' });
            });

            it('Change color string value if initial data with hash', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    color: { type: controlType.string, visual: SchemaItemVisual.COLOR, default: '#FF0000' },
                };
                const VALUES = { color: '#FF0000' };
                const INITIAL_DATA = { color: '#00FF00' };
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ color: '#00FF00' });
            });

            it('Change color string value if initial data without hash', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    color: { type: controlType.string, visual: SchemaItemVisual.COLOR, default: '#FF0000' },
                };
                const VALUES = { color: '#FF0000' };
                const INITIAL_DATA = { color: '00FF00' };
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ color: '#00FF00' });
            });

        });

        describe('Work with json', () => {

            it('Doesn\'t change json if no initial Data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    json_field: { type: controlType.json },
                };
                const VALUES = { json_field: null };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const RESULT = { json_field: null };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change json if initial Data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    json_field: { type: controlType.json },
                };
                const VALUES = { json_field: null };
                const INITIAL_DATA = { json_field: { a: 1 } };
                const PARENTS: string[] = [];

                const RESULT = { json_field: JSON.stringify({ a: 1 }, null, JSON_TAB_SIZE) };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

        });

        describe('Work with numeric', () => {
            it('Doesn\'t change empty numeric value if no initial data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    number: { type: controlType.numeric, default: null },
                };
                const VALUES = { number: null };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ number: null });
            });

            it('Doesn\'t change not empty numeric value if no initial data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    number: { type: controlType.numeric, default: 10 },
                };
                const VALUES = { number: 10 };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ number: 10 });
            });

            it('Change numeric value if initial data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    number: { type: controlType.numeric, default: null },
                };
                const VALUES = { number: null };
                const INITIAL_DATA = { number: 10 };
                const PARENTS: string[] = [];

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ number: 10 });
            });

            describe('Work with TIMESTAMP', () => {

                it('Doesn\'t change empty numeric timestamp value if no initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.TIMESTAMP },
                    };
                    const VALUES = { number: null };
                    const INITIAL_DATA = {};
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual({ number: null });
                });

                it('Doesn\'t change not empty numeric timestamp value if no initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: 1583843078, visual: SchemaItemVisual.TIMESTAMP },
                    };
                    const VALUES = { number: 1583843078 };
                    const INITIAL_DATA = {};
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 1583843078 });
                });

                it('Change numeric timestamp value if initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.TIMESTAMP },
                    };
                    const VALUES = { number: null };
                    const INITIAL_DATA = { number: 1583843078 };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 1583843078 });
                });

                it('Doesn\'t change numeric timestamp value if invalid initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: 1583843078, visual: SchemaItemVisual.TIMESTAMP },
                    };
                    const VALUES = { number: 1583843078 };
                    const INITIAL_DATA = { number: 'invalid_date' };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 1583843078 });
                });

                it('Change numeric timestamp value if valid initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.TIMESTAMP },
                    };
                    const VALUES = { number: null };
                    const INITIAL_DATA = { number: 1583843078 };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 1583843078 });
                });
            });

            describe('Work with RUBS', () => {

                it('Doesn\'t change empty numeric rubs value if no initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.RUBS },
                    };
                    const VALUES = { number: null };
                    const INITIAL_DATA = {};
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: null });
                });

                it('Doesn\'t change not empty numeric rubs value if no initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: 100, visual: SchemaItemVisual.RUBS },
                    };
                    const VALUES = { number: 100 };
                    const INITIAL_DATA = {};
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 100 });
                });

                it('Change numeric rubs value if initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.RUBS },
                    };
                    const INITIAL_COINS_VALUE = 100;
                    const VALUES = { number: null };
                    const INITIAL_DATA = { number: INITIAL_COINS_VALUE };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(
                        { number: +((INITIAL_COINS_VALUE / COINS_PER_RUBLE).toFixed(COINS_LENGTH)) },
                    );
                });

                it('Doesn\'t change numeric rubs value if initial data is not a number', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: 100, visual: SchemaItemVisual.RUBS },
                    };
                    const VALUES = { number: 100 };
                    const INITIAL_DATA = { number: '999' };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 100 });
                });

                it('Change numeric rubs value if initial data is a number', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.RUBS },
                    };
                    const INITIAL_COINS_VALUE = 100;
                    const VALUES = { number: null };
                    const INITIAL_DATA = { number: INITIAL_COINS_VALUE };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(
                        { number: +((INITIAL_COINS_VALUE / COINS_PER_RUBLE).toFixed(COINS_LENGTH)) },
                    );
                });

            });

            describe('Work with MONEY', () => {
                it('Doesn\'t change empty numeric money value if no initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.MONEY },
                    };
                    const VALUES = { number: null };
                    const INITIAL_DATA = {};
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: null });
                });

                it('Doesn\'t change not empty numeric money value if no initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: 100, visual: SchemaItemVisual.MONEY },
                    };
                    const VALUES = { number: 100 };
                    const INITIAL_DATA = {};
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 100 });
                });

                it('Change numeric money value if initial data', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.MONEY },
                    };
                    const INITIAL_COINS_VALUE = 100;
                    const VALUES = { number: null };
                    const INITIAL_DATA = { number: INITIAL_COINS_VALUE };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(
                        { number: +((INITIAL_COINS_VALUE / COINS_PER_RUBLE).toFixed(COINS_LENGTH)) },
                    );
                });

                it('Doesn\'t change numeric money value if initial data is not a number', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: 100, visual: SchemaItemVisual.MONEY },
                    };
                    const VALUES = { number: 100 };
                    const INITIAL_DATA = { number: '999' };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS))
                        .toEqual({ number: 100 });
                });

                it('Change numeric money value if initial data is a number', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        number: { type: controlType.numeric, default: null, visual: SchemaItemVisual.MONEY },
                    };
                    const INITIAL_COINS_VALUE = 100;
                    const VALUES = { number: null };
                    const INITIAL_DATA = { number: INITIAL_COINS_VALUE };
                    const PARENTS: string[] = [];

                    const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(
                        { number: +((INITIAL_COINS_VALUE / COINS_PER_RUBLE).toFixed(COINS_LENGTH)) },
                    );
                });
            });
        });

        describe('Work with structure', () => {
            it('Doesn\'t change empty structure value if no initial data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    structure_field: {
                        type: controlType.structure,
                        structure: { string_field: { type: controlType.string } },
                    },
                };

                const VALUES = { structure_field: { string_field: null } };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];
                const RESULT = { structure_field: { string_field: null } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Doesn\'t change not empty structure value if no initial data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    structure_field: {
                        type: controlType.structure,
                        structure: { string_field: { type: controlType.string } },
                    },
                };

                const VALUES = { structure_field: { string_field: 'string' } };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];
                const RESULT = { structure_field: { string_field: 'string' } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change empty structure value if no initial data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    structure_field: {
                        type: controlType.structure,
                        structure: { string_field: { type: controlType.string } },
                    },
                };

                const VALUES = { structure_field: { string_field: null } };
                const INITIAL_DATA = { structure_field: { string_field: 'string' } };
                const PARENTS: string[] = [];
                const RESULT = { structure_field: { string_field: 'string' } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });
        });

        describe('Work with variables', () => {

            it('Set control field value if initial Data in variants', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: null } };
                const INITIAL_DATA = { control_variants_fields: 'simple' };
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: 'simple', string_field: null } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Set control field value if initial Data not in variants and in default', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                        default_fields: {
                            numeric_field: { type: controlType.numeric },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: null } };
                const INITIAL_DATA = { control_variants_fields: 'another' };
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: 'another', numeric_field: null } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Set control field value if initial Data not in variants and no default values', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: null } };
                const INITIAL_DATA = { control_variants_fields: 'another' };
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: 'another' } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Don\'t change control field value if no initial Data', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                                default: 'simple',
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: 'simple', string_field: null } };
                const INITIAL_DATA = {};
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: 'simple', string_field: null } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change control field value if initial Data in variants', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                                default: 'simple',
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: 'simple', string_field: null } };
                const INITIAL_DATA = { control_variants_fields: 'hard' };
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: 'hard', json: null } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Remove control field value if initial Data in variants', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                                default: 'simple',
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: 'simple', string_field: null } };
                const INITIAL_DATA = { control_variants_fields: null };
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: null } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change primitive variant field value', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['simple', 'hard'],
                                default: 'simple',
                            },
                        },
                        variants_fields: {
                            simple: {
                                string_field: { type: controlType.string },
                            },
                            hard: {
                                json: { type: controlType.json },
                            },
                        },
                    },
                };

                const VALUES = { variable_field: { control_variants_fields: 'simple', string_field: null } };
                const INITIAL_DATA = { string_field: 'string' };
                const PARENTS: string[] = [];
                const RESULT = { variable_field: { control_variants_fields: 'simple', string_field: 'string' } };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change structure variant field value', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['structure_variants'],
                            },
                        },
                        variants_fields: {
                            structure_variants: {
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
                        control_variants_fields: 'structure_variants',
                        structure_field: { string_field: 'second_string' },
                    },
                };
                const INITIAL_DATA = {
                    structure_field: { string_field: 'second_string' },
                };
                const PARENTS: string[] = [];
                const RESULT = {
                    variable_field: {
                        control_variants_fields: 'structure_variants',
                        structure_field: { string_field: 'second_string' },
                    },
                };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change control and variant field value', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_fields: {
                                type: controlType.variants,
                                variants: ['structure_variants'],
                            },
                        },
                        variants_fields: {
                            structure_variants: {
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
                    variable_field: { control_variants_fields: null },
                };
                const INITIAL_DATA = {
                    control_variants_fields: 'structure_variants',
                    structure_field: { string_field: 'string' },
                };
                const PARENTS: string[] = [];
                const RESULT = {
                    variable_field: {
                        control_variants_fields: 'structure_variants',
                        structure_field: { string_field: 'string' },
                    },
                };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change control field value in nested variable field in structure', () => {
                const SCHEMA: Dict<ISchemaItem> = {
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
                                    },
                                },
                            },
                        },
                    },
                };

                const VALUES = { structure_field: { variable_field: { control_variants_fields: null } } };
                const INITIAL_DATA = {
                    structure_field: { control_variants_fields: 'simple' },
                };
                const PARENTS: string[] = [];
                const RESULT = {
                    structure_field: {
                        variable_field: { control_variants_fields: 'simple', string_field: null },
                    },
                };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change control field value in nested variable field in one more variable without selecting', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field_first: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_field_first: {
                                type: controlType.variants,
                                variants: ['with_nested_variable'],
                            },
                        },
                        variants_fields: {
                            with_nested_variable: {
                                variable_field_second: {
                                    type: controlType.variable,
                                    control_field: {
                                        control_variants_field_second: {
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
                    },
                };

                const VALUES = { variable_field_first: { control_variants_field_first: null } };
                const INITIAL_DATA = { control_variants_field_first: 'with_nested_variable' };
                const PARENTS: string[] = [];
                const RESULT = {
                    variable_field_first: {
                        control_variants_field_first: 'with_nested_variable',
                        variable_field_second: { control_variants_field_second: null },
                    },
                };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });

            it('Change control field value in nested variable field in one more variable with selecting', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    variable_field_first: {
                        type: controlType.variable,
                        control_field: {
                            control_variants_field_first: {
                                type: controlType.variants,
                                variants: ['with_nested_variable'],
                            },
                        },
                        variants_fields: {
                            with_nested_variable: {
                                variable_field_second: {
                                    type: controlType.variable,
                                    control_field: {
                                        control_variants_field_second: {
                                            type: controlType.variants,
                                            variants: ['simple'],
                                            default: 'simple',
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

                const VALUES = { variable_field_first: { control_variants_field_first: null } };
                const INITIAL_DATA = { control_variants_field_first: 'with_nested_variable' };
                const PARENTS: string[] = [];
                const RESULT = {
                    variable_field_first: {
                        control_variants_field_first: 'with_nested_variable',
                        variable_field_second: { control_variants_field_second: 'simple', string_field: null },
                    },
                };

                const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
            });
        });

        it('Change text value if initial Data', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                text_field: { type: controlType.text, default: 'first_text' },
            };

            const VALUES = { text_field: 'first_text' };
            const INITIAL_DATA = { text_field: 'second_text' };
            const PARENTS: string[] = [];
            const RESULT = { text_field: 'second_text' };

            const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
            expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
        });

        it('Change array value if initial Data', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                array_field: {
                    type: controlType.array_types,
                    array_type: { type: controlType.string },
                    default: ['a', 'b', 'c'],
                },
            };

            const VALUES = { array_field: ['a', 'b', 'c'] };
            const INITIAL_DATA = { array_field: ['d', 'f', 'e'] };
            const PARENTS: string[] = [];
            const RESULT = { array_field: ['d', 'f', 'e'] };

            const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
            expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
        });

        it('Change boolean value if initial Data', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                bool_field: {
                    type: controlType.bool,
                    default: true,
                },
            };

            const VALUES = { bool_field: true };
            const INITIAL_DATA = { bool_field: false };
            const PARENTS: string[] = [];
            const RESULT = { bool_field: false };

            const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
            expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
        });

        it('Change variants value if initial Data', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                variants_field: {
                    type: controlType.variants,
                    variants: ['a', 'b', 'c'],
                    default: ['a'],
                },
            };

            const VALUES = { variants_field: ['a'] };
            const INITIAL_DATA = { variants_field: ['b'] };
            const PARENTS: string[] = [];
            const RESULT = { variants_field: ['b'] };

            const wrapper: any = shallow(<FormConstructorInputs schema={SCHEMA}/>);
            expect(wrapper.instance().setInitialData(VALUES, INITIAL_DATA, PARENTS)).toEqual(RESULT);
        });

        describe('Build Excluded structures object', () => {

            it('Build Excluded structures object for schema without objects and arrays', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    name: {
                        type: controlType.string,
                    },
                };

                const wrapper = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                const state: any = wrapper.instance().state;

                expect(state.excludedStructures).toEqual({});
            });

            describe('Build Excluded structures object without initialData', () => {
                it('Build Excluded structures object for schema with not required structure and without initialData',
                    () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.structure,
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                it('Build Excluded structures object for schema with not required array and without initialData',
                    () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.array_types,
                                array_type: {
                                    type: controlType.string,
                                },
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                it('Build Excluded structures object for schema with required structure and without initialData',
                    () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.structure,
                                required: true,
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                it('Build Excluded structures object for schema with required array and without initialData', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        name: {
                            type: controlType.array_types,
                            required: true,
                            array_type: {
                                type: controlType.string,
                            },
                        },
                    };

                    const wrapper = shallow(<FormConstructorInputs schema={SCHEMA}/>);
                    const state: any = wrapper.instance().state;

                    expect(state.excludedStructures).toEqual({});
                });
            });

            describe('Build Excluded structures object with initialData', () => {

                describe('Build Excluded structures object with initialData without field in initialData', () => {

                    it('Build Excluded structures object for schema with initialData without field in initialData and NOT required structure', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.structure,
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{}} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({ name: true });
                    });

                    it('Build Excluded structures object for schema with initialData without field in initialData and NOT required array', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.array_types,
                                array_type: {
                                    type: controlType.string,
                                },
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{}} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({ name: true });
                    });

                    it('Build Excluded structures object  for schema with initialData without field in initialData and required structure', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                required: true,
                                type: controlType.structure,
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{}} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                    it('Build Excluded structures object  for schema with initialData without field in initialData and required array', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                required: true,
                                type: controlType.array_types,
                                array_type: {
                                    type: controlType.string,
                                },
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{}} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });
                });

                describe('Build Excluded structures object with initialData with field in initialData', () => {

                    it('Build Excluded structures object for schema with initialData with field in initialData and NOT required structure', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.structure,
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{ name: {} }} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                    it('Build Excluded structures object for schema with initialData with field in initialData and NOT required array', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                type: controlType.array_types,
                                array_type: {
                                    type: controlType.string,
                                },
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{ name: [] }} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                    it('Build Excluded structures object for schema with initialData with field in initialData and required structure', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                required: true,
                                type: controlType.structure,
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{ name: {} }} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                    it('Build Excluded structures object for schema with initialData with field in initialData and required array', () => {
                        const SCHEMA: Dict<ISchemaItem> = {
                            name: {
                                required: true,
                                type: controlType.array_types,
                                array_type: {
                                    type: controlType.string,
                                },
                            },
                        };

                        const wrapper = shallow(<FormConstructorInputs initialData={{ name: [] }} schema={SCHEMA}/>);
                        const state: any = wrapper.instance().state;

                        expect(state.excludedStructures).toEqual({});
                    });

                });

            });
        });
    });
});
