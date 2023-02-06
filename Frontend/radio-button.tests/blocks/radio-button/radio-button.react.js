import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'radio-button',
    attrs() {
        return {
            ...this.__base(...arguments),
            onChange: this.onChange.bind(this)
        };
    },
    content({value: mainValue}) {
        mainValue = this.state.mainValue || mainValue;

        return this.__base(Object.assign({}, ...arguments, {value: mainValue}));
    },
    onChange(e) {
        this.setState({mainValue: e.target.value});
    }
});
