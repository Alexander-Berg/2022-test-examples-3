import {makeCase, makeSuite, mergeSuites} from 'ginny';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';

import {PromoEnd} from '@self/root/src/components/BindBonus/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import promoMock from '@self/root/src/spec/hermione/kadavr-mock/loyalty/promos';
import {DEFAULT_DATE_FORMAT} from '@self/root/src/constants/date';

dayjs.extend(customParseFormat);

const promoMockDateFormat = DEFAULT_DATE_FORMAT;
const promoTextDateFormat = 'DD.MM.YYYY';

module.exports = makeSuite('Дата сгорания.', {
    id: 'bluemarket-2802',
    issue: 'BLUEMARKET-6631',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    bonusButton: () => this.createPageObject(Button, {parent: this.bindBonus}),
                    promoEnd: () => this.createPageObject(PromoEnd, {parent: this.bindBonus}),
                });
            },
        },

        makeSuite('В будущем.', {
            defaultParams: {
                promo: promoMock.active,
            },
            story: {
                'Отображается текст "Акция заканчивается + дата"': makeCase({
                    test() {
                        const dateAsText = dayjs(this.params.promo.promoEndDate, promoMockDateFormat)
                            .format(promoTextDateFormat);

                        return this.promoEnd.getText()
                            .should.eventually.be.equal(
                                `Акция заканчивается ${dateAsText}`,
                                `Блок должен содержать текст "акция заканчивается ${dateAsText}"`
                            );
                    },
                }),
            },
        }),

        makeSuite('В прошлом.', {
            defaultParams: {
                promo: promoMock.expired,
            },
            story: {
                'Отображается текст "Акция закончилась"': makeCase({
                    test() {
                        return this.promoEnd.getText()
                            .should.eventually.be.equal(
                                'Акция закончилась',
                                'Блок должен содержать текст "акция закончилась"'
                            );
                    },
                }),
            },
        })
    ),
});
