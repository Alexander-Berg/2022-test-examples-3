module.exports = function(timeout = 10000) {
    return this.waitUntil(
        async () => {
            const { lastFrame, currentTime } = await this.execute(() => {
                return {
                    lastFrame: window.performance.getEntriesByType('frame').pop(),
                    currentTime: window.performance.now(),
                };
            }).then((result) => result.value);

            // console.warn(lastFrame.startTime, currentTime, currentTime - lastFrame.startTime);
            return (currentTime - lastFrame.startTime) > 2000;
        },
        timeout,
        'Анимация кадров всё еще не завершена',
    );
};
