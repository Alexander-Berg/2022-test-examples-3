// Получать текст из children
// для корректного bemjson => jsx

import React, {Children} from 'react';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'tumbler',
    _setLabels(labels) {
        if(Children.count(labels) === 2) {
            const leftLabelText = labels[0].props.children;
            const rightLabelText = labels[1].props.children;

            return {
                        leftLabel: React.cloneElement(labels[0], {text: leftLabelText}),
                        rightLabel: React.cloneElement(labels[1], {text: rightLabelText})
                    };
        } else if(Children.count(labels)) {
            const labelText = labels.props.children;
            const {side} = labels.props;

            return side === 'left' || !side
                    ? {leftLabel: React.cloneElement(labels, {text: labelText})}
                    : {rightLabel: React.cloneElement(labels, {text: labelText})};
        }
        return {};
    }
});
