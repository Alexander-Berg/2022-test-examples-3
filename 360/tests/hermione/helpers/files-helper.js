const util = require('util');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const readdir = util.promisify(fs.readdir);
const stat = util.promisify(fs.stat);
const unlink = util.promisify(fs.unlink);
const rmdir = util.promisify(fs.rmdir);

/**
 * Рассчитывает хеши файлов производя рекурсивный обход переданной директории. Возвращает дерево вложенных объектов, где
 * в качестве ключей используется либо имя папки/файла, либо возврат функции translateKeyFn(имя папки/файла),
 * в качестве листовых значений для каждого файла будет создан соответствующий объект FileHashesData.
 *
 * @typedef FileHashesData
 * @property {string} name - имя файла
 * @property {number} size - размер файла в байтах
 * @property {string} md5
 * @property {string} sha256
 *
 * @param {string} sourcePath
 * @param {Function} [translateKeyFn]
 * @returns {Promise<Object>}
 */
async function generateHashes(sourcePath, translateKeyFn) {
    const stats = await stat(sourcePath);
    if (stats.isFile()) {
        const hashes = await Promise.all([calculateHash(sourcePath, 'md5'), calculateHash(sourcePath, 'sha256')]);
        return {
            name: path.basename(sourcePath),
            size: stats.size,
            md5: hashes[0],
            sha256: hashes[1]
        };
    } else if (stats.isDirectory()) {
        const entries = (await readdir(sourcePath)).filter((entry) => !entry.startsWith('.'));
        const hashSets = await Promise.all(
            entries.map((entry) => generateHashes(path.join(sourcePath, entry), translateKeyFn))
        );

        return hashSets.reduce((accum, hashes, index) => {
            const key = translateKeyFn ? translateKeyFn(entries[index]) : entries[index];
            accum[key] = hashes;
            return accum;
        }, {});
    }
}

/**
 * Получает путь к файлу и название алгоритма, возвращает хеш файла. Алгоритм должен поддерживаться node модулем crypto.
 *
 * @param {string} filePath
 * @param {string} algorithm
 * @returns {Promise}
 */
function calculateHash(filePath, algorithm) {
    return new Promise((resolve, reject) => {
        const hash = crypto.createHash(algorithm);
        const fileStream = fs.createReadStream(filePath, { encoding: 'binary' });
        fileStream.on('data', (data) => hash.update(data));
        fileStream.on('end', () => resolve(hash.digest('hex')));
        fileStream.on('error', (error) => reject(error));
    });
}

/**
 * Рекурсивно удаляет файл или папку по переданному пути.
 *
 * @param {string} deletePath
 * @returns {Promise<*|Promise<any>|Promise<void>>}
 */
async function removeRecursive(deletePath) {
    const stats = await stat(deletePath);
    if (stats.isFile()) {
        return unlink(deletePath);
    } else if (stats.isDirectory()) {
        const files = await readdir(deletePath);
        await Promise.all(files.map((file) => removeRecursive(path.join(deletePath, file))));
        return rmdir(deletePath);
    }
}

module.exports = {
    generateHashes,
    removeRecursive,
    calculateHash
};
