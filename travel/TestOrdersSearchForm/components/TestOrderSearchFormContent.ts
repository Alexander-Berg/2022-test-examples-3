import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';
import {Input} from 'components/Input';
import {Loader} from 'components/Loader';
import {Button} from 'components/Button';

export default class TestOrderSearchFormContent extends Component {
    readonly title: Component;
    readonly enterAccountLink: TestLink;
    readonly prettyOrderIdInput: Input;
    readonly userPhoneOrEmailInput: Input;
    readonly loader: Loader;
    readonly searchButton: Button;
    readonly notFoundError: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.enterAccountLink = new TestLink(browser, {
            parent: this.qa,
            current: 'enterAccountLink',
        });

        this.prettyOrderIdInput = new Input(browser, {
            parent: this.qa,
            current: 'prettyOrderIdInput',
        });

        this.userPhoneOrEmailInput = new Input(browser, {
            parent: this.qa,
            current: 'userPhoneOrEmailInput',
        });

        this.loader = new Loader(browser, {parent: this.qa, current: 'loader'});

        this.searchButton = new Button(browser, {
            parent: this.qa,
            current: 'searchButton',
        });

        this.notFoundError = new Component(browser, {
            parent: this.qa,
            current: 'notFoundError',
        });
    }
}
