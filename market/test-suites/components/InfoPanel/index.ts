'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Заглушка c информацией
 * @param {PageObject.InfoPanel} panel
 * @param {Object} params
 * @param {string} [params.title] Заголовок информационной панели
 * @param {string} [params.text] Текст информационной панели
 */
export default makeSuite('Информационная панель', {
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites({
        'при открытии страницы': {
            'отображается корректно': makeCase({
                async test() {
                    await this.allure.runStep('Ожидаем появления информационной панели', () =>
                        this.panel.waitForVisible().should.eventually.equal(true, 'Информационная панель отображается'),
                    );

                    const {title, text} = this.params;

                    if (title) {
                        await this.panel.getTitle().should.eventually.equal(title, 'Заголовок корректный');
                    }

                    if (text) {
                        await this.panel.getText().should.eventually.equal(text, 'Текст корректный');
                    }
                },
            }),
        },
    }),
});
