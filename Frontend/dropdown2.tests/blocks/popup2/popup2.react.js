import {decl} from '../../../../i-bem/i-bem.react';

// TODO: Оторвать после https://st.yandex-team.ru/ISL-4265
export default decl({
    block: 'popup2',
    didMount() {
        this._scope = document.querySelector('.b-page__body');
        this.__base(...arguments);
        this._scope = document.body;
    },
    willUnmount() {
        this._scope = document.querySelector('.b-page__body');
        this.__base(...arguments);
        this._scope = document.body;
    }
});
