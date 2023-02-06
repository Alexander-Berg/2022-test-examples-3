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

    // SERP-132336 Оптимизация save_copy_url и Ко
    flags.velocity_sign_saved_copy = 1;

    return flags;
};
