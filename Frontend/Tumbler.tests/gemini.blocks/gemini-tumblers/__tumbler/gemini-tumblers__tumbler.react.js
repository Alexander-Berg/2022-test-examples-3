// Определение этого блока - временное решение для тестирования.
// TODO: Оторвать в рамках https://st.yandex-team.ru/ISL-4123

import React, {Children} from 'react';
import {decl} from '../../../../../i-bem/i-bem.react';

export default decl({
    block: 'gemini-tumblers',
    elem: 'tumbler',
    willInit({children}) {
        this.state = {};

        this.onChange = this.onChange.bind(this);

        Children.forEach(children, (child, i) => {
                this.state[i] = child.props ? child.props.checked : false;
            }
        );
    },
    onChange() {
        for(let key in this.state) {
            this.setState({[`${key}`]: !this.state[key]});
        }
    },
    mods({size}) {
        return {size};
    },
    content({children}) {
        return Children.map(children, (child, i) => {
            if(!child.type || child.type.displayName !== 'tumbler') {
                return child;
            }

            return React.cloneElement(child, {checked: this.state[i], onChange: this.onChange});
        });
    }
});
