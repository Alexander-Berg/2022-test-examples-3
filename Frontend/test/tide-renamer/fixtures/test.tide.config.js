const path = require('path');
const fs = require('fs');
const { getShortMD5 } = require('@yandex-int/short-md5');

module.exports = {
    silent: true,
    parsers: {
        'tide-screenshot-parser': {
            dirPath: (test, baseDirPath) => {
                const hermioneFilePath = test.files.hermione.filePath;
                const fullTitle = test.fullTitle();

                try {
                    const possiblePath = path.join(baseDirPath, getShortMD5(`${hermioneFilePath} ${fullTitle}`, 7));

                    fs.accessSync(possiblePath);

                    return possiblePath;
                } catch {
                    return path.join(baseDirPath, getShortMD5(fullTitle, 7));
                }
            }
        },
        'tide-testdata-parser': {
            dirPath: (test, baseDirPath) => {
                const hermioneFilePath = test.files.hermione.filePath;
                const fullTitle = test.fullTitle();

                try {
                    const possiblePath = path.join(baseDirPath, getShortMD5(`${hermioneFilePath} ${fullTitle}`, 7));

                    fs.accessSync(possiblePath);

                    return possiblePath;
                } catch {
                    return path.join(baseDirPath, getShortMD5(fullTitle, 7));
                }
            },
            filePath: (testdataFile, dirPath) => {
                if (!testdataFile.data.meta) {
                    console.log(testdataFile.original.filePath);
                }
                const fileHash = `${getShortMD5(testdataFile.data.meta.url, 16)}.json.gz`;
                const browser = path.basename(path.dirname(testdataFile.filePath));

                return path.join(dirPath, browser, fileHash);
            },
        },
    },
    plugins: {
        'tide-renamer': {
            enabled: true,
        },
        [path.resolve('test/tide-renamer/fixtures/tide-spy-plugin.js')]: {},
        'tide-usage-stats': {
            enabled: false,
        },
    },
};
