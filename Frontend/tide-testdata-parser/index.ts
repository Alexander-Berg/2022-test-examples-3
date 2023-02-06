import path from 'path';
import _ from 'lodash';

import TestdataFile from './testdata-file';
import TestdataParser from './parser';
import { TOOL } from './constants';
import * as constants from './constants';
import { parseConfig } from './config';
import { TestdataParserConfig } from './types';
import { Tide } from '../../types';

export = (tide: Tide, options: Partial<TestdataParserConfig>): void => {
    const pluginConfig = parseConfig(options);

    if (!pluginConfig.enabled) {
        return;
    }

    tide.addConstants({ [TOOL]: constants });

    tide.on(tide.events.BEFORE_FILES_READ, () => {
        tide.addParser(new TestdataParser({ options: pluginConfig }, pluginConfig));
    });

    // Нужно учитывать, что другие плагины могут менять тесты на это же событие, и порядок изменений не определен.
    tide.prependListener(tide.events.AFTER_FILES_READ, () => {
        const testdataFiles = [...tide.fileCollection.unfilteredFiles].filter(
            (file) => file.tool === TOOL && file.filePath,
        );

        if (!testdataFiles.length) {
            return;
        }

        const testdataFilesByDirHashPath = _.groupBy(testdataFiles, (file) =>
            file.filePath ? path.basename(path.resolve(file.filePath, '../'.repeat(2))) : '',
        );

        tide.testCollection.eachTest((test) => {
            if (!test.filePaths.hermione) {
                return;
            }

            const baseDirPath = pluginConfig.baseDirPath(test.filePaths.hermione);
            const dirPath = pluginConfig.dirPath(test, baseDirPath);

            if (!dirPath) {
                return;
            }

            const dirHashPath = path.basename(dirPath);
            const testdataFilesForTest = testdataFilesByDirHashPath[dirHashPath] as
                | TestdataFile[]
                | undefined;

            if (!testdataFilesForTest) {
                return;
            }

            for (const testdataFile of testdataFilesForTest) {
                testdataFile.addTest(test);
                _.defaults(test.files, { [TOOL]: [] });
                (test.files.testdata as TestdataFile[]).push(testdataFile);
            }
        });

        if (tide.fileCollection.getFilter()) {
            tide.fileCollection.invalidateCaches();
        }
    });
};
