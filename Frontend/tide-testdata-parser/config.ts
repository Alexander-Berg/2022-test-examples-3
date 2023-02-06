import path from 'path';
import crypto from 'crypto';
import _ from 'lodash';

import { getShortMD5 } from '@yandex-int/short-md5';
import { TestdataParserConfig } from './types';
import { DIR_NAME } from './constants';
import Test from '../../test';
import TestdataFile from './testdata-file';
import { isFile } from '../../utils/file-system';

const defaultOptions: TestdataParserConfig = {
    enabled: true,
    baseDirPath: (hermioneOrDirPath: string | undefined): string | undefined => {
        if (!hermioneOrDirPath || !isFile(hermioneOrDirPath)) {
            return hermioneOrDirPath;
        }
        return path.resolve(path.dirname(hermioneOrDirPath), DIR_NAME);
    },
    dirPath: (test: Test | undefined, baseDirPath: string | undefined): string | undefined => {
        if (!test || !baseDirPath) {
            return;
        }

        return path.join(baseDirPath, getShortMD5(test.fullTitle(), 7));
    },
    filePath: (file: TestdataFile | undefined, dirPath: string | undefined): string | undefined => {
        if (!file || !file.fileExt || !file.filePath || !dirPath) {
            return file?.filePath;
        }

        const pathParts: string[] = [dirPath];
        const browser = path.basename(path.dirname(file.filePath));

        if (browser) {
            pathParts.push(browser);
        }

        const hash = crypto
            .createHash('sha1')
            .update(JSON.stringify(file.data))
            .digest('hex')
            .slice(0, 16);

        pathParts.push(`${hash}.${file.fileExt}`);

        return path.join(...pathParts);
    },
};

export function parseConfig(options: Partial<TestdataParserConfig>): TestdataParserConfig {
    return _.cloneDeep(_.defaultsDeep(options, defaultOptions));
}
