hermione.only.notIn(['win-ie11'], 'статика для IE локализована, прогон тестов неактуален');
describe('Окно добавления источника в избранное из внешнего источника', () => {
  describe('должно отображаться корректно у авторизованного пользователя', () => {
    hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
    it('с еще не добавленным источником', function() {
      return this.browser
        .openComponent('external-favorite-signed-in', 'default', 'desktop')
        .yaWaitForVisible('.news-add-favorite-source-modal__icon')
        .assertView('external-favorite-signed-in-without-favorite', '.Modal-Content');
    });
  });

  describe('должно отображаться корректно у неавторизованного пользователя', () => {
    hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
    it('с еще не добавленным источником', function() {
      return this.browser
        .openComponent('external-favorite-logged-out', 'default', 'desktop')
        .yaWaitForVisible('.news-add-favorite-source-modal__icon')
        .assertView('external-favorite-logged-out-without-favorite', '.Modal-Content');
    });
  });
});
