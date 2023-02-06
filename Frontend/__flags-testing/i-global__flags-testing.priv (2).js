/**
 * Включить флаги экспериментов на все прогоны hermione
 *
 * @param {Object} flags - объект, через который можно включить эксперимент на верстке
 * @param {GlobalData} data
 *
 * @returns {Object|undefined}
 */
defBlock('i-global__flags-testing', function bl(flags, data) {
    if (!data.isTesting) return;

    flags = bl.__base(flags, data);

    return flags;
});
