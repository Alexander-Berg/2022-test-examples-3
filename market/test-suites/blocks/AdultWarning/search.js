import {makeSuite, makeCase} from 'ginny';
import _ from 'lodash';
import dayjs from 'dayjs';
import {getLastReportRequestParams} from './helpers';

const COOKIE_EXPIRATION_IN_YEARS = 10;

/**
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 */
export default makeSuite('Подтверждение возраста на поисковой выдаче', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'По умолчанию': {
            'присутствует на странице': makeCase({
                id: 'marketfront-859',
                issue: 'MARKETVERSTKA-32600',
                async test() {
                    const isVisible = await this.adultConfirmationPopup.isVisible();

                    return this.expect(isVisible).to.be.equal(true, 'Информер присутствует на странице');
                },
            }),
        },
        'При нажатии на кнопку «Да»': {
            'параметр adult отправляется в репорт': makeCase({
                id: 'marketfront-3075',
                issue: 'MARKETVERSTKA-32589',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const {adult} = await getLastReportRequestParams(this);

                    return this.expect(Number(adult)).to.be.equal(1, 'параметр adult присутствует в запросе');
                },
            }),
            'устанвливается несессионная кука adult': makeCase({
                id: 'marketfront-3078',
                issue: 'MARKETVERSTKA-32579',
                async test() {
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const cookie = await this.browser.cookie();
                    const adultCookie = _.find(cookie.value, {name: 'adult'});
                    const {expiry} = adultCookie;

                    const expectedValue = dayjs().add(COOKIE_EXPIRATION_IN_YEARS, 'year').startOf('year');
                    const currentValue = dayjs.unix(expiry).startOf('year');
                    const isEqual = expectedValue.diff(currentValue, 'year') === 0;

                    return this.expect(isEqual).to.be.equal(
                        true,
                        `кука истекает через ${COOKIE_EXPIRATION_IN_YEARS} лет`
                    );
                },
            }),
            'отображается дисклеймер «возрастное ограничение 18+»': makeCase({
                id: 'marketfront-861',
                issue: 'MARKETVERSTKA-24992',
                async test() {
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const isAgeWarningDisclaimerExists =
                        await this.ageWarning.isVisible().then(items => items[0] && items[1]);

                    return this.expect(isAgeWarningDisclaimerExists).to.be.equal(true, 'Дисклеймер отображается');
                },
            }),
        },
        'При нажатии на кнопку «Нет»': {
            'параметр adult не отправляется в репорт': makeCase({
                id: 'marketfront-3071',
                issue: 'MARKETVERSTKA-32576',
                async test() {
                    await this.adultConfirmationPopup.clickDecline();
                    await this.browser.yaWaitForPageReady();

                    const {adult} = await getLastReportRequestParams(this);

                    return this.expect(adult).to.be.equal(undefined, 'параметр adult отсутствует в запросе');
                },
            }),
            'устанавливается сессионная кука adult со значением CHILD': makeCase({
                id: 'marketfront-3077',
                issue: 'MARKETVERSTKA-32578',
                async test() {
                    await this.adultConfirmationPopup.clickDecline();
                    await this.browser.yaWaitForPageReady();

                    const cookie = await this.browser.cookie();
                    const adultCookie = _.find(cookie.value, {name: 'adult'});
                    const adultCookieValue = String(adultCookie.value).split(':')[2];

                    return this.expect(adultCookieValue).to.be.equal(
                        'CHILD',
                        'кука сессионная'
                    );
                },
            }),
        },
    },
});
