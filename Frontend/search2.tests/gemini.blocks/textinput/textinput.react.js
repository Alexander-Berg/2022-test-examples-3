import React from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

import Found from 'e:found';
import Filter from 'e:filter';

export default decl({
    block: 'textinput',
    content({found, filter, text}) {
        return [].concat(
            <Found
                key='found'
                text={text}
                offset={this.state.foundOffset}>
                {found}
            </Found>,
            filter ? <Filter>{filter}</Filter> : [],
            ...this.__base(...arguments)
        );
    },

    didMount() {
        const styles = window.getComputedStyle(this._control);
        this._controlQueryLeftOffset =
            parseInt(styles['padding-left'], 10) +
            (parseInt(styles['border-left-width'], 10) || 0);

        this.setState({foundOffset: this._controlQueryLeftOffset});
    }

}, function(TextInput) {
    class Wrapper extends React.Component {
        constructor(props, context) {
            super(props);

            this.state = {text: props.text};

            this.onChange = this.onChange.bind(this);
        }

        onChange(text) {
            this.setState({text});
        }

        render() {
            return <TextInput {...{...this.props, text: this.state.text}} onChange={this.onChange} />;
        }
    }

    return Wrapper;
});
