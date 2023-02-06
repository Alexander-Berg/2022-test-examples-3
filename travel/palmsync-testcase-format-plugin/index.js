// Быстрый и простой способ обойти некоторые ограничения со стороны palmsync
// Не вынесен в настоящий модуль для удобства разработки + по сути это набор костылей
module.exports = palmsync => {
    palmsync.on(palmsync.events.TEST_CASE, testCase => {
        // Зачищаем список браузеров
        testCase.attributes.browsers = [];
    });
};
