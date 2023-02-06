import {
    makeSuite,
    prepareSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';

import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import OutletActions from '@self/root/src/widgets/content/OutletInfo/components/OutletActions/__pageObject';
import OutletMap from '@self/root/src/widgets/content/OutletInfo/components/OutletMap/__pageObject';

import authorizedUser from './authorizedUser';
import unauthorizedUser from './unauthorizedUser';
import outletInfo from './outletInfo';
import {nonBrandedOutletMock, openOutletPage} from './helpers';


export default makeSuite('Информация о ПВЗ', {
    environment: 'kadavr',
    feature: 'Страница ПВЗ',
    issue: 'MARKETFRONT-29390',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    outletActions: () => this.createPageObject(OutletActions),
                    primaryButton: () => this.createPageObject(Button, {
                        parent: OutletActions.primaryButton,
                    }),
                    outletMap: () => this.createPageObject(OutletMap),
                });

                await this.browser.yaScenario(this, setReportState, {
                    state: {
                        data: {
                            results: [
                                nonBrandedOutletMock,
                            ],
                            search: {results: []},
                            blueTariffs: deliveryConditionMock,
                        },
                    },
                });
            },

            'После загрузки карты.': {
                'Метка на карте отображается корректно': makeCase({
                    id: 'bluemarket-3937',
                    async test() {
                        await openOutletPage.call(this);

                        await this.outletMap.waitForPlacemarkVisible()
                            .should.eventually.be.equal(
                                true,
                                'Метка на карте должна быть видна'
                            );

                        return this.outletMap.getPlacemarkText()
                            .should.eventually.be.equal(
                                nonBrandedOutletMock.name,
                                'Текст подписи к метке должен соответствовать названию ПВЗ'
                            );
                    },
                }),
            },
        },

        prepareSuite(authorizedUser),
        prepareSuite(unauthorizedUser),
        prepareSuite(outletInfo, {
            hooks: {
                beforeEach() {
                    return openOutletPage.call(this);
                },
            },
        })
    ),
});
