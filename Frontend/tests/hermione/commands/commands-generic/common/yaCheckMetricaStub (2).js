module.exports = function(goal, payload) {
    return this
        .execute(function(goal) {
            return window.Ya.hermione.metricaLog.get(goal);
        }, goal)
        .then(result => {
            const metrics = result.value;
            const metricaContainsPayload = metrica =>
                Object.keys(payload).every(key => metrica.params[key] === payload[key]);

            assert.isTrue(metrics.length > 0, `Цель ${goal ? goal : ''} была найдена`);

            if (payload) {
                const hasMetricsWithPayload = metrics.some(metricaContainsPayload);
                assert.isTrue(hasMetricsWithPayload, `Цель ${goal ? goal : ''} не содержит данных ${JSON.stringify(payload)}`);
            }
        });
};
