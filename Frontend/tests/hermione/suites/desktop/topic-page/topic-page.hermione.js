describe('Страница топика (темы)', () => {
    const animationDelay = 1500;

    hermione.only.notIn('linux-chrome-ipad', 'нет такой функциональности');
    hermione.skip.in([/./], 'https://st.yandex-team.ru/YOUNGLINGS-2678');
    it('Проверка бокового меню', function() {
        return this.browser
            .yaOpenPage('/subject/tag/problems/?ege_number_id=176&tag_id=19')
            .yaScroll(PO.NavCard.LastLink())
            .click(PO.NavCard.LastLink())
            .pause(animationDelay)
            .yaShouldBeVisible(PO.TagTasksToSolveList.TitleLast())
            .click(PO.NavCard.FirstLink())
            .pause(animationDelay)
            .yaShouldBeVisible(PO.TagTasksToSolveList.TitleFirst())
            .assertView('plain', PO.NavCard());
    });
});
