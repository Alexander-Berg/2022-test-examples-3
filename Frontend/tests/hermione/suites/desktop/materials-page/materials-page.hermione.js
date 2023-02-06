describe('Страница материалов', function() {
    describe('Блок результатов поиска', () => {
        beforeEach(function() {
            return this.browser
                .yaOpenPage('/ege/')
                .click(PO.HeadTabs.LinkItemFifth())
                .yaWaitForLoadPage()
                .yaWaitForVisible(PO.MaterialsPage());
        });

        it('Общий вид', function() {
            return this.browser
                .assertView('empty-form', PO.SearchForm())
                .assertView('empty-results', PO.MaterialsPage.Results())
                .assertView('popular-queries', PO.PopularQueries(), {
                    invisibleElements: ['.YndxBug'],
                })
                .setValue(PO.SearchForm.Input.Control(), 'geirby doc')
                .yaWaitChangeUrl(() => this.browser.click(PO.SearchForm.Submit()))
                .execute(function() {
                    // Edge не успевает убрать фокус из TextInput
                    document.activeElement && document.activeElement.blur();
                })
                .yaWaitForVisible(PO.MaterialsPage())
                .yaWaitForHidden(PO.MaterialsPage.ContentLoading())
                .assertView('results', PO.MaterialsPage.Results(), {
                    invisibleElements: ['.YndxBug'],
                });
        });

        it('Саджест', function() {
            return this.browser
                .click(PO.SearchForm.Input.Control())
                .setValue(PO.SearchForm.Input.Control(), 'пушкин')
                .yaWaitForVisible(PO.Suggest(), 5000)
                .execute(function() {
                    document.activeElement && document.activeElement.select();
                })
                .assertView('suggest', [PO.SearchForm.Input(), PO.Suggest()], {
                    invisibleElements: [PO.PopularQueries(), '.YndxBug'],
                });
        });
    });
});
