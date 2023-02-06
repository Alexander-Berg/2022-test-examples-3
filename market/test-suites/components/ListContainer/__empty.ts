'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок ListContainer без элементов.
 * @param {PageObject.ListContainer} list
 */
export default makeSuite('Пустой список.', {
    environment: 'testing',
    story: {
        'При отсутствии дочерних элементов': {
            'Отображает сообщение "Ничего не найдено"': makeCase({
                async test() {
                    await this.list.waitForLoading();
                    return this.list.root.getText().should.eventually.be.equal('Ничего не найдено');
                },
            }),
        },
    },
});
