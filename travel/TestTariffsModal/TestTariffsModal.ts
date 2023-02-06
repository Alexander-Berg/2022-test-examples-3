import {TestModalWithBackButton} from 'helpers/project/common/components';
import TestVariantsTariffTable from 'helpers/project/avia/components/TestTariffsModal/components/TestVariantsTariffTable';

import {Component} from 'components/Component';
import {TestModal} from 'components/TestModal';
import {TestLink} from 'components/TestLink';

export default class TestTariffsModal extends Component {
    /**
     * Модальное окно с кнопкой "Назад"
     * используется в тачах
     */
    modalWithBackButton: TestModalWithBackButton | null;

    /**
     * Модальное окно с крестиком
     * используется в десктопе
     */
    modal: TestModal | null;

    /**
     * Ссылка на полные правила применения тарифов
     */
    fullFareRulesLink: TestLink;

    /**
     * Таблица с тарифами
     */
    table: TestVariantsTariffTable;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'tariffsModal');

        this.fullFareRulesLink = new TestLink(browser, {
            parent: this.qa,
            current: 'fullFareRulesLink',
        });

        this.table = new TestVariantsTariffTable(browser, {
            parent: this.qa,
            current: 'table',
        });

        if (this.isTouch) {
            this.modalWithBackButton = new TestModalWithBackButton(browser, {
                parent: this.qa,
                current: 'modalWithBackButton',
            });
            this.modal = null;
        } else {
            this.modalWithBackButton = null;
            this.modal = new TestModal(browser, {
                parent: this.qa,
                current: 'modal',
            });
        }
    }
}
