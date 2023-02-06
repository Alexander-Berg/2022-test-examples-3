/**
 * Класс, описывающий
 *   - ожидание выполнения условия на странице
 *   - действие при невыполнении условия
 *
 * @abstract
 */
module.exports = class Expectation {
    /**
     * @param {Object} browser - Экземпляр браузера. Объект {@link http://webdriver.io/api.html|webdriverio}
     * @param {String} message - Сообщение об ошибке, специфичное для дочернего класса
     */
    constructor(browser, message) {
        this.id = Math.floor(Math.random() * 1e8).toString(16);
        this.browser = browser;
        this.message = message;
        this.hint = 'Попробуйте воспроизвести шаги теста вручную: https://nda.ya.ru/3SdgxW';
    }

    /**
     * Возвращает функцию, которая будет выполнена в контексте бразуера
     * @see {@link http://webdriver.io/api/protocol/execute.html|execute}
     * Возвращаемая функция проверяет произвольное условие: наличие DOM элемента, скрипта, объекта и т.п.
     * В теле функции нужно использовать только ES5 из-за плохой поддержки современного JS в браузерах
     * @returns {function() : Boolean}
     * @abstract
     */
    validateInBrowserContext() {
        return function() {
            throw new Error('Must be implemented by subclass');
        };
    }

    /**
     * Обработчик ошибки при невыполнении условия из {@link Expectation#validateInBrowserContext}.
     *
     * @param {Error} error - Исходная ошибка
     * @throws Если не переопределен в дочернем классе, выбрасывает исходную ошибку с более специфичным описанием
     */
    onFail(error) {
        Expectation.reThrow(error, `${this.message}\n${this.hint}`);
    }

    /**
     * Сериализует метод проверки условия для выполнения в контексте браузера.
     *
     * @param {Object} expectation
     *
     * @returns {String}
     */
    static stringify(expectation) {
        return JSON.stringify({
            id: expectation.id,
            condition: expectation.validateInBrowserContext().toString(),
        });
    }

    /**
     * Подменяет исходное соообщение об ошибке
     *
     * @param {Error} e - Ошибка, возникающая при невыполнении условия из {@link Expectation#validateInBrowserContext}.
     * @param {String} message
     * @throws Выбрасывает ошибку с измененным описанием
     */
    static reThrow(e, message) {
        e.message = message || e.message;
        throw e;
    }
};
