import {Component} from 'components/Component';
import TestFieldLabel from 'components/TestFieldLabel';

export default class TestPartnerField extends Component {
    /**
     * Используется в таче
     * @private
     */
    private fieldLabel: TestFieldLabel;

    /**
     * Поля в десктопе
     * @private
     */
    private _name: Component;
    private _value: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.fieldLabel = new TestFieldLabel(browser, {
            parent: this.qa,
            current: 'fieldLabel',
        });

        this._name = new Component(browser, {parent: this.qa, current: 'name'});
        this._value = new Component(browser, {
            parent: this.qa,
            current: 'value',
        });
    }

    /**
     * Название поля
     * Работает и для тача и для десктопа
     */
    get name(): Component {
        if (this.isTouch) {
            return this.fieldLabel.label;
        }

        return this._name;
    }

    /**
     * Значение поля
     * Работает и для тача и для десктопа
     */
    get value(): Component {
        if (this.isTouch) {
            return this.fieldLabel.value;
        }

        return this._value;
    }
}
