/**
 * Заглушка для подмены модуля asker в тестах.
 */

const askerStub = sinon.stub();
const originalResetBehavior = askerStub.resetBehavior; // Сохраняем оригинальный resetBehavior стаба

/**
 * Подменяем resetBehavior стаба.
 * Стаб нашей заглушки должен сначала вызывать оригинальный resetBehavior,
 * а также устанавливать дефолтное поведение стабу (просто резолвим запрос без объекта ответа).
 */
askerStub.resetBehavior = function() {
    originalResetBehavior.call(askerStub);
    askerStub.returns(Promise.resolve());
};
askerStub.resetBehavior();

module.exports = askerStub;
