'use strict';

import {makeCase, makeSuite} from 'ginny';

type LocalWindow = typeof window & {__confirm?: Window['confirm'] & {calls: Array<string | undefined>}};

/**
 * Тест на удаление логина для роли
 * @param {PageObject.Tag} tag - тег с удаляемым логином
 * @param {Object} params
 * @param {string} params.login – удаляемый логин
 * @param {string} params.confirmText – текст подтверждения удаления
 */
export default makeSuite('Удаление логина для роли.', {
    feature: 'Управление пользователями',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При удалении логина': {
            'тег скрывается из списка': makeCase({
                async test() {
                    const {login, confirmText} = this.params;

                    await this.browser.allure.runStep('Ожидаем появления тега с логином', async () => {
                        await this.browser.waitUntil(
                            () => this.tag.isVisible(),
                            /*
                             * Запросы выполняются поочередно, поэтому максимальный таймаут n-го запроса
                             * равен n * 5000, где n - кол-во ролей
                             */
                            85000,
                            'Тег с логином не появился',
                        );
                        await this.tag.root
                            .getText()
                            .should.eventually.be.equal(login, 'Тег с выбранным логином появился');
                    });

                    // Подменяем функцию confirm, т.к. webdriver не умеет подтверждать такие диалоги
                    await this.browser.yaExecute(() => {
                        (window as LocalWindow).__confirm = (window as LocalWindow).confirm as LocalWindow['__confirm'];
                        (window as LocalWindow).__confirm!.calls = [];
                        (window as LocalWindow).confirm = function (question) {
                            (window as LocalWindow).__confirm!.calls.push(question); // eslint-disable-line

                            return true;
                        };
                    });

                    await this.tag.remove();

                    await this.browser.allure.runStep('Подтверждаем удаление', () =>
                        // execute на каком-то этапе заворачивает все в {value}
                        this.browser
                            .yaExecute(() => (window as LocalWindow).__confirm!.calls)
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            .then(({value: calls}) => calls[0])
                            .should.eventually.be.equal(confirmText, `Текст подтверждения: "${confirmText}"`),
                    );

                    await this.browser.yaExecute(() => {
                        (window as LocalWindow).confirm = (window as LocalWindow).__confirm!;

                        delete (window as LocalWindow).__confirm;
                    });

                    await this.browser.allure.runStep('Ожидаем скрытия тега с логином', () =>
                        this.browser.waitUntil(
                            async () => {
                                const existing = await this.tag.isExisting();

                                return existing === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Тег скрылся',
                        ),
                    );
                },
            }),
        },
    },
});
