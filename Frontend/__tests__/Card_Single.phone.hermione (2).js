describe('Одиночная карточка', () => {
  hermione.only.notIn(['appium-chrome-phone', 'searchapp-iphone', 'searchapp-android'], '-');
  it('должена отображаться корректно', function() {
    return this.browser
      .openComponent('card-single', 'with-favorite-annotation')
      .yaWaitForVisible('.mg-card_single')
      .assertView('favorite-annotation', '.mg-card_single');
  });
});
