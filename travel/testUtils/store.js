/**
 * Функция поиска экшена по типу
 * @param {Object[]} actions - список вызванных экшенов
 * @param {string|Symbol} type - тип экшена
 * @return {Object|undefined}
 */
export const findAction = (actions, type) =>
    actions.find(action => action.type === type);
