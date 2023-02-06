/**
 * Возвращает переданное имя ресурса, удаляя часть имени после знака _ .
 * Актуально для файлов, имя которых меняется путем добавления timestamp
 *
 * @param {string} name
 * @returns {string}
 */
function getNameWithoutTimestamps(name) {
    const parts = name.split(/[\_\.]/);
    return `${parts[0]}.${parts[parts.length - 1]}`;
}

module.exports = {
    getNameWithoutTimestamps
};
