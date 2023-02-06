const getProblemUrl = id => `/subject/problem/?problem_id=T${id}`;

describe('Карточка задачи', function() {
    describe('Валидация правильного формата ответа', function() {
        it('Показывать попап по ховеру', function() {
            return this.browser
                .yaOpenPage(getProblemUrl(900))
                .moveToObject(PO.Task.InfoIcon(), 5, 5)
                // необходимо, чтобы дать попапу доехать до своего места
                .pause(200)
                .assertView('plain', PO.TaskMistakeInfoPopup());
        });
    });
});
