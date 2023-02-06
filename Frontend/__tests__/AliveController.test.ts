import { MockHelper } from '@yandex-int/messenger.utils/lib/mocks';
import { AliveController } from '../AliveController';

const PULSE_TIMEOUT = 30 * 1000;
const DEFAULT_INACTIVE_TIMEOUT = 5 * 1000;
const MAX_INACTIVE_TIMEOUT = 2 * 60 * 1000;

function getNextResponseTimeout(timeout: number) {
    return Math.min(timeout * 2, MAX_INACTIVE_TIMEOUT);
}

describe('AliveController', () => {
    let focus = true;
    let aliveController: AliveController;

    beforeAll(() => {
        MockHelper.mock(window.document, 'hasFocus', () => {
            return () => focus;
        });
    });

    afterAll(() => {
        MockHelper.unmock(window.document.hasFocus);
    });

    beforeEach(() => {
        aliveController = new AliveController({
            pulseTimeout: PULSE_TIMEOUT,
            defaultInactiveTimeout: DEFAULT_INACTIVE_TIMEOUT,
            maxInactiveTimeout: MAX_INACTIVE_TIMEOUT,
        });
        jest.useFakeTimers();
    });

    afterEach(() => {
        aliveController.stop();
        jest.useRealTimers();
        focus = true;
    });

    describe('#start', () => {
        it('Должно быть вызвано событие onPulse, запланирован следующий вызов pulse', () => {
            const spy = jest.fn();

            aliveController.onPulse.addListener(spy);
            aliveController.start();

            expect(spy).toBeCalled();
            expect(setTimeout).nthCalledWith(1, expect.any(Function), PULSE_TIMEOUT);
            expect(setTimeout).nthCalledWith(2, expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);

            aliveController.touch();

            jest.runTimersToTime(PULSE_TIMEOUT);
            expect(spy).toBeCalledTimes(2);
            expect(setTimeout).nthCalledWith(3, expect.any(Function), PULSE_TIMEOUT);
            expect(setTimeout).nthCalledWith(4, expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);
        });

        it('Должно быть вызвано событие onPulse, если в обработчик событие остановлен контроллер', () => {
            const spy = jest.fn();

            aliveController.onPulse.addListener((controller) => {
                spy();
                controller.stop();
            });

            aliveController.start();

            expect(spy).toBeCalled();
            expect(setTimeout).nthCalledWith(1, expect.any(Function), PULSE_TIMEOUT);
            expect(clearTimeout).toBeCalled();
        });

        it('Должно быть вызвано событие onPulse, если inactiveTimeout = 0', () => {
            const spy = jest.fn();

            aliveController = new AliveController({
                pulseTimeout: PULSE_TIMEOUT,
                defaultInactiveTimeout: 0,
                maxInactiveTimeout: 0,
            });

            aliveController.onPulse.addListener(spy);

            aliveController.start();

            expect(spy).toBeCalled();
            expect(setTimeout).nthCalledWith(1, expect.any(Function), PULSE_TIMEOUT);
            expect(clearTimeout).toBeCalled();

            aliveController.stop();
        });

        it('Изменение фокуса должно обрабатываться правильно', () => {
            const spy = jest.fn();

            aliveController.onPulse.addListener(spy);
            aliveController.start();

            expect(spy).toBeCalled();
            expect(setTimeout).nthCalledWith(1, expect.any(Function), PULSE_TIMEOUT);
            expect(setTimeout).nthCalledWith(2, expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);

            focus = false;

            aliveController.touch();

            jest.runTimersToTime(PULSE_TIMEOUT);
            expect(spy).toBeCalledTimes(1);
            expect(setTimeout).toBeCalledTimes(3);
            expect(setTimeout).nthCalledWith(3, expect.any(Function), PULSE_TIMEOUT);

            focus = true;

            jest.runTimersToTime(PULSE_TIMEOUT);
            expect(spy).toBeCalledTimes(2);
            expect(setTimeout).nthCalledWith(4, expect.any(Function), PULSE_TIMEOUT);
            expect(setTimeout).nthCalledWith(5, expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);
        });
    });

    describe('#touch', () => {
        it('Вызов touch должен сбросить текущий таймаут на значение по умолчанию', () => {
            const spy = jest.fn();
            aliveController.onTimeout.addListener(spy);

            aliveController.start();

            for (let i = 1; i < 10; i++) {
                jest.runTimersToTime(PULSE_TIMEOUT);
            }

            aliveController.touch();
            jest.runTimersToTime(PULSE_TIMEOUT);

            expect(setTimeout).lastCalledWith(expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);
        });
    });

    describe('#stop', () => {
        it('Таймаут отсутствия активности не должен быть сброшен в значение по умолчанию', () => {
            const spy = jest.fn();
            aliveController.onTimeout.addListener(spy);

            aliveController.start();

            for (let i = 1; i < 10; i++) {
                jest.runTimersToTime(PULSE_TIMEOUT);
            }

            aliveController.stop();
            aliveController.start();

            expect(setTimeout).lastCalledWith(expect.any(Function), MAX_INACTIVE_TIMEOUT);
        });
    });

    describe('#resetCurrentInactiveTimeout', () => {
        it('Таймаут отсутствия активности не должен быть сброшен в значение по умолчанию', () => {
            const spy = jest.fn();
            aliveController.onTimeout.addListener(spy);

            aliveController.start();

            for (let i = 1; i < 10; i++) {
                jest.runTimersToTime(PULSE_TIMEOUT);
            }

            aliveController.stop();
            aliveController.resetCurrentInactiveTimeout();
            aliveController.start();

            expect(setTimeout).lastCalledWith(expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);
        });

        it('Таймаут отсутствия активности должен быть сброшен в значение по умолчанию', () => {
            const spy = jest.fn();
            aliveController.onTimeout.addListener(spy);

            aliveController.start();

            for (let i = 1; i < 10; i++) {
                jest.runTimersToTime(PULSE_TIMEOUT);
            }

            aliveController.stop();
            aliveController.resetCurrentInactiveTimeout();
            aliveController.start();

            expect(setTimeout).lastCalledWith(expect.any(Function), DEFAULT_INACTIVE_TIMEOUT);
        });
    });

    describe('#onTimeout', () => {
        it('onTimeout не должен быть вызван если вкладка не в фокусе', () => {
            const spy = jest.fn();

            focus = false;

            aliveController.onTimeout.addListener(spy);
            aliveController.start();

            jest.runTimersToTime(PULSE_TIMEOUT);

            expect(spy).not.toBeCalled();

            focus = true;

            jest.runTimersToTime(PULSE_TIMEOUT);
            jest.runTimersToTime(PULSE_TIMEOUT);

            expect(spy).toBeCalledTimes(1);
        });

        it('Прогрессивный вызов таймаута', () => {
            let nextResponseTimeout = DEFAULT_INACTIVE_TIMEOUT;
            const spy = jest.fn().mockImplementation(() => {
                nextResponseTimeout = getNextResponseTimeout(nextResponseTimeout);
            });

            const spy2 = jest.fn();
            let k = 0;
            let time = 0;

            aliveController.onTimeout.addListener(spy);
            aliveController.onPulse.addListener(spy2);

            aliveController.start();

            for (let i = 1; i < 20; i++) {
                const currentResponseTimeout = nextResponseTimeout;

                jest.runTimersToTime(PULSE_TIMEOUT);

                expect(spy2).toBeCalledTimes(i + 1);

                time += PULSE_TIMEOUT;

                if (currentResponseTimeout <= time) {
                    k++;
                    time = 0;
                    expect(spy).toBeCalledTimes(k);
                    expect(setTimeout).lastCalledWith(expect.any(Function), nextResponseTimeout);
                }
            }
        });
    });
});
