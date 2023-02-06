describe('Турбо-оверлей', () => {
    describe('Утилиты', () => {
        describe('Подмена домена и урла под кликом в омнибоксе', () => {
            let overrideHostUtil: { addOverrideHostEntries: Function };
            beforeEach(() => {
                // Так как модуль переопределяет функцию после первого вызова, нужно запрашивать его несколько раз
                overrideHostUtil = require('../addOverrideHostEntries');
            });

            afterEach(() => {
                jest.resetModules();
            });

            describe('API подмены доступно', () => {
                beforeAll(() => {
                    window.yandex = {
                        publicFeature: {
                            addOverrideHostEntries: jest.fn(),
                        },
                    };
                });

                afterEach(() => {
                    window.yandex.publicFeature.addOverrideHostEntries.mockClear();
                });

                afterAll(() => {
                    delete window.yandex;
                });

                it('Вызывает метод платформы', () => {
                    const overrideData = {
                        keyUrl: 'https://yandex.ru/turbo?text=123',
                        displayUrl: 'https://test.example.com/1',
                        displayHost: 'test.example.com',
                    };

                    overrideHostUtil.addOverrideHostEntries(overrideData);

                    expect(window.yandex.publicFeature.addOverrideHostEntries, 'Метод платформы не был вызван')
                        .toHaveBeenCalledTimes(1);
                    expect(
                        window.yandex.publicFeature.addOverrideHostEntries,
                        'Метод платформы был вызван с неверным набором параметров'
                    ).toBeCalledWith([{
                        keyUrl: 'https://yandex.ru/turbo?text=123',
                        displayUrl: 'https://test.example.com/1',
                        displayHost: 'test.example.com',
                    }]);

                    // Модуль при первом вызове переопределяет экспортируемую функцию,
                    // чтобы не делать каждый раз одинаковые проверки,
                    // поэтому вызовем его еще раз.
                    window.yandex.publicFeature.addOverrideHostEntries.mockClear();

                    overrideHostUtil.addOverrideHostEntries(overrideData);

                    expect(
                        window.yandex.publicFeature.addOverrideHostEntries,
                        'Метод платформы не был вызван второй раз'
                    ).toHaveBeenCalledTimes(1);
                    expect(
                        window.yandex.publicFeature.addOverrideHostEntries,
                        'Метод платформы второй раз был вызван с неверным набором параметров'
                    ).toBeCalledWith([{
                        keyUrl: 'https://yandex.ru/turbo?text=123',
                        displayUrl: 'https://test.example.com/1',
                        displayHost: 'test.example.com',
                    }]);
                });

                it('Вырезает hash из ссылки в keyUrl', () => {
                    const overrideData = {
                        keyUrl: 'https://yandex.ru/turbo?text=123#test',
                        displayUrl: 'https://test.example.com/1',
                        displayHost: 'test.example.com',
                    };

                    overrideHostUtil.addOverrideHostEntries(overrideData);

                    expect(window.yandex.publicFeature.addOverrideHostEntries, 'В урле для keyUrl не был отброшен хэш')
                        .toBeCalledWith([{
                            keyUrl: 'https://yandex.ru/turbo?text=123',
                            displayUrl: 'https://test.example.com/1',
                            displayHost: 'test.example.com',
                        }]);
                });
            });

            describe('API подмены не доступно', () => {
                it('Не падает при вызове', () => {
                    const overrideData = {
                        keyUrl: 'https://yandex.ru/turbo?text=123',
                        displayUrl: 'https://test.example.com/1',
                        displayHost: 'test.example.com',
                    };

                    expect(
                        () => overrideHostUtil.addOverrideHostEntries(overrideData),
                        'Вызов метода привел к исключению'
                    ).not.toThrow();

                    // Модуль при первом вызове переопределяет экспортируемую функцию,
                    // чтобы не делать каждый раз одинаковые проверки,
                    // поэтому вызовем его еще раз.
                    expect(
                        () => overrideHostUtil.addOverrideHostEntries(overrideData),
                        'Второй вызов метода привел к исключению'
                    ).not.toThrow();
                });
            });
        });
    });
});
