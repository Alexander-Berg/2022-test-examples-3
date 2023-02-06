import React, {Children} from 'react';
import Bem, {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'test',
    willInit() {
        this.state = {
            visible: false
        };
    },
    mods() {
        // "case" - зарезервированное слово
        return {'case': this.props['case']};
    },
    attrs() {
        return {
            onClick: this._onClick.bind(this)
        };
    },
    content({children}) {
        return Children.map(this.props.children, (child, i) => {
                    if(child.props.block === 'test' && child.props.elem === 'anchor') {
                        return <Bem {...child.props} attrs={{ref: ref => this.anc = ref}}>{child.props.children}</Bem>;
                    } else { // eslint-disable-line no-else-return
                        return React.cloneElement(child, {
                            mainOffset: child.props.theme === 'normal' ? 5 : 0,
                            anchor: this.anc,
                            visible: this.state.visible
                        });
                    }
                });
    },
    _onClick() {
        this.setState({visible: true});
    }
});


