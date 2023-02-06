import PropTypes from 'prop-types';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'tooltip',
    mods({testcase}) {
        return {
            ...this.__base(...arguments),
            testcase
        };
    }
}, {
    propTypes: {
        testcase: PropTypes.string
    }
});
