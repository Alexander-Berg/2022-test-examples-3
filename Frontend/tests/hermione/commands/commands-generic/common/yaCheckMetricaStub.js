module.exports = function(goal, payload) {
    const message = payload ? `Цель "${goal}" c данными ${JSON.stringify(payload)} не найдена` : `Цель "${goal}" не найдена`;

    return this
        .waitUntil(
            () =>
                this.execute(function(goal) {
                    return window.Ya.hermione.metricaLog.get(goal);
                }, goal)
                    .then(result => {
                        const metrics = result.value;

                        if (metrics.length && !payload) {
                            return true;
                        }

                        if (payload) {
                            function metricaContainsPayload(metrica) {
                                return Object.keys(payload)
                                    .every(key => {
                                        const value = payload[key];

                                        // Проверяем просто наличие свойстваы
                                        if (typeof value === 'undefined') {
                                            return metrica.params.hasOwnProperty(key);
                                        }

                                        return metrica.params[key] === value;
                                    });
                            }

                            return metrics.some(metricaContainsPayload);
                        }

                        return false;
                    }),
            10000, message);
};
