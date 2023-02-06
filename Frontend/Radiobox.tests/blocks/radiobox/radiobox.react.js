import React from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'radiobox'
},
    RadioBox => class Wrapper extends React.Component {
        constructor(props, context) {
            super(props);

            this.state = {value: this.props.value};
            this.onChange = this.onChange.bind(this);
        }

        onChange({target}) {
            this.setState({value: target.value});
        }

        render() {
            return (<RadioBox {...this.props}
                onChange={this.onChange}
                value={this.state.value} />);
        }
    }
);
