describe('Окно добавления источника в избранное из внешнего источника', () => {
  describe('должно отображаться корректно у авторизованного пользователя', () => {
    it('с еще не добавленным источником', function() {
      return this.browser
        .openComponent('external-favorite-signed-in', 'default', 'phone')
        .yaWaitForVisible('.news-add-favorite-source-modal__icon')
        .assertView('external-favorite-signed-in-without-favorite', '.mg-modal__container');
    });
  });

  describe('должно отображаться корректно у неавторизованного пользователя', () => {
    it('с еще не добавленным источником', function() {
      return this.browser
        .openComponent('external-favorite-logged-out', 'default', 'phone')
        .yaWaitForVisible('.news-add-favorite-source-modal__icon')
        .assertView('external-favorite-logged-out-without-favorite', '.mg-modal__container');
    });
  });
});
