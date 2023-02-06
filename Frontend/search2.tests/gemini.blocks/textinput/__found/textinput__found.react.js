import React from 'react';
import ReactDOM from 'react-dom';
import {decl} from '../../../../../i-bem/i-bem.react';

export default decl({
    block: 'textinput',
    elem: 'found',
    mods({text}) {
        const visibility = text && text === this.initialText
            ? 'visible'
            : '';
        return {visibility};
    },
    attrs({offset = 0}) {
        const width = this._qHolder
            ? Math.round(this._qHolder.getBoundingClientRect().width)
            : 0;
        return {style: {left: (offset + width) + 'px'}};
    },
    content({text, children}) {
        return [
            <span
                key='query-holder'
                ref={q => this._qHolder = ReactDOM.findDOMNode(q)}
                aria-hidden='true'
                className='textinput__query-holder'>
                {text}
            </span>,
            children
        ];
    },

    willInit({text}) {
        this.__base(...arguments);

        this.initialText = text;
    }
});
