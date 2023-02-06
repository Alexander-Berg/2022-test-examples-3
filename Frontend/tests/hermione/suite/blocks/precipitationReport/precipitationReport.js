const selectors = require('../../../page-objects');

describe('Блоки', function() {
    describe('Главная страница', function() {
        hermione.only.in(/^chromeMobile(LandScape)?$/);
        it.langs.full();
        it('Опрос об осадках', async function() {
            const cookieTarget = 'yw_rr_shwn';
            const {
                index: {
                    MainNavigation, FactContainer, FactCondition, HourlyMain, MapImg, MapAlert,
                    PrecipitationReport, AlertsContainer, MainSliders,
                    PrecipitationReportBtnNo, PrecipitationReportPopup, PrecipitationReportPopupBtn
                },
                header: { White: HeaderWhite }
            } = selectors;

            const invisibleElements = [
                HeaderWhite,
                MainNavigation,
                FactCondition,
                MapImg,
                MapAlert,
                AlertsContainer,
                MainSliders
            ];

            await this.browser
                // мало ли коммунальный браузер окажется на другом домене
                .deleteCookie(cookieTarget)
                .ywOpenPage('/', {
                    lang: this.lang,
                    query: {
                        usemock: 'prec-report'
                    }
                })
                .deleteCookie(cookieTarget)
                .ywWaitForVisible(PrecipitationReport, 5000)
                .ywHidePopup()
                .ywAppendStyle(`${FactContainer} ~ * { display: none }`)
                .assertView('initial', PrecipitationReport);

            await this.browser
                .click(PrecipitationReportBtnNo)
                .ywWaitForVisible(PrecipitationReportPopup, 5000)
                .assertView('popup', HourlyMain, {
                    invisibleElements,
                    allowViewportOverflow: true
                });

            await this.browser
                .click(PrecipitationReportPopupBtn)
                .ywWaitForVisible(PrecipitationReportPopup, 400, undefined, true)
                .assertView('thanks', PrecipitationReport, { screenshotDelay: 400 });
        });
    });
});
