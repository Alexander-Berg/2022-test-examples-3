import {makeSuite, makeCase} from 'ginny';
import {getLastReportRequestParams} from '@self/platform/spec/hermione/helpers/getLastReportRequestParams';

/**
 * @param {PageObject.ageConfirmation} ageConfirmation
 */
export default makeSuite('Подтверждение возраста — кнопка «да»', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'При нажатии на кнопку «да»': {
            'параметр adult отправляется в репорт': makeCase({
                async test() {
                    await this.ageConfirmation.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const {reportPlace} = this.params;

                    // прверка зависит от порядка запросов
                    //      - не ясно, распространяется ли на все запросы КМ правило (на моем тикете не касающемся этой темы она поломалась)
                    // дополнил фильтрацию по platform: 'touch', чтобы отсечь запросы ручек рутовых,
                    //      в которых явно не использовалась стратегия useAdult в resolveCommonReportParams
                    const {adult} = await getLastReportRequestParams(this, reportPlace || 'prime', {
                        platform: 'touch',
                    });

                    return this.expect(parseInt(adult, 10)).to.be.equal(1, 'Параметр adult = 1 отправляется в репорт');
                },
            }),
        },
    },
});
