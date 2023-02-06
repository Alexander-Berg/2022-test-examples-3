'use strict';

import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Заголовок.', {
    environment: 'testing',
    story: {
        'Отображается': makeCase({
            test() {
                return this.title.isVisible().should.eventually.be.equal(true, 'Отображается');
            },
        }),
        'Содержит корректный текст': makeCase({
            params: {
                caption: 'Ожидаемый текст',
            },
            test() {
                return this.title.getText().should.eventually.equal(this.params.caption, 'Заголовок корректный');
            },
        }),
    },
});
