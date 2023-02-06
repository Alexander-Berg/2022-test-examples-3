import React from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'checkbox'
},
    CheckBox => class Wrapper extends React.Component {
        constructor(props, context) {
            super(props);

            this.state = {checked: this.props.checked};
            this.onChange = this.onChange.bind(this);
        }

        onChange() {
            this.setState({checked: !this.state.checked});
        }

        render() {
            return (<CheckBox {...this.props}
                onChange={this.onChange}
                checked={this.state.checked} />);
        }
    }
);
