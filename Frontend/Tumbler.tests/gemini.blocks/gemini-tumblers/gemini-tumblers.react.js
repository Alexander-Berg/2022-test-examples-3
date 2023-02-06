import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'gemini-tumblers',
    mods({multiple, disabled, background, checked, context}) {
        return {
            context,
            disabled,
            background,
            multiple: this.__self.bool2string(multiple)
        };
    }
});
