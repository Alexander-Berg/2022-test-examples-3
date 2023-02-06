import { versionComparer } from './versionComparer';

describe('Функция сравнения версий', () => {
    it('Первая больше второй', () => {
        expect(versionComparer('8.0', '4.4.2') > 0).toBeTruthy();
    });

    it('Вторая больше первой', () => {
        expect(versionComparer('1.0.0', '12.1.0') < 0).toBeTruthy();
    });

    it('Равные версии', () => {
        expect(versionComparer('7.0', '7.0.0') === 0).toBeTruthy();
    });

    it('Больше значащих чисел, чем в semver', () => {
        expect(versionComparer('1.2.7.0.1', '1.2.7.2.0') < 0).toBeTruthy();
    });

    it('Сравнение с числом', () => {
        expect(versionComparer('5', '1.2.3') > 0).toBeTruthy();
    });
});
