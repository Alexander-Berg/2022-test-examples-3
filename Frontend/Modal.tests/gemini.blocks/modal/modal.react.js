import PropTypes from 'prop-types';
import React from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'modal',

    attrs({style}) {
        return {style};
    }
}, function(Modal) {
    class Wrapper extends React.Component {
        constructor(props, context) {
            super(props);

            this.state = {visible: false};

            context.registerClickHandler(() => {
                this.setState({visible: !this.state.visible});
            });
        }

        render() {
            return (<Modal {...this.props}
                // TODO: Оторвать после https://st.yandex-team.ru/ISL-4265
                scope={document.querySelector('.b-page__body')}
                visible={this.state.visible} />);
        }
    }

    Wrapper.contextTypes = {
        registerClickHandler: PropTypes.func
    };

    return Wrapper;
});
