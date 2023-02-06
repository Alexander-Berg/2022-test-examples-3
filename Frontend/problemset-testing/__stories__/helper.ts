import {
    CheckerType,
    IProblem,
    IPropertyLimits,
    ProblemType,
    StandardCheckerType,
    TestFileType,
} from 'common/types/problem';

import problemData from 'client/components/problem-settings/defaults';

function testSetsGenerator() {
    return [
        {
            id: 1,
            name: 'samples',
            sample: true,
            tests: new Array(generateRandom(100)),
            inputFilePattern: 'tests/{01-03}',
            outputFilePattern: '*.a',
        },
        {
            id: 2,
            name: 'all tests',
            sample: false,
            tests: new Array(generateRandom(100)),
            inputFilePattern: 'tests/{01-03}',
            outputFilePattern: '*.a',
        },
        {
            id: 3,
            name: 'tests',
            sample: false,
            tests: new Array(generateRandom(100)),
            inputFilePattern: 'tests/{01-03}',
            outputFilePattern: '*.a',
        },
        {
            id: 4,
            name: 'other tests',
            sample: false,
            tests: new Array(generateRandom(100)),
            inputFilePattern: 'tests/{01-03}',
            outputFilePattern: '*.a',
        },
        {
            id: 5,
            name: '1',
            sample: false,
            tests: new Array(generateRandom(100)),
            inputFilePattern: 'tests/{01-03}',
            outputFilePattern: '*.a',
        },
    ];
}

function generateRandom(max: number = 1000) {
    return Math.round(Math.random() * max);
}

function limitGenerator(): IPropertyLimits {
    return {
        common: {
            idlenessLimit: generateRandom(),
            memoryLimit: generateRandom(),
            timeLimit: generateRandom(),
            outputLimit: generateRandom(),
        },
        custom: [
            {
                compilers: ['openjdk7_x64', 'openjdk7_x32', 'java7_x32', 'java8'],
                limit: {
                    idlenessLimit: generateRandom(),
                    memoryLimit: generateRandom(),
                    timeLimit: generateRandom(),
                    outputLimit: generateRandom(),
                },
            },
        ],
        files: {
            files: [],
        },
    };
}

export function* problemsGenerator(count: number = 1, isEmpty: boolean = false) {
    let i: number = 0;
    const types: ProblemType[] = [
        ProblemType.INTERACTIVE_PROBLEM,
        ProblemType.TEST_PROBLEM,
        ProblemType.TEXT_ANSWER_PROBLEM,
        ProblemType.PROBLEM_WITH_CHECKER,
    ];
    const amountOfTypes: number = types.length;
    while (i++ < count) {
        const id = i.toString();
        yield {
            ...problemData,
            id,
            shortName: 'max-sum',
            name: `Максимальная сумма с обменом${id}`,
            names: { ru: `Максимальная сумма с обменом${id}`, en: `English${id}` },
            statements: [],
            title: id,
            type: types[Math.floor(i % amountOfTypes)],
            testSets: isEmpty ? [] : testSetsGenerator(),
            createdAt: 1562336864195,
            modifiedAt: 1562336864195,
            runtimeLimit: limitGenerator(),
            compileLimit: limitGenerator(),
            postprocessorLimit: {
                custom: null,
                common: null,
                files: {
                    files: [],
                },
            },
            fileSettings: {
                inputFileName: 'input.txt',
                outputFileName: 'output.txt',
                allowReadStdin: i % 2 === 0,
                allowWriteStdout: i % 3 === 0,
                allowFileCreation: false,
                testFileType: TestFileType.BINARY,
                maxSourceSize: 1024,
            },
            owner: {
                id: 42,
                name: 'vasya',
                login: 'vasya',
            },
            problemSets: [],
            details: {},
            validators: [],
            solutions: [],
            checkerSettings: {
                type: CheckerType.STANDARD,
                limits: {
                    timeLimitMillis: 1234567,
                    idlenessLimitMillis: 1234567,
                    memoryLimit: 12345678,
                    outputLimit: 1234567,
                },
                details: {
                    scoring: false,
                    checkerFiles: [],
                    checkerId: StandardCheckerType.CMP_FILE,
                },
            },
        } as IProblem;
    }
}
