/**
 * Заглушка для подмены модуля logger в тестах.
 */

const loggerStub = {
    info: sinon.stub(),
    warn: sinon.stub(),
    error: sinon.stub(),
};
function originalResetBehavior(obj = {}) {
    loggerStub.info.resetBehavior(obj.info);
    loggerStub.warn.resetBehavior(obj.warn);
    loggerStub.error.resetBehavior(obj.error);
} // Сохраняем оригинальный resetBehavior стаба

/**
 * Подменяем resetBehavior стаба.
 * Стаб нашей заглушки должен сначала вызывать оригинальный resetBehavior,
 * а также устанавливать дефолтное поведение стабу (просто резолвим запрос без объекта ответа).
 */
loggerStub.resetBehavior = function() {
    originalResetBehavior.call(loggerStub);
    loggerStub.info.returns(Promise.resolve());
    loggerStub.warn.returns(Promise.resolve());
    loggerStub.error.returns(Promise.resolve());
};

loggerStub.reset = function() {
    loggerStub.info.reset();
    loggerStub.warn.reset();
    loggerStub.error.reset();
};
loggerStub.resetBehavior();

module.exports = loggerStub;
