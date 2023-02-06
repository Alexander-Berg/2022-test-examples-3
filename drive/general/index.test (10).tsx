/*eslint-disable no-magic-numbers*/
import { Dict } from '../../../../types';
import { controlType, ISchemaItem } from '../types';
import { ArrayAction, DEFAULT_TAB_NAME, FormConstructorTabsWorker, IFormTabs, ROOT_KEY } from './index';

const TEST_TAB_NAME = 'tab';
const TEST_TAB_NAME_2 = 'tab_2';

describe('FormConstructorTabsWorker', () => {
    describe('Construct by Schema', () => {

        describe('Construct by Schema for primitive types', () => {
            describe('Construct tabs for one-field schema without tab_name', () => {
                it('Construct one tab', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        string_field: { type: controlType.string },
                    };

                    expect(FormConstructorTabsWorker
                        .constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].tabs.length)
                        .toEqual(1);
                });

                it('Construct one tab with right name', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        string_field: { type: controlType.string },
                    };

                    expect(FormConstructorTabsWorker
                        .constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].tabs[0].name)
                        .toEqual(DEFAULT_TAB_NAME);
                });

                it('Construct one tab with right countItems', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        string_field: { type: controlType.string },
                    };

                    expect(FormConstructorTabsWorker
                        .constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].tabs[0].countItems)
                        .toEqual(1);
                });

                it('Construct tabs with right currentTab', () => {
                    const SCHEMA: Dict<ISchemaItem> = {
                        string_field: { type: controlType.string },
                    };

                    expect(FormConstructorTabsWorker
                        .constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].currentTab)
                        .toEqual(DEFAULT_TAB_NAME);
                });
            });

            it('Construct tabs for one-field schema with tab_name', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    string_field: { type: controlType.string, tab_name: TEST_TAB_NAME },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{
                            name: TEST_TAB_NAME,
                            countItems: 1,
                        }],
                        currentTab: TEST_TAB_NAME,
                    },
                };
                expect(FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })).toEqual(TABS_RESULT);
            });

            it('Construct tabs for two-field schema without tab_name', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    string_field: { type: controlType.string },
                    string_field_2: { type: controlType.string },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [
                            {
                                name: DEFAULT_TAB_NAME,
                                countItems: 2,
                            }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };
                expect(FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })).toEqual(TABS_RESULT);
            });

            it('Construct tabs for two-field schema with one tab_name', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    string_field: { type: controlType.string, tab_name: TEST_TAB_NAME },
                    string_field_2: { type: controlType.string },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [
                            {
                                name: DEFAULT_TAB_NAME,
                                countItems: 1,
                            },
                            {
                                name: TEST_TAB_NAME,
                                countItems: 1,
                            }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };
                expect(FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })).toEqual(TABS_RESULT);
            });

            it('Construct tabs for two-field schema with two tab_name', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    string_field: { type: controlType.string, tab_name: TEST_TAB_NAME },
                    string_field_2: { type: controlType.string, tab_name: TEST_TAB_NAME_2 },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [
                            {
                                name: TEST_TAB_NAME,
                                countItems: 1,
                            },
                            {
                                name: TEST_TAB_NAME_2,
                                countItems: 1,
                            }],
                        currentTab: TEST_TAB_NAME,
                    },
                };
                expect(FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })).toEqual(TABS_RESULT);
            });

        });

        it('Construct tabs for schema with structure', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                structure_field: {
                    type: controlType.structure,
                    display_name: 'structure_field',
                    order: 9,
                    structure: {
                        str: {
                            type: controlType.string, display_name: 'string_structure_field', order: 1,
                        },
                        nmb: {
                            type: controlType.numeric, display_name: 'numeric_structure_field', order: 2,
                        },
                    },
                },
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                structure_field: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 2,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };
            expect(FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with primitive array with values', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                array_types_field: {
                    type: controlType.array_types,
                    array_type: {
                        type: controlType.string,
                        order: 1,
                    },
                },
            };

            const VALUES = {
                array_types_field: [
                    'a', 'b',
                ],
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                'array_types_field.0': {
                    tabs: [{
                        name: DEFAULT_TAB_NAME,
                        countItems: 1,
                    }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                'array_types_field.1': {
                    tabs: [{
                        name: DEFAULT_TAB_NAME,
                        countItems: 1,
                    }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with structure array', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                array_types_structure_field: {
                    type: controlType.array_types,
                    array_type: {
                        str: {
                            type: controlType.string,
                        },
                        nmb: {
                            type: controlType.numeric,
                        },
                    },
                },
            };

            const VALUES = {
                array_types_structure_field: [
                    {
                        str: 'a',
                        nmb: 1,
                    },
                    {
                        str: 'b',
                        nmb: 2,
                    },
                ],
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                'array_types_structure_field.0': {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 2,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },

                'array_types_structure_field.1': {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 2,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with structure array with values', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                array_types_structure_field: {
                    type: controlType.array_types,
                    array_type: {
                        str: {
                            type: controlType.string,
                        },
                        nmb: {
                            type: controlType.numeric,
                        },
                    },
                },
            };

            const VALUES: Dict<any> = {
                array_types_structure_field: [
                    {
                        str: 'str_1',
                        nmb: 1,
                    },
                    {
                        str: 'str_2',
                        nmb: 2,
                    },
                    {
                        str: 'str_3',
                        nmb: 3,
                    },
                ],
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                'array_types_structure_field.0': {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 2,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                'array_types_structure_field.1': {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 2,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                'array_types_structure_field.2': {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 2,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with variable without control field value', () => {
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
                            string_field: { type: controlType.string, display_name: 'simple_string' },
                        },
                        hard: {
                            json: { type: controlType.json },
                        },
                    },
                },
            };

            const VALUES = {
                variable_field: {},
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with variable with control field value and with this value in variants', () => {
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
                            string_field: { type: controlType.string, display_name: 'simple_string' },
                        },
                        hard: {
                            json: { type: controlType.json },
                        },
                    },
                },
            };

            const VALUES = {
                variable_field: { control_variants_fields: 'simple' },
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                variable_field: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with variable with control field value and with this in defaults', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                variable_field: {
                    type: controlType.variable,
                    control_field: {
                        control_variants_fields: {
                            type: controlType.variants,
                            variants: ['simple', 'hard'],
                        },
                    },
                    default_fields: {
                        string_field: { type: controlType.string, display_name: 'simple_string' },
                    },
                },
            };

            const VALUES = {
                variable_field: { control_variants_fields: 'simple' },
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                variable_field: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Construct tabs for schema with variable with control field value and without this value in variants or defaults', () => {
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
                            string_field: { type: controlType.string, display_name: 'simple_string' },
                        },
                        hard: {
                            json: { type: controlType.json },
                        },
                    },
                },
            };

            const VALUES = {
                variable_field: { control_variants_fields: 'another' },
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [
                        {
                            name: DEFAULT_TAB_NAME,
                            countItems: 1,
                        }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            expect(FormConstructorTabsWorker.constructTabsBySchema({
                schemaInit: SCHEMA,
                values: VALUES,
            })).toEqual(TABS_RESULT);
        });

        it('Sort tabs with both string names', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                string_field: { type: controlType.string, tab_name: 'a' },
                string_field_2: { type: controlType.string, tab_name: 'b' },
            };

            const tabs = FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].tabs;
            const tabNames = tabs.map(tab => tab.name);

            expect(tabNames).toEqual(['a', 'b']);
        });

        it('Sort tabs with both numeric names', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                string_field: { type: controlType.string, tab_name: 1 },
                string_field_2: { type: controlType.string, tab_name: 2 },
            };

            const tabs = FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].tabs;
            const tabNames = tabs.map(tab => tab.name);

            expect(tabNames).toEqual([1, 2]);
        });

        it('Sort tabs with string and numeric names', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                string_field: { type: controlType.string, tab_name: 1 },
                string_field_2: { type: controlType.string, tab_name: 'a' },
            };

            const tabs = FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA })[ROOT_KEY].tabs;
            const tabNames = tabs.map(tab => tab.name);

            expect(tabNames).toEqual([1, 'a']);
        });
    });

    describe('Get tabs info', () => {
        it('Get tabs info for one-level schema', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                string_field: { type: controlType.string },
            };

            const TABS_RESULT: IFormTabs = {
                tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                currentTab: DEFAULT_TAB_NAME,
            };

            expect(FormConstructorTabsWorker.getTabsInfoByPath(
                {
                    tabsInfo: FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA }),
                })).toEqual(TABS_RESULT);
        });

        it('Get tabs info for one-level schema', () => {
            const SCHEMA: Dict<ISchemaItem> = {
                array_types_structure_field: {
                    type: controlType.array_types,
                    array_type: {
                        str: {
                            type: controlType.string,
                        },
                        nmb: {
                            type: controlType.numeric,
                        },
                    },
                },
            };

            const VALUES: Dict<any> = {
                array_types_structure_field: [
                    {
                        str: 'str_1',
                        nmb: 1,
                    },
                    {
                        str: 'str_2',
                        nmb: 2,
                    },
                    {
                        str: 'str_3',
                        nmb: 3,
                    },
                ],
            };

            const TABS_RESULT: IFormTabs = {
                tabs: [{ name: DEFAULT_TAB_NAME, countItems: 2 }],
                currentTab: DEFAULT_TAB_NAME,
            };

            expect(FormConstructorTabsWorker.getTabsInfoByPath(
                {
                    tabsInfo: FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA, values: VALUES }),
                    parents: ['array_types_structure_field', '1'],
                })).toEqual(TABS_RESULT);
        });
    });

    describe('Change current tab by path', () => {
        it('Don\'t change init tabsInfo', () => {
            const INIT_TABS_INFO = {
                [ROOT_KEY]: {
                    tabs: [{ name: 'one', countItems: 1 }, { name: 'two', countItems: 1 }],
                    currentTab: 'one',
                },
            };

            const TABS_INFO = {
                [ROOT_KEY]: {
                    tabs: [{ name: 'one', countItems: 1 }, { name: 'two', countItems: 1 }],
                    currentTab: 'one',
                },
            };

            FormConstructorTabsWorker.changeCurrentTabByPath({
                tabsInfo: TABS_INFO,
                parents: [],
                currentTab: 'two',
            });

            expect(TABS_INFO).toEqual(INIT_TABS_INFO);
        });

        it('Don\'t change tabsInfo if currentTab is not in tabs', () => {
            const TABS_INFO = {
                [ROOT_KEY]: {
                    tabs: [{ name: 'one', countItems: 1 }, { name: 'two', countItems: 1 }],
                    currentTab: 'one',
                },
            };

            const TABS_INFO_RESULT = {
                [ROOT_KEY]: {
                    tabs: [{ name: 'one', countItems: 1 }, { name: 'two', countItems: 1 }],
                    currentTab: 'one',
                },
            };

            expect(FormConstructorTabsWorker.changeCurrentTabByPath({
                tabsInfo: TABS_INFO,
                parents: [],
                currentTab: 'three',
            })).toEqual(TABS_INFO_RESULT);
        });

        it('Change tabsInfo', () => {
            const TABS_INFO = {
                [ROOT_KEY]: {
                    tabs: [{ name: 'one', countItems: 1 }, { name: 'two', countItems: 1 }],
                    currentTab: 'one',
                },
            };

            const TABS_INFO_RESULT = {
                [ROOT_KEY]: {
                    tabs: [{ name: 'one', countItems: 1 }, { name: 'two', countItems: 1 }],
                    currentTab: 'two',
                },
            };

            expect(FormConstructorTabsWorker.changeCurrentTabByPath({
                tabsInfo: TABS_INFO,
                parents: [],
                currentTab: 'two',
            })).toEqual(TABS_INFO_RESULT);
        });
    });

    describe('Change tabs info after changing array', () => {
        describe('Change tabs info after adding item in array', () => {
            it('Change tabs info for empty array', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    array_types_structure_field: {
                        type: controlType.array_types,
                        array_type: {
                            str: {
                                type: controlType.string,
                                tab_name: 'tab',
                            },
                            nmb: {
                                type: controlType.numeric,
                            },
                        },
                    },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.0': {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }, { name: 'tab', countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: FormConstructorTabsWorker.constructTabsBySchema({ schemaInit: SCHEMA }),
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.ADD,
                        schema: SCHEMA,
                    })).toEqual(TABS_RESULT);
            });

            it('Change tabs info for not empty array', () => {
                const SCHEMA: Dict<ISchemaItem> = {
                    array_types_structure_field: {
                        type: controlType.array_types,
                        array_type: {
                            str: {
                                type: controlType.string,
                                tab_name: 'tab',
                            },
                            nmb: {
                                type: controlType.numeric,
                            },
                        },
                    },
                };

                const VALUES: Dict<any> = {
                    array_types_structure_field: [
                        {
                            str: 'str_1',
                            nmb: 1,
                        },
                        {
                            str: 'str_2',
                            nmb: 2,
                        },
                    ],
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.0': {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }, { name: 'tab', countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }, { name: 'tab', countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.2': {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }, { name: 'tab', countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: FormConstructorTabsWorker.constructTabsBySchema({
                            schemaInit: SCHEMA,
                            values: VALUES,
                        }),
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.ADD,
                        schema: SCHEMA,
                    })).toEqual(TABS_RESULT);
            });
        });

        describe('Change tabs info after removing item in array', () => {
            it('Change tabs info for one-element array', () => {
                const TABS_BEFORE_REMOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_REMOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.REMOVE,
                        index: 0,
                    })).toEqual(TABS_RESULT);
            });

            it('Change tabs info for array after removing first element', () => {
                const TABS_BEFORE_REMOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_2',
                    },
                    'array_types_structure_field.2': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_2',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_REMOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.REMOVE,
                        index: 0,
                    })).toEqual(TABS_RESULT);
            });

            it('Change tabs info for array after removing middle element', () => {
                const TABS_BEFORE_REMOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_2',
                    },
                    'array_types_structure_field.2': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_REMOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.REMOVE,
                        index: 1,
                    })).toEqual(TABS_RESULT);
            });

            it('Change tabs info for array after removing last element', () => {
                const TABS_BEFORE_REMOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_2',
                    },
                    'array_types_structure_field.2': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, { name: 'tab_2', countItems: 1 }],
                        currentTab: 'tab_2',
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_REMOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.REMOVE,
                        index: 2,
                    })).toEqual(TABS_RESULT);
            });

            it('Don\'t change tabs info for not existing index', () => {
                const TABS_BEFORE_REMOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_REMOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.REMOVE,
                        index: 10,
                    })).toEqual(TABS_RESULT);
            });
        });

        describe('Change tabs info after moving item in array', () => {

            it('Don\'t change tabs info for one-element array', () => {
                const TABS_BEFORE_MOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_MOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.MOVE_DOWN,
                        index: 0,
                    })).toEqual(TABS_RESULT);
            });

            it('Change tabs info for swipe array down', () => {
                const TABS_BEFORE_MOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_2',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_2',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_MOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.MOVE_DOWN,
                        index: 1,
                    })).toEqual(TABS_RESULT);
            });

            it('Change tabs info for swipe array up', () => {
                const TABS_BEFORE_MOVE = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_2',
                    },
                    [ROOT_KEY]: { tabs: [{ name: 'default', countItems: 1 }], currentTab: 'default' },
                };

                const TABS_RESULT: Dict<IFormTabs> = {
                    'array_types_structure_field.0': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_2',
                    },
                    'array_types_structure_field.1': {
                        tabs: [{ name: 'tab_1', countItems: 1 }, {
                            name: 'tab_2',
                            countItems: 1,
                        }], currentTab: 'tab_1',
                    },
                    [ROOT_KEY]: {
                        tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                        currentTab: DEFAULT_TAB_NAME,
                    },
                };

                expect(FormConstructorTabsWorker.changeArrayTabInfo(
                    {
                        tabsInfo: TABS_BEFORE_MOVE,
                        parents: ['array_types_structure_field'],
                        action: ArrayAction.MOVE_UP,
                        index: 0,
                    })).toEqual(TABS_RESULT);
            });

        });
    });

    describe('Change tabs info after changing variable control value', () => {

        it('Change tabs info after selecting control field value over empty value', () => {
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
                            json: { type: controlType.json, tab_name: 'json_tab' },
                        },
                    },
                },
            };

            const TABS_INFO = {
                [ROOT_KEY]: {
                    tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                variable_field: {
                    tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            const VALUES = {
                variable_field: {
                    control_variants_fields: 'simple',
                },
            };

            expect(FormConstructorTabsWorker.changeControlVariableTabInfo(
                {
                    tabsInfo: TABS_INFO,
                    schema: SCHEMA,
                    values: VALUES,
                })).toEqual(TABS_RESULT);
        });

        it('Change tabs info after selecting control field value over existing value', () => {
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
                            json: { type: controlType.json, tab_name: 'json_tab' },
                        },
                    },
                },
            };

            const TABS_INFO = {
                [ROOT_KEY]: {
                    tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                variable_field: {
                    tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                    currentTab: DEFAULT_TAB_NAME,
                },
            };

            const TABS_RESULT: Dict<IFormTabs> = {
                [ROOT_KEY]: {
                    tabs: [{ name: DEFAULT_TAB_NAME, countItems: 1 }],
                    currentTab: DEFAULT_TAB_NAME,
                },
                variable_field: {
                    tabs: [{ name: 'json_tab', countItems: 1 }],
                    currentTab: 'json_tab',
                },
            };

            const VALUES = {
                variable_field: {
                    control_variants_fields: 'hard',
                },
            };

            expect(FormConstructorTabsWorker.changeControlVariableTabInfo(
                {
                    tabsInfo: TABS_INFO,
                    schema: SCHEMA,
                    values: VALUES,
                })).toEqual(TABS_RESULT);
        });

    });
});
