/**
 * @typedef { import('../helpers/types').CleanerHandler } CleanerHandler
 */

/**
 * Изменение даты попадания в овердрафт
 *
 * @type {CleanerHandler}
 */
async function unlockOverdraftUser(bro, days) {
    const overdraftDate = new Date();
    overdraftDate.setDate(overdraftDate.getDate() - days);
    const day = overdraftDate.getDate();
    const month = overdraftDate.getMonth() + 1;
    const year = overdraftDate.getFullYear();
    const formattedOverdraftDate = `${year}-${(month < 10 ? '0' : '') + month}-${(day < 10 ? '0' : '') + day}`;

    // дергаем ручку, которая изменит дату попадания в овердрафт
    await bro.execute((overdraftDate) => {
        window.rawFetchModel('do-set-user-overdraft-date',
            {
                overdraftDate
            });
    }, formattedOverdraftDate);
}

module.exports = {
    hard: (bro) => unlockOverdraftUser(bro, 15),
    lite: (bro) => unlockOverdraftUser(bro, 1),
};
