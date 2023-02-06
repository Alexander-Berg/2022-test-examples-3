describe('Боковой виджет', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');

  it('должен отображаться корректно', function() {
    return this.browser
      .openComponent('widget', 'default', 'desktop')
      .yaWaitForVisible('.news-widget')
      .assertView('right-widget', '.news-widget');
  });
});
