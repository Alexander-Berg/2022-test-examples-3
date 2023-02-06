import {Component} from 'components/Component';
import {Button} from 'components/Button';
import {ComponentArray} from 'components/ComponentArray';

export default class TestMessageList extends Component {
    sendButton: Button;
    messages: ComponentArray;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.sendButton = new Button(browser, {
            parent: this.qa,
            current: 'sendButton',
        });

        this.messages = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'message',
            },
            Component,
        );
    }
}
