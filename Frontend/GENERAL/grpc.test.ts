import { ApphostContext } from '@yandex-int/frontend-apphost-context';

import { ABTGrpcSplitter, UaasResponse, UaasRestrictions, UaasSplitIds } from './grpc';

import { UsersplitResult, Handler, HandlerComponentValue } from './typings/uaas';

interface HandlerDescriptor {
    componentDescs: ComponentDescriptor[];
    handlerName?: string;
}

interface ComponentDescriptor {
    name: string;
    withAdditionalFlag?: boolean;
    withOtherValues?: boolean;
    customFlags?: boolean;
    customBoolFlags?: HandlerComponentValue['BooleanMap'];
}

interface ResultDescriptor {
    handlerName?: string;
    testIds?: string[];
    handlerDescs?: HandlerDescriptor[];
}

const DEFAULT_SPLITTER_RESULT = {
    version: -1,
    testIds: [],
    flags: {},
    randomUid: null,
    experiments: null,
    boxes: null,
    configs: [],
};

const CONFIG_VERSION = '123456';
const CRYPTED_EXP_BOXES = 'cryptedBoxes';
const DEFAULT_OPTS = { service: 'serviceName', handler: 'SERVICE' };

/**
 * Helper function for building handler mock.
 *
 * @param componentDescs Descriptor for component of uaas handler
 * @param [handlerName] Name of handler to build
 * @returns handler as an item of uaas response handlers
 */
const getHandler = (componentDescs: ComponentDescriptor[], handlerName: string = DEFAULT_OPTS.handler): Handler => {
    const components: NonNullable<Handler['Components']> = {};

    for (const descriptor of componentDescs) {
        const {
            name, withAdditionalFlag = false, withOtherValues = false, customFlags = false, customBoolFlags = {},
        } = descriptor;

        const componentBody: HandlerComponentValue = {};
        components[name] = componentBody;

        if (!customFlags) {
            componentBody.BooleanMap = { bool_flag: !withOtherValues };
            componentBody.FloatMap = { float_flag: withOtherValues ? 7.25 : 3.14 };
            componentBody.IntMap = { int_flag: withOtherValues ? 100 : 10 };
            componentBody.StringListMap = { str_list_flag: {
                StringItem: withOtherValues ? ['4', '5'] : ['1', '2', '3'],
            } };
            componentBody.StringMap = { str_map_flag: withOtherValues ? 'other string' : 'value' };

            if (withAdditionalFlag) {
                componentBody.BooleanMap.bool_flag_2 = false;
            }
        } else {
            componentBody.BooleanMap = customBoolFlags;
        }
    }

    return {
        Name: handlerName,
        Components: components,
    };
};

/**
 * Shorthand for getHandler with custom flags.
 *
 * @param customFlags customBoolFlags from getHandler
 * @returns handler with custom flags.
 */
const getCustomHandler = (customFlags: HandlerComponentValue['BooleanMap']) => (
    getHandler([{ name: 'COMPONENT', customFlags: true, customBoolFlags: customFlags }])
);

/**
 * Helper function for building uaas result mock.
 *
 * @param [options] Descriptor of needed uaas result
 * @returns uaas result
 */
const getUaasResult = ({
    handlerName = DEFAULT_OPTS.handler,
    testIds = ['4036573', '4036565', '4035234'],
    handlerDescs = [{ componentDescs: [{ name: 'COMPONENT' }] }],
}: ResultDescriptor = {}): UsersplitResult => {
    const result: UsersplitResult = {};

    result.RawHandlers = [`{"HANDLER": "${handlerName}", "MAIN": {"CONTEXT": {"COMPONENT": {"FLAG": "VALUE"}}}}`];

    result.TestBucket = testIds.map(
        testId => ({ Testid: testId, Bucket: 1 }),
    );

    result.Handlers = [];
    for (const handlerDesc of handlerDescs) {
        result.Handlers.push(
            getHandler(handlerDesc.componentDescs, handlerDesc.handlerName),
        );
    }

    return result;
};

describe('GrpcSplitter', () => {
    let uaasResponse: UaasResponse;

    beforeEach(() => {
        uaasResponse = {
            FeatureToggles: getUaasResult(),
            Service: DEFAULT_OPTS.service,
            Experiments: [
                getUaasResult({
                    handlerDescs: [{ componentDescs: [{ name: 'COMPONENT_2' }] }],
                }),
                getUaasResult({ handlerName: 'SERVICE_2' }),
                getUaasResult({ handlerName: 'SERVICE_3' }),
            ],
            ConfigVersion: CONFIG_VERSION,
            CryptedExpBoxes: CRYPTED_EXP_BOXES,
        };
    });

    it('should work without crashing if uaasResponse is null or consists of nullable fields', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        const withNull = splitter.parseInfo(null);

        expect(withNull).toEqual(DEFAULT_SPLITTER_RESULT);

        const withNullFields = splitter.parseInfo({
            FeatureToggles: null,
            Service: null,
            Experiments: null,
            ConfigVersion: null,
            CryptedExpBoxes: null,
        });

        expect(withNullFields).toEqual(DEFAULT_SPLITTER_RESULT);
    });

    it('should work without crashing if handlers in uaasResponse is null or consists of nullable fields', () => {
        const nullableHandler: Handler = {
            Name: null,
            Components: null,
        };
        const nullableHandlerComponent: HandlerComponentValue = {
            BooleanMap: null,
            FloatMap: null,
            IntMap: null,
            StringListMap: null,
            StringMap: null,
        };
        const handlerWithNullComp: Handler = { Name: DEFAULT_OPTS.handler, Components: {
            COMPONENT: nullableHandlerComponent,
        } };

        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        const withNullResFields = splitter.parseInfo({
            FeatureToggles: { Handlers: null, RawHandlers: null, TestBucket: null },
            Service: null, Experiments: null, ConfigVersion: null, CryptedExpBoxes: null,
        });

        expect(withNullResFields).toEqual(DEFAULT_SPLITTER_RESULT);

        const withNullableHandler = splitter.parseInfo({
            FeatureToggles: { Handlers: [nullableHandler], RawHandlers: null, TestBucket: null },
            Service: null, Experiments: null, ConfigVersion: null, CryptedExpBoxes: null,
        });

        expect(withNullableHandler).toEqual(DEFAULT_SPLITTER_RESULT);

        const emptyHandler = splitter.parseInfo({
            FeatureToggles: { Handlers: [{ Name: '', Components: {} }], RawHandlers: null, TestBucket: null },
            Service: null, Experiments: null, ConfigVersion: null, CryptedExpBoxes: null,
        });

        expect(emptyHandler).toEqual(DEFAULT_SPLITTER_RESULT);

        const withNullHandlerCompVal = splitter.parseInfo({
            FeatureToggles: { Handlers: [handlerWithNullComp], RawHandlers: null, TestBucket: null },
            Service: null, Experiments: null, ConfigVersion: null, CryptedExpBoxes: null,
        });

        expect(withNullHandlerCompVal).toEqual(DEFAULT_SPLITTER_RESULT);
    });

    it('should extract UaaS version', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        const result = splitter.parseInfo(uaasResponse);

        expect(result.version).toEqual(Number(CONFIG_VERSION));
    });

    it('should extract encrypted exp boxes', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        const result = splitter.parseInfo(uaasResponse);

        expect(result.experiments).toEqual(CRYPTED_EXP_BOXES);
    });

    it('should extract testIds', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);
        uaasResponse.Experiments = [
            getUaasResult({ testIds: ['123', '456'] }),
            getUaasResult({ testIds: [] }),
            getUaasResult({ testIds: ['123'] }),
        ];
        uaasResponse.FeatureToggles = getUaasResult({ testIds: ['789'] });

        const result = splitter.parseInfo(uaasResponse);

        const expectedTestIds = [
            '123', '456', '789',
        ];
        expectedTestIds.forEach(
            testId => expect(result.testIds).toContain(testId),
        );
    });

    it('should collect raw handlers into configs', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);
        uaasResponse.Experiments = [];
        uaasResponse.FeatureToggles = getUaasResult();

        const result = splitter.parseInfo(uaasResponse);

        expect(result.configs).toEqual([
            `{"HANDLER": "${DEFAULT_OPTS.handler}", "MAIN": {"CONTEXT": {"COMPONENT": {"FLAG": "VALUE"}}}}`,
        ]);
    });

    it('should parse flags from feature toggles and experiments at the same time', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        // Build different flags for FeatureToggles & Experiments.
        uaasResponse.FeatureToggles = { Handlers: [
            getCustomHandler({ first_flag: true }),
        ] };
        uaasResponse.Experiments = [{ Handlers: [
            getCustomHandler({ second_flag: false }),
        ] }];

        const result = splitter.parseInfo(uaasResponse);

        expect(result.flags).toEqual({
            first_flag: true,
            second_flag: false,
        });
    });

    it('should parse flags through different components', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        // There is used another component.
        const handlerDescs: HandlerDescriptor[] = [
            { componentDescs: [{ name: 'COMPONENT_3', withAdditionalFlag: true }] },
        ];
        uaasResponse.FeatureToggles = getUaasResult({ handlerDescs });

        const result = splitter.parseInfo(uaasResponse);

        expect(result.flags).toEqual({
            bool_flag: true,
            bool_flag_2: false,
            float_flag: 3.14,
            int_flag: 10,
            str_list_flag: ['1', '2', '3'],
            str_map_flag: 'value',
        });
    });

    it('should filter flags by service', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        uaasResponse.Experiments = [];
        const handlerDescs: HandlerDescriptor[] = [
            // Add additional flag to test filter.
            { handlerName: `${DEFAULT_OPTS.handler}_ANOTHER`, componentDescs: [{ name: 'COMPONENT', withAdditionalFlag: true }] },
            { componentDescs: [{ name: 'COMPONENT' }] },
        ];
        uaasResponse.FeatureToggles = getUaasResult({ handlerDescs });

        const result = splitter.parseInfo(uaasResponse);

        expect(result.flags).not.toHaveProperty('bool_flag_2');
    });

    it('should override feature toggles flags by experiments flags', () => {
        const splitter = new ABTGrpcSplitter(DEFAULT_OPTS);

        const handlerDescs: HandlerDescriptor[] = [
            { componentDescs: [{ name: 'COMPONENT', withOtherValues: true }] },
        ];
        uaasResponse.Experiments = [
            getUaasResult({ handlerDescs }),
        ];

        const result = splitter.parseInfo(uaasResponse);

        expect(result.flags).toEqual({
            bool_flag: false,
            float_flag: 7.25,
            int_flag: 100,
            str_list_flag: ['4', '5'],
            str_map_flag: 'other string',
        });
    });

    it('should override any flags by overrides', () => {
        const overrides = {
            bool_flag: false,
            int_flag: 500,
            float_flag: -0.14,
            str_list_flag: [],
            str_map_flag: 'overridden',
        };

        // `overrides: { ...overrides }` needed for copying.
        const splitter = new ABTGrpcSplitter({ ...DEFAULT_OPTS, overrides: { ...overrides } });

        const result = splitter.parseInfo(uaasResponse);

        expect(result.flags).toEqual(overrides);
    });

    it('overrides can add new flags, even if there are not flags in uaas response', () => {
        const overrides = {
            new_flag: 'string value',
        };

        // `overrides: { ...overrides }` needed for copying.
        const splitter = new ABTGrpcSplitter({ ...DEFAULT_OPTS, overrides: { ...overrides } });

        // Remove other flags.
        uaasResponse.Experiments = null;
        uaasResponse.FeatureToggles = null;

        const result = splitter.parseInfo(uaasResponse);

        expect(result.flags).toEqual(overrides);
    });

    describe('static members', () => {
        let restrictions: UaasRestrictions;
        let splitIds: UaasSplitIds;

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let uaasInputMethods: jest.MockedFunction<any>[];
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        let uaasResponseMethods: jest.MockedFunction<any>[];

        beforeEach(() => {
            restrictions = {
                Service: 'serviceName',
                IsStaff: true,
            };
            splitIds = {
                Icookie: 'someUUID',
            };

            // Prepare mock functions.
            const verify = jest.fn(() => false);
            const finish = jest.fn(() => new Uint8Array());
            const encode = jest.fn(() => ({
                finish,
            }));
            const create = jest.fn(() => {});

            const TUaasInput = {
                verify,
                create,
                encode,
            };

            const toObject = jest.fn(() => {});
            const decode = jest.fn(() => {});

            const TUaasResponse = {
                verify,
                toObject,
                decode,
            };

            jest.resetModules();
            jest.mock('./uaasProtoAdapter', () => ({
                TUaasInput,
                TUaasResponse,
            }));

            // Collect mock functions.
            uaasInputMethods = Object.values(TUaasInput);
            uaasResponseMethods = Object.values(TUaasResponse);
        });

        describe('getUaasInput', () => {
            it('should run without crashing', () => {
                const result = ABTGrpcSplitter.getUaasInput(restrictions, splitIds);

                expect(result).toMatchSnapshot();
            });

            it('should throw error if splitIds are not specified', () => {
                expect(() => (
                    ABTGrpcSplitter.getUaasInput(restrictions, {})
                )).toThrow();
            });

            it('should use catcher for error handling', () => {
                const caller = jest.fn(() => ABTGrpcSplitter.getUaasInput(restrictions, {}, _ => {}));

                caller();

                expect(caller).toReturn();
            });
        });

        describe('getUaasRequest', () => {
            it('should use protobuf methods while running', async done => {
                await ABTGrpcSplitter.getUaasRequest(restrictions, splitIds);

                for (const protobufMethod of uaasInputMethods) {
                    expect(protobufMethod).toHaveBeenCalled();
                }

                done();
            });
        });

        describe('getUaasResponse', () => {
            let apphostContext: ApphostContext;

            beforeEach(() => {
                // Add mock apphostContext.
                apphostContext = {
                    getProtobufItems() {
                        return [
                            Buffer.from([]),
                        ];
                    },
                } as unknown as ApphostContext;
            });

            it('should use protobuf methods while running', async done => {
                await ABTGrpcSplitter.getUaasResponse(apphostContext, DEFAULT_OPTS.service);

                for (const protobufMethod of uaasResponseMethods) {
                    expect(protobufMethod).toHaveBeenCalled();
                }

                done();
            });
        });
    });
});
