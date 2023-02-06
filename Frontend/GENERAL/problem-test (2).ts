export interface IProblemTest {
    inputPath: string;
    outputPath: string;
    isValid?: boolean;
}

export interface IProblemTestsetBriefInfo {
    id: number;
    name: string;
    sample: boolean;
}

export interface IProblemTestset {
    id: number;
    outputFilePattern: string;
    sample: boolean;
    inputFilePattern: string;
    name: string;
    tests: IProblemTest[];
    testsMatched?: boolean;
}

export enum GenerateTestStatus {
    WAITING = 'WAITING',
    EXECUTED = 'EXECUTED',
    FAILED = 'FAILED',
}

export interface IGenerateTestStatus {
    status: GenerateTestStatus;
    command: string;
    arguments: string[];
    testPath: string;
}

export interface IProblemGeneratedTest extends IProblemTest {
    command: string;
    status?: GenerateTestStatus;
}

export interface IProblemGenerator {
    name: string;
    sourcePath: string;
}

export interface IProblemGeneratorsSettings {
    generators: IProblemGenerator[];
    script: string;
}
