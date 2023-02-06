import TestdataFile from './testdata-file';
import Test from '../../test';

export interface TestdataParserConfig {
    enabled: boolean;
    /** Путь до директории с данными для данного hermione файла. Например, /some/path/test-data */
    baseDirPath: (hermioneOrDirPath: string | undefined) => string | undefined;
    /** Путь до директории с данными теста. Например, /some/path/test-data/64ac8d */
    dirPath: (test: Test | undefined, baseDirPath: string | undefined) => string | undefined;
    /** Путь до файла данных. Например, /some/path/test-data/64ac8d/some-browser/a3c5[...].json.gz */
    filePath: (
        testdataFile: TestdataFile | undefined,
        dirPath: string | undefined,
    ) => string | undefined;
}
