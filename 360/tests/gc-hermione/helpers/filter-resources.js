/**
 * @typedef { import('./types').FilterOptions } FilterOptions
 */

/**
 * Сравнивает имя на основе заданного фильтра
 *
 * @param {string} item item to compare
 * @param {string | string[] | ((name: string) => boolean)} filterByName
 * @returns {boolean}
 */
function yaFilterByName(item, filterByName) {
    if (!item) {
        return false;
    }
    if (typeof filterByName === 'function') {
        return filterByName(item);
    }
    if (typeof filterByName === 'string') {
        return item.includes(filterByName);
    }
    return filterByName.some((key) => item.includes(key));
}

/**
 * Сравнивает дату на основе заданного фильтра
 *
 * @param {Date} item item to compare
 * @param {Date | (() => Date)} filterBeforeDate
 * @returns {boolean}
 */
function yaFilterByDate(item, filterBeforeDate) {
    if (!item) {
        return false;
    }
    if (typeof filterBeforeDate === 'function') {
        return filterBeforeDate() > item;
    }
    return filterBeforeDate > item;
}

/**
 * Фильтрует список с элементами листинга в соответствии с заданным фильтром
 *
 * @param {any[]} resources
 * @param {FilterOptions} filter
 * @returns {any[]}
 */
async function yaFilterResources(resources, filter) {
    return resources.filter(({ name, mtime, title }) =>
        (
            !filter.byName ||
            yaFilterByName(name, filter.byName) ||
            yaFilterByName(title, filter.byName)
        ) &&
        yaFilterByDate(new Date(mtime) * 1000, filter.beforeDate)
    );
}

module.exports = {
    yaFilterResources,
    yaFilterByDate,
    yaFilterByName,
};
