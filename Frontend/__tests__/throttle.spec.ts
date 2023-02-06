import { throttle } from '../throttle';

type TT = jest.Mock<void> & {
    cancel: () => void;
    checker: (expectedRun: number, expectedExecute: number, done: () => void) => () => void;
}

function getRunner({ delay = 100, delayedLastCall = undefined } = {}) {
    const mockFn = jest.fn();
    const runnerFn = jest.fn();

    const throttledFn = throttle(mockFn,
        delay,
        delayedLastCall !== undefined ? { delayedLastCall } : undefined
    );
    const testFn = runnerFn.mockImplementation(throttledFn) as TT;

    testFn.cancel = throttledFn.cancel;

    testFn.checker = (expectedRun: number, expectedExecute: number, done: () => void) => () => {
        expect(mockFn).toHaveBeenCalledTimes(expectedExecute);
        expect(testFn).toHaveBeenCalledTimes(expectedRun);

        done();
    };

    return testFn;
}

/**
 * Фабрика для функций имитации течения времени
 */
function getTimeMachine() {
    const base = Date.now();
    let timeLine = 0;

    jest.useFakeTimers();

    // Мокаем Date.now, чтобы возвращало всегда базовую величину + управляемый сдвиг (задается возвращаемой функцией)
    jest.spyOn(Date, 'now').mockImplementation(() => base + timeLine);

    return (a: number, finish: boolean = false) => {
        // сдвигаем время
        timeLine = a;

        if (finish) {
            // выполняем все таймеры
            jest.runAllTimers();

            // Восстанавливаем Date.now
            jest.spyOn(Date, 'now').mockRestore();
        } else {
            // выполняем все таймеры запланированные к текущему моменту времени
            jest.runTimersToTime(a);
        }
    };
}

let updateTime: ReturnType<typeof getTimeMachine>;

describe('throttle', () => {
    beforeEach(() => {
        updateTime = getTimeMachine();
    });

    it('Первый вызов выполняется синхронно', done => {
        const runner = getRunner();

        runner();

        runner.checker(1, 1, done)();
    });

    it('Синхронные вызовы троттлятся', done => {
        const runner = getRunner();

        runner();
        runner();
        runner();
        runner();
        runner();

        runner.checker(5, 1, done)();
    });

    it('Асинхронные вызовы троттлятся', done => {
        const runner = getRunner();

        setTimeout(runner, 20);
        setTimeout(runner, 30);
        setTimeout(runner, 30);
        setTimeout(runner, 40);
        setTimeout(runner, 50);

        setTimeout(runner.checker(5, 1, done), 500);

        jest.runAllTimers();
    });

    it('Хвостовой вызов', done => {
        const runner = getRunner({ delayedLastCall: true });

        setTimeout(runner, 20);
        setTimeout(runner, 30);
        setTimeout(runner, 30);
        setTimeout(runner, 40);
        setTimeout(runner, 50);

        setTimeout(runner.checker(5, 2, done), 500);

        jest.runAllTimers();
    });

    it('Вызовы троттлятся в рамках указанного периода', done => {
        const runner = getRunner();

        setTimeout(runner, 20); // Вызов
        setTimeout(runner, 30); // Скипнется
        setTimeout(runner, 35); // Скипнется
        setTimeout(runner, 40); // Скипнется

        setTimeout(runner, 140); // Выполнится

        setTimeout(runner.checker(5, 2, done), 500);

        // Запланировали всё, а теперь имитируем ход времени по дискретным точкам
        updateTime(21);
        updateTime(31);
        updateTime(41);
        updateTime(150);

        updateTime(600, true);
    });

    it('Хвостовой вызов выполняется через таймаут после предыдущего успешного', done => {
        const runner = getRunner({ delayedLastCall: true });

        setTimeout(runner, 20); // Вызов
        setTimeout(runner, 30); // Скипнется, но запланируется
        setTimeout(runner, 30); // Скипнется
        setTimeout(runner, 110); // Скипнется
        setTimeout(runner, 150); // До этого уже выполнится отложенный запуск, поэтому это запланируется

        setTimeout(runner.checker(5, 3, done), 500);

        // Запланировали всё, а теперь имитируем ход времени по дискретным точкам
        updateTime(21);
        updateTime(31);
        updateTime(41);
        updateTime(111);
        updateTime(151);

        updateTime(600, true);
    });

    it('Работает отмена отложенного выполнения', done => {
        const runner = getRunner({ delayedLastCall: true });

        runner(); // исполнится
        runner(); // запланируется
        runner(); // скипнется

        runner.cancel(); // отменяем вызов

        setTimeout(runner.checker(3, 1, done), 500);

        jest.runAllTimers();
    });
});
