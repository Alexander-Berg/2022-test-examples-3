import PropTypes from 'prop-types';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'test',

    attrs({style}) {
        return {style};
    },

    willInit() {
        this.__base(...arguments);

        this._onClick = this._onClick.bind(this);
        this._registerClickHandler = this._registerClickHandler.bind(this);
    },

    _onClick() {
        this.handler();
    },

    _registerClickHandler(handler) {
        this.handler = handler;
    },

    getChildContext() {
        return {
            onClick: this._onClick,
            registerClickHandler: this._registerClickHandler
        };
    }
}, {
    childContextTypes: {
        onClick: PropTypes.func,
        registerClickHandler: PropTypes.func
    }
});
