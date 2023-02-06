describe('Карточка темы', () => {
  it('должна отображаться корректно', function() {
    return this.browser
      .openComponent('themepage', 'default', 'phone')
      .assertView('plain', '.news-theme-page');
  });

  it('длинная аннотация должна разворачивается по клику на "Читать дальше"', function() {
    return this.browser
      .openComponent('themepage', 'long-annotation', 'phone')
      .assertView('with-read-more', ['.news-theme-page__annot', '.news-theme-page__annot-expand'])
      .click('.news-theme-page__annot-expand')
      .yaShouldNotExist('.news-theme-page__annot-expand', 'Кнопка "Читать дальше" присутствует')
      .assertView('expanded', '.news-theme-page__annot');
  });
});
