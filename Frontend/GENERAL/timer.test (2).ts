import MockDate from 'mockdate';

import { startTimer } from './timer';

describe('Хелпер: Таймер', () => {
    it('Создание таймера должно породить нативный таймер', () => {
        jest.useFakeTimers();

        startTimer(jest.fn(), 10);
        expect(setTimeout).toHaveBeenCalledTimes(1);
    });

    it('stop должен удалить таймер', () => {
        jest.useFakeTimers();

        const timer = startTimer(jest.fn(), 10);
        timer.stop();
        expect(clearTimeout).toHaveBeenCalledTimes(1);
    });

    it('resume без stop не должен породить лишний таймер', () => {
        jest.useFakeTimers();

        const timer = startTimer(jest.fn(), 10);
        timer.resume();

        expect(setTimeout).toHaveBeenCalledTimes(1);
    });

    // Мокаем, т.к. время таймера может закончиться до вызова второго resume
    it('Двойной resume не должен породить лишний таймер', () => {
        jest.useFakeTimers();

        const timerTime = 100;
        const pauseTime = 20;
        const now = Date.now();

        MockDate.set(now);
        const timer = startTimer(jest.fn(), timerTime);

        MockDate.set(now + pauseTime);
        timer.stop();
        timer.resume();
        timer.resume();

        expect(setTimeout).toHaveBeenCalledTimes(2); // 1 (таймер заведения) + 1 (таймер продожения)
    });

    it('resume должен продожить отсчитывать с момента остановки', () => {
        jest.useFakeTimers();

        const timerTime = 100;
        const pauseTime = 20;
        const now = Date.now();

        MockDate.set(now);
        const timer = startTimer(jest.fn(), timerTime);

        // прошло немного времени — стопим
        MockDate.set(now + pauseTime);
        timer.stop();

        timer.resume();

        expect(setTimeout).toHaveBeenCalledTimes(2); // 1 (таймер заведения) + 1 (таймер продожения)
        expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), timerTime - pauseTime);

        MockDate.reset();
    });

    it('Двойной stop не дожен повлиять на resume', () => {
        jest.useFakeTimers();

        const timerTime = 100;
        const pauseTime = 20;
        const now = Date.now();

        MockDate.set(now);
        const timer = startTimer(jest.fn(), timerTime);

        // прошло немного времени — стопим
        MockDate.set(now + pauseTime);
        timer.stop();

        // еще прошло немного времени — стопим
        MockDate.set(now + pauseTime + pauseTime);
        timer.stop();

        timer.resume();

        expect(setTimeout).toHaveBeenCalledTimes(2); // 1 (таймер заведения) + 1 (таймер продожения)
        expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), timerTime - pauseTime);

        MockDate.reset();
    });
});
