import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'b-page',

    attrs({style}) {
        return {...this.__base(...arguments), style};
    }
});
