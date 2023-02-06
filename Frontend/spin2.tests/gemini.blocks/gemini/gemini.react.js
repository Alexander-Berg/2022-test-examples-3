import React, {Children} from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

import 'b:spin2 m:progress=yes';

export default decl({
    block: 'gemini',
    willInit() {
        this._onClick = this._onClick.bind(this);
        this.state = {
            progress: false
        };
    },
    mods({position}) {
        return {
            ...this.__base(...arguments),
            position
        };
    },
    _onClick() {
        this.setState({progress: !this.state.progress});
    },
    content({children}) {
        return Children.map(children, (child, i) => {
            if(child.type && child.type.displayName === 'button2') {
                return React.cloneElement(child, {
                    ref: button => this._button = button,
                    onClick: this._onClick
                });
            } else { // eslint-disable-line no-else-return
                return React.cloneElement(child, {
                    ref: spin => this._spin = spin,
                    progress: this.state.progress
                });
            }
        });
    }
});
