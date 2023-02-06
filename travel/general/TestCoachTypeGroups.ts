import {ECoachType} from 'helpers/project/trains/types/coachType';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import {TestCoachTypeGroup} from './TestCoachTypeGroup';

export class TestCoachTypeGroups extends Component {
    groups: ComponentArray<TestCoachTypeGroup>;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'coachTypeGroups') {
        super(browser, qa);

        this.groups = new ComponentArray(
            browser,
            {parent: this.qa, current: 'item'},
            TestCoachTypeGroup,
        );
    }

    // Получаем блок по атрибуту не завязываясь на заголовок,
    // т.к. заголовок может отсутствовать (как пример - в поезде один класс мест)
    async getGroupByType(type: ECoachType) {
        return this.groups.find(item => item.qa.includes(type));
    }
}
