import { Browser } from '@ps-int/ufo-hermione/types';

/**
 * @typedef FilterOptions
 * @property {string | string[] | (name: string) => boolean} byName - Фильтр по имени ресурсы
 * @property {Date | () => Date} beforeDate - Фильтр по дате ресурса (файлы ранее будут удалены)
 */
export interface FilterOptions {
    byName: string | string[] | ((name: string) => boolean);
    beforeDate: Date | (() => Date);
}

/**
 * @typedef CleanerHandlerOptions
 * @property {FilterOptions} filter
 */
export interface CleanerHandlerOptions {
    filter: FilterOptions;
}

/**
 * Функция очистки
 *
 * @callback CleanerHandler
 * @param {Browser} bro
 * @param {CleanerHandlerOptions} options
 * @returns {Promise<any>}
 * @async
 */
export interface CleanerHandler {
    (bro: Browser, options: CleanerHandlerOptions): Promise<any>;
}

/**
 * Конфигурация для очистки артефакта
 *
 * @typedef {Object} ArtifactConfig
 * @property {string} name
 * @property {FilterOptions} filter
 * @property {(bro: Browser) => Promise<any>} [beforeClean] - Вызывается перед началом очистки артефактов
 * @property {(bro: Browser) => Promise<any>} [afterClean] - Вызывается после выполнения очистки артефактов
 */
export interface ArtifactConfig {
    name: string;
    filter: FilterOptions;
    url: string;
    beforeClean: (bro: Browser) => Promise<any>;
    afterClean: (bro: Browser) => Promise<any>;
}

/**
 * Конфигурация для очистки теста
 *
 * @typedef {Object} CleanerConfig
 * @property {string} testId - ID теста
 * @property {string[]} users - Логины пользователей
 * @property {Array<string|ArtifactConfig>} artifacts - Артефакты к очистке
 * @property {FilterOptions} filter
 * @property {(bro: Browser) => Promise<any>} [beforeClean] - Вызывается перед началом очистки теста
 * @property {(bro: Browser) => Promise<any>} [afterClean] - Вызывается после выполнения очистки теста
 */
export interface CleanerConfig {
    testId: string;
    users: string[];
    artifacts: Array<string | ArtifactConfig>;
    filter: FilterOptions;
    beforeClean: (bro: Browser) => Promise<any>;
    afterClean: (bro: Browser) => Promise<any>;
}
