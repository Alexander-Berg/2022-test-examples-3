const {File, ScreenshotFile, TestdataFile} = require("../../../src");

const path = require('path');
const fs = require('fs');
const { getShortMD5 } = require('@yandex-int/short-md5');

module.exports = {
    silent: true,
    plugins: {
        [path.resolve(process.cwd(), 'test/tide-experimenter/fixtures/tide-spy-plugin.js')]: {},
        'tide-usage-stats': {
            enabled: false,
        },
        'tide-experimenter': {
            enabled: true,
            targetExpFilePath: (originalFile, expName) => {
                if (
                    (originalFile instanceof TestdataFile || originalFile instanceof ScreenshotFile) &&
                    !originalFile.filePath
                ) {
                    throw new Error('Asset files must have a valid filePath');
                }

                let relativePath = path.relative(
                    process.cwd(),
                    originalFile.filePath ? originalFile.filePath :
                    path.resolve(
                        `./features/NewFeature/NewFeature.test/test@common.${originalFile.fileExt}`,
                    ),
                );
                // Новый стек
                if (relativePath.startsWith('src/')) {
                    return path.join('output/src/experiments', expName, relativePath.slice(4));
                }
                // Старый стек
                return path.join('output/experiments', expName, relativePath);
            },
        }
    },
};
