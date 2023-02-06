import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'example-group',
    attrs({attrs}) {
        return {
            ...this.__base(...arguments),
            ...attrs
        };
    }
});
