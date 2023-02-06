'use strict';

const PO = require('./Timer.page-object')('desktop');

// Очищает поле ввода, перед тем как выставить новое значение.
const BACKSPACE = '\uE003';
async function setInputValueWithClear(browser, selector, value) {
    const text = await browser.getValue(selector);
    await browser.setValue(selector, BACKSPACE.repeat(text.length));
    await browser.setValue(selector, value);
}

specs('Таймер', function() {
    // eslint-disable-next-line camelcase
    const data_filter = 'timer';
    const text = 'foreverdata';
    const foreverdata = '2950075779';
    const baobabPath = '/$page/$main/$result[@wizard_name="timer"]/timer';

    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text, foreverdata, data_filter }, PO.timer());
        await browser.yaCheckBaobabServerCounter({ path: baobabPath });
        await browser.assertView('not_started', PO.timer());

        await setInputValueWithClear(browser, PO.timer.minutesInput.control(), '');
        await browser.setValue(PO.timer.secondsInput.control(), '1');
        await browser.yaCheckBaobabCounter(
            PO.timer.controls.actionButton(),
            { path: `${baobabPath}/start` },
        );
        await browser.assertView('started', PO.timer());

        // Ждем пока сработает таймер
        await browser.pause(1000);
        await browser.assertView('finished', PO.timer());

        // Отключение таймера
        await browser.yaCheckBaobabCounter(
            PO.timer.controls.actionButton(),
            { path: `${baobabPath}/stop` },
        );
        await browser.assertView('not_started_again', PO.timer());
    });

    it('Редактирование формы', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text, foreverdata, data_filter }, PO.timer());

        await browser.click(PO.timer.minutesInput.control());
        await browser.assertView('focused', PO.timer());

        await setInputValueWithClear(browser, PO.timer.minutesInput.control(), '');
        await browser.assertView('disabled_controls', PO.timer.controls());

        await setInputValueWithClear(browser, PO.timer.hoursInput.control(), '99');
        await setInputValueWithClear(browser, PO.timer.minutesInput.control(), '59');
        await setInputValueWithClear(browser, PO.timer.secondsInput.control(), '59');
        await browser.assertView('filled', PO.timer());

        await browser.click(PO.timer.controls.actionButton());
        await browser.assertView('started', PO.timer());

        // Ждем пока поменяется значение таймера, чтобы потом проверить сброс формы
        await browser.pause(1000);
        await browser.click(PO.timer.controls.defaultButton());
        await browser.yaCheckBaobabCounter(
            PO.timer.controls.clearButton(),
            { path: `${baobabPath}/reset` },
        );
        await browser.assertView('after_reset', PO.timer());
    });

    it('Пауза', async function() {
        const { browser } = this;
        await browser.yaOpenSerp({ text, foreverdata, data_filter }, PO.timer());

        // Пауза
        await browser.click(PO.timer.controls.actionButton());
        await browser.yaCheckBaobabCounter(
            PO.timer.controls.defaultButton(),
            { path: `${baobabPath}/pause` },
        );
        await browser.assertView('paused_controls', PO.timer.controls());

        // Продолжить
        await browser.yaCheckBaobabCounter(
            PO.timer.controls.defaultButton(),
            { path: `${baobabPath}/resume` },
        );
        await browser.assertView('resumed_controls', PO.timer.controls());
    });

    it('Полноэкранный режим', async function() {
        const { browser } = this;
        await browser.yaOpenSerp({ text, foreverdata, data_filter }, PO.timer());

        // Запускаем таймер
        await browser.click(PO.timer.controls.actionButton());
        // Ставим на паузу, чтобы не шли секунды, и не менялся скриншот
        await browser.click(PO.timer.controls.defaultButton());
        // Переходим в полноэкранный режим
        await browser.yaCheckBaobabCounter(
            PO.timer.fullscreenEnterButton(),
            { path: `${baobabPath}/toggle-fullscreen` },
        );
        await browser.assertView('fullscreen', PO.timerFullscreen());

        // Возобновляем таймер по нажатию на <button>пробел</button>
        await browser.yaCheckBaobabCounter(
            PO.timer.controlsFullscreen.right.spaceButton(),
            { path: `${baobabPath}/space-fullscreen` },
        );

        // Ждем пока пропадут кнопки, и появится лого
        await browser.pause(3500);
        await browser.yaShouldNotBeVisible(PO.timer.controlsFullscreen.left());
        await browser.yaShouldNotBeVisible(PO.timer.controlsFullscreen.right());
        await browser.assertView('fullscreen-logo', PO.timer.fullscreenLogo());

        // Двигаем мышью, чтобы появились кнопки управления
        await browser.moveToObject('body', 10, 10);
        await browser.yaShouldBeVisible(PO.timer.controlsFullscreen.left());
        await browser.yaShouldBeVisible(PO.timer.controlsFullscreen.right());
        await browser.yaShouldNotBeVisible(PO.timer.fullscreenLogo());

        // Закрываем полноэкранный режим по нажатию на <button>esc</button>
        await browser.yaCheckBaobabCounter(
            PO.timer.controlsFullscreen.left.escButton(),
            { path: `${baobabPath}/esc-fullscreen` },
        );
        await browser.yaShouldNotExist(PO.timerFullscreen());

        // Проверяем закрытие при нажатии на крестик
        await browser.click(PO.timer.fullscreenEnterButton());
        await browser.yaShouldExist(PO.timerFullscreen());
        await browser.yaCheckBaobabCounter(
            PO.timer.fullscreenExitButton(),
            { path: `${baobabPath}/toggle-fullscreen` },
        );
        await browser.yaShouldNotExist(PO.timerFullscreen());
    });

    it('Полноэкранный режим - горячие клавиши', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ text, foreverdata, data_filter }, PO.timer());
        await browser.click(PO.timer.controls.actionButton());
        await browser.click(PO.timer.fullscreenEnterButton());
        await browser.yaShouldExist(PO.timerFullscreen());

        // Ставим таймер на паузу по нажатию пробела на клавиатуре
        await browser.yaKeyPress('SPACE');
        await browser.assertView('fullscreen-controls-paused', PO.timer.controlsFullscreen());

        // Закрываем полноэкранный режим по нажатию кнопки ESC на клавиатуре
        await browser.yaKeyPress('ESC');
        await browser.yaShouldNotExist(PO.timerFullscreen());
    });
});
