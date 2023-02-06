import PropTypes from 'prop-types';
import React from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'popup2'
}, function(SelectPopup) {
    class Wrapper extends React.Component { // eslint-disable-line

        render() {
            return (<SelectPopup {...this.props}
                // TODO: Оторвать после https://st.yandex-team.ru/ISL-4265
                scope={document.querySelector('.b-page__body')}
                cls={this.context.cls + '-popup'} />);
        }
    }

    Wrapper.contextTypes = {
        cls: PropTypes.string
    };

    return Wrapper;
});
