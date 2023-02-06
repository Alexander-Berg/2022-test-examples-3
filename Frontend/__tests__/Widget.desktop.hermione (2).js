hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
describe('Боковой виджет', () => {
  it('должен отображаться корректно', function() {
    return this.browser
      .openComponent('Widget', 'default')
      .yaWaitForVisible('.sport-widget')
      .assertView('plain', '.sport-widget');
  });
});
