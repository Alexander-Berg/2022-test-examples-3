module.exports = function() {
    return this.waitUntil(function() {
        return this.execute(function() {
            return document.readyState;
        }).then(ret => {
            return ret.value !== 'completed';
        });
    }, 30000, 'Страница не загрузилась в течение 30 секунд');
};
