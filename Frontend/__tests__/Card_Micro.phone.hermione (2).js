describe('Микро карточка', () => {
  hermione.only.notIn(['appium-chrome-phone', 'searchapp-iphone', 'searchapp-android'], '-');
  it('должена отображаться корректно', function() {
    return this.browser
      .openComponent('card-micro', 'with-favorite-annotation')
      .yaWaitForVisible('.mg-card_micro')
      .assertView('favorite-annotation', '.mg-card_micro');
  });
});
