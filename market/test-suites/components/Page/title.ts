'use strict';

import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Заголовок окна', {
    issue: 'VNDFRONT-1883',
    params: {
        user: 'Пользователь',
        title: 'Заголовок',
    },
    story: {
        корректный: makeCase({
            test() {
                return this.browser.allure.runStep(
                    'Проверяем, что заголовок текущей страницы соотвествует ожидаемой',
                    () => this.browser.getTitle().should.eventually.be.equal(this.params.title),
                );
            },
        }),
    },
});
