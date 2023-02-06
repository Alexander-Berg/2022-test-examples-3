import PropTypes from 'prop-types';
import {decl} from '../../../../../i-bem/i-bem.react';

export default decl({
    block: 'test',
    elem: 'clicker',

    willInit() {
        this.__base(...arguments);

        this._onClick = this._onClick.bind(this);
    },

    attrs() {
        return {onClick: this._onClick};
    },

    _onClick() {
        this.context.onClick();
    }
}, {
    contextTypes: {
        onClick: PropTypes.func
    }
});
