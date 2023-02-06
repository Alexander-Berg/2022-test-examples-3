/**
 * Включить флаги экспериментов на все прогоны hermione
 *
 * @param {Object} flags - объект, через который можно включить эксперимент на верстке
 * @param {GlobalData} data
 *
 * @returns {Object|undefined}
 */
blocks['i-global__flags-testing'] = function(flags, data) {
    if (!data.isTesting) return;

    flags.under_header_beauty = 1;

    return flags;
};
