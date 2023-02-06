describe('Дежурства', function() {
    describe('Положительные', function() {
        it('1. Заполнение формы и отмена создания дежурства', function() {
            return this.browser
                // открыть страницу настроек дежурств (/services/zanartestservice007/duty/settings/new)
                .openIntranetPage({
                    pathname: '/services/zanartestservice007/duty/settings/new',
                })
                .disableAnimations('*')

                // ждём, когда форма станет доступной (появится дейтпикер в ней)
                .waitForVisible('.DutySchedulesSettings .ta-datepicker', 10000)

                // в поле "Название дежурства" указать "Тестинг"
                .waitForVisible('.DutySchedulesSettings__name .textinput__control') // Если это не написать, следующая строка будет сбоить. Не знаю почему.
                .addValue('.DutySchedulesSettings__name .textinput__control', 'Тестинг')

                // в поле "Код" указать zanartestservice007
                .addValue('.DutySchedulesSettings__slug .textinput__control', 'zanartestservice007')

                // в поле "Роль" указать "DevOps"
                .click('.DutySchedulesSettings__role')
                .addValue('.DutySchedulesSettings__role .textinput__control', 'devops') // TODO: https://st.yandex-team.ru/ABC-6157

                // в поле "Роль на дежурстве" указать "Дежурный"
                .click('.DutySchedulesSettings__role-on-duty')
                .addValue('.DutySchedulesSettings__role-on-duty .textinput__control', 'Дежурный')

                // в поле "Одновременно дежурят" указать "2"
                .customSetValue('.DutySchedulesSettings__persons-count .textinput__control', '2')

                // в поле "Начало дежурства" выбрать дату "17 июля 2019"
                .setTaDatepickerDate('.DutySchedulesSettings .ta-datepicker', 2019, 6, 3, 3)

                // в поле "Длительность" указать "4"
                .customSetValue('.DutySchedulesSettings__duration .textinput__control', 4)

                // в поле "Автоматически подтверждать смены за" указать "14"
                .customSetValue('.DutySchedulesSettings__autoapprove-timedelta .textinput__control', 14)

                // проставить галочку "Не учитывать другие дежурства"
                .setCheckboxVal('.DutySchedulesSettings__consider-other-schedules', true)

                // убрать галочку "Дежурить по праздникам"
                .setCheckboxVal('.DutySchedulesSettings__duty-on-holidays', false)

                // убрать галочку "Дежурить по выходным"
                .setCheckboxVal('.DutySchedulesSettings__duty-on-weekends', false)

                // screenshot: заполненная карточка с дежурством [duty-settings]
                .assertView('duty-settings', '.DutySettings')

                // Нажать "Отмена"
                .click('.DutySettings-Cancel')

                // Проверить редирект на календарь дежурств сервиса
                .yaGetParsedUrl()
                .then(url => assert(url.pathname === '/services/zanartestservice007/duty',
                    'Не произошел редирект на страницу сервиса',
                ));
        });
    });

    describe('Отрицательные', function() {
        it('1. Незаполненные поля', function() {
            return this.browser
                // открыть страницу настроек дежурств (/services/zanartestservice007/duty/settings/new)
                .openIntranetPage({
                    pathname: '/services/zanartestservice007/duty/settings/new',
                })
                .disableAnimations('*')

                // ждём, когда форма станет доступной (появится дейтпикер в ней)
                .waitForVisible('.DutySchedulesSettings .ta-datepicker', 10000)

                .setValue('.DutySchedulesSettings__name .textinput__control', ' ')

                // нажать "Сохранить"
                .click('.DutySettings-Submit')

                // screenshot: попап настройки графика дежурств с ошибкой [duty-settings-error]
                .waitForVisible('.DutySchedulesSettings-Error')
                .assertView('duty-settings-error', '.DutySettings');
        });
    });
});
