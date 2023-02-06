import PropTypes from 'prop-types';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'select2',

    attrs({style}) {
        return {...this.__base(...arguments), style};
    },

    getChildContext() {
        return {
            ...this.__base(...arguments),
            cls: this.props.cls
        };
    }
}, {
    childContextTypes: {
        isIE: PropTypes.bool,
        cls: PropTypes.string,
        getMenu: PropTypes.func,
        wasOpened: PropTypes.bool
    }
});
