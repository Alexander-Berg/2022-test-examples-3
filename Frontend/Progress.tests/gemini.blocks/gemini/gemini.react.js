import React from 'react';
import {decl} from '../../../../i-bem/i-bem.react';
import Progress from 'b:progress';

export default decl({
    block: 'gemini',
    willInit() {
        this.state = {
            count: 0
        };

        this.onClick = this.onClick.bind(this);
    },
    attrs() {
        return {
            onClick: this.onClick
        };
    },
    onClick() {
        this.setState({count: (this.state.count + 1) % 3});
    },
    content() {
        return <Progress value={this.state.count / 2} />;
    }
});
