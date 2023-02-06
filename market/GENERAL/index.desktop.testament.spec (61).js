import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {TEST_CASES} from './testCases/';
import {baseMockFunctionality, TelemostResolversMockConstructor} from './common/mockFunctionality';

const WIDGET_PATH = '@self/root/src/widgets/content/Telemost/TelemostButton';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await jestLayer.runCode(() => {
        // eslint-disable-next-line global-require
        const actions = require('@self/root/src/actions/popups');
        jest.spyOn(actions, 'showUserPopup')
            .mockImplementation(props => actions.requestShowPopup(props));
    }, []);
    await jestLayer.runCode(baseMockFunctionality, []);
});

afterAll(() => {
    mirror.destroy();
    jest.clearAllMocks();
});

const createTestSuites = ({
    testCase,
    widgetPath,
    mockFunction,
}) => {
    if (testCase.describe) {
        // eslint-disable-next-line jest/valid-describe
        describe(testCase.describe.name, () => {
            testCase.describe.suites.map(suite => createTestSuites({
                testCase: suite,
                widgetPath,
                mockFunction,
            }));
        });
    } else {
        it(testCase.caseName, async () => {
            await testCase.method({
                widgetPath,
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockFunction,
                mockData: testCase.mockData,
                widgetParams: testCase.widgetParams,
            });
        });
    }
};

describe('TelemostButton', () => {
    describe('Спросить у консультанта', () => {
        TEST_CASES.map(testCase => createTestSuites({
            testCase,
            widgetPath: WIDGET_PATH,
            mockFunction: TelemostResolversMockConstructor,
        }));
    });
});
