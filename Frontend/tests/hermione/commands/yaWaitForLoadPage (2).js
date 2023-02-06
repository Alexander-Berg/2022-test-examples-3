module.exports = function() {
    return this.waitUntil(function() {
        return this.execute(() => document.readyState)
            .then((ret) => ret.value !== 'completed');
    }, 30000, 'Страница не загрузилась в течение 30 секунд');
};
