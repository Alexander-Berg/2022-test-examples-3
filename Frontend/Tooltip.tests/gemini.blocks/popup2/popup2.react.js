import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'popup2',
    // Прикрепить popup к b-page, а не к body
    // TODO: Оторвать после https://st.yandex-team.ru/ISL-4011
    didMount() {
        this.hasDOMNode = false;
        this._container = document.createElement('div');
        document.getElementsByClassName('b-page__body')[0].appendChild(this._container);
        this.redraw(this.props);
    }
});
