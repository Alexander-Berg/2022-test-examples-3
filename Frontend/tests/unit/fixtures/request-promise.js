/**
 * Заглушка для подмены модуля request-promise в тестах.
 */

const requestPromiseStub = sinon.stub();
const originalResetBehavior = requestPromiseStub.resetBehavior; // Сохраняем оригинальный resetBehavior стаба

/**
 * Подменяем resetBehavior стаба.
 * Стаб нашей заглушки должен сначала вызывать оригинальный resetBehavior,
 * а также устанавливать дефолтное поведение стабу (просто резолвим запрос без объекта ответа).
 */
requestPromiseStub.resetBehavior = function() {
    originalResetBehavior.call(requestPromiseStub);
    requestPromiseStub.returns(Promise.resolve());
};
requestPromiseStub.resetBehavior();

module.exports = requestPromiseStub;
