// TODO: https://github.com/bem/bem-react-core/issues/130
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'image',
    mix({mix}) {
      return [].concat(this.__base(...arguments), mix).map(mixBlock => {
        return {
            block: mixBlock.block,
            elem: mixBlock.elem,
            mods: mixBlock.elemMods
        };
      });
    }
});
