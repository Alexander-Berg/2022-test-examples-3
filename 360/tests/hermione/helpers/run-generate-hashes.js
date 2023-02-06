/**
 * - Утилита для генерации JSON конфига со списком файлов и их хешами и размерами.
 * - Нагенеренный конфиг затем может используется в тестах совместно с helpers/disk-arrange
 * - Запускается: `node ./tests/hermione/helpers/run-generate-hashes.js <путь к папке с файлами> <путь к json файлу для записи> [-t]`.
 *
 * Параметр -t добавляет преобразование имен ключей (приводит к upper-case, заменяет пробелы, прочерки и точки на подчеркивания).
 *
 * Пример нагнеренного файла:ß
 * {
 *   IMAGE_FILES: {},
 *   TEXT_FILES: {
 *       TEST_TXT: {
 *           "name": "test.txt"
 *           "size": 29,
 *           "md5": "65b656ca65d08b15c786d4a1fe1c3ff3",
 *           "sha256": "31c75ac448081587a1001a63d609c673e19635243585b44ba5354ab8a8dcea0b"
 *       },
 *       TEXT_TXT: {
 *           "name": "text.txt"
 *           "size": 29,
 *           "md5": "b6d429a2a50707be314dcdb8218d560f",
 *           "sha256": "8066ea330fbeee054db19930e50fc4aea8d44218c2c0b9119bb65731c505f408"
 *       }
 *   }
 * }
 */

const fs = require('fs');
const util = require('util');
const process = require('process');
const { generateHashes } = require('./files-helper');
const writeFile = util.promisify(fs.writeFile);

/**
 * @param {string} fileName
 * @returns {string}
 */
function _translateKeyName(fileName) {
    return fileName.replace(/\s|-|\./g, '_').toUpperCase();
}

(async function generateHashesAndWrite() {
    if (process.argv.length < 4) {
        // eslint-disable-next-line no-console
        console.log(`Usage:
        run-generate-hashes sourceDir destinatinFile [-t]
        sourceDir - path to directory of files hashes to be calculeted from
        destionationDir - path to json file calculated hashes to be saved
        -t - translate file names to uppercase and replace spaces, dots and hyphens with underscore`);
        return;
    }

    const sourcePath = process.argv[2];
    const destinationPath = process.argv[3];

    let keyNameFn = null;
    if (process.argv.includes('-t')) {
        keyNameFn = _translateKeyName;
    }

    const hashSets = await generateHashes(sourcePath, keyNameFn);
    const hashSetsStr = JSON.stringify(hashSets, null, 4);
    await writeFile(destinationPath, hashSetsStr);
    // eslint-disable-next-line no-console
    console.log('Files hash sets written: ' + destinationPath);
}());
