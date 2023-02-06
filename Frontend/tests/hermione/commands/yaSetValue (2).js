/**
 * Устанавливает value для input, textinput, textarea, используемых в реакт компонентах;
 * Так как Вебдрайвер делает это нативным способом, примерно как $0.value = 'new_value',
 * Реакт не видит этого, потому что не срабатывает события с изменением инпута. Изза этого
 * не происходит очистка инпута перед установкой нового значения, и как следствие в инпут
 * может выставится значение вида old_valuenew_value
 *
 * @param {String} selector - селектор контрола
 * @param {String} val - новое значение инпута
 *
 * @returns {Promise}
 */
module.exports = async function yaSetValue(selector, val) {
    const element = await this.element(selector);

    if (!element) {
        throw new Error(`Не найдено ни одного элемента с селектором "${selector}"`);
    }

    return this.execute(
        function(selector, value) {
            (function() {
                let input = document.querySelector(selector);

                input.scrollIntoView();
                let lastValue = input.value;

                input.value = value;
                let event = new Event('input', { bubbles: true });

                // hack React15
                event.simulated = true;
                // hack React16
                let tracker = input._valueTracker;

                if (tracker) {
                    tracker.setValue(lastValue);
                }

                input.dispatchEvent(event);
            })();
        }, selector, val)
        .click(selector);
};
