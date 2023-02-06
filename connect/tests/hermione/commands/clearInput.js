/*
 * В chrome 77 метод setValue дописывает значение в инпут,
 * вместо того чтобы переписать то, что там было
 * issue хромдрайвера
 * https://wiki.yandex-team.ru/market/verstka/hermionecookbook/#troubleshooting
 *
 * Пример решения
 * https://github.com/webdriverio/webdriverio/issues/3024#issuecomment-542507563
 * Это не совсем честно, не имитируется пользовательский ввод,
 * поэтому сначала очищаем инпут
 */

module.exports = function clearInput() {
    return this
        .execute(selector => {
            const elem = document.querySelector(selector);
            const event = new Event('input', { bubbles: true });
            const previousValue = elem.value;

            elem.value = '';

            // hack React15
            event.simulated = true;

            // hack React16
            let tracker = elem._valueTracker;

            if (tracker) {
                tracker.setValue(previousValue);
            }

            elem.dispatchEvent(event);
            elem.focus();
        }, ...arguments);
};
