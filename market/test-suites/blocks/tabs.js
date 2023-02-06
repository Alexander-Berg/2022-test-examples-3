import {makeSuite, makeCase} from 'ginny';

/**
 * Сьют тестов на блок tabs.
 * Контекст:
 * @param {PageObject.Tabs} tabs
 * @param {Array} params.actualTabs Список заголовков табов.
 */
export default makeSuite('Табы.', {
    feature: 'Попапи',
    params: {
        actualTabs: 'Табы',
        selectedTab: 'Выбранный таб',
    },
    story: {
        'По-умолчанию': {
            'должен отображаться верный выбранный таб': makeCase({
                id: 'marketfront-953',
                test() {
                    return this.tabs
                        .findSelectedTab()
                        .then(text => this.tabs.checkTabIndex(text, this.params.selectedTab || 1))
                        .should.eventually.to.be.equal(true, 'Выбран верный таб');
                },
            }),

            'должен содержать правильный набор': makeCase({
                id: 'marketfront-955',
                test() {
                    const tabs = this.tabs;
                    const {actualTabs} = this.params;

                    const checkTab = index => {
                        if (index < actualTabs.length) {
                            return tabs
                                .checkTabIndex(actualTabs[index], index + 1)
                                .should.eventually.to.be.equal(true, 'Выбран верный таб')
                                .then(() => checkTab(index + 1));
                        }

                        return undefined;
                    };

                    return tabs
                        .checkTabsLength(actualTabs.length)
                        .should.eventually.to.be.equal(true, `Кол-во табов ${actualTabs.length}`)
                        .then(() => checkTab(0));
                },
            }),
        },

        'По клику': {
            'должны переключаться': makeCase({
                id: 'marketfront-965',
                test() {
                    const {tabs} = this;

                    let index = 1;
                    let tabsCount;

                    const checkSelectTab = result => {
                        if (result) {
                            if (!index) {
                                return true;
                            } else if (index === tabsCount + 1) {
                                index = 0;

                                return selectTab(1);
                            }

                            return selectTab(index++);
                        }

                        return false;
                    };

                    const selectTab = currentIndex => tabs
                        .selectTabByIndex(currentIndex)
                        .then(() => tabs.checkSelectedTabByIndex(currentIndex))
                        .then(checkSelectTab);

                    return tabs.menuTabs
                        .then(status => {
                            tabsCount = status.value.length;
                        })
                        .then(() => selectTab(index++))
                        .should.eventually.to.be.equal(true, 'Табы переключаются');
                },
            }),
        },
    },
});
