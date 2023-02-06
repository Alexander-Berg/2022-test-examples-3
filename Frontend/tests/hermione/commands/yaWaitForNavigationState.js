module.exports = function(newDirectionState, timeout = 30000) {
    return this.waitUntil(
        async () => {
            const { availableDirections } = await this.yaQuasarGetAll();

            return Object.entries(newDirectionState).every(
                ([direction, value]) => availableDirections[direction] === value,
            );
        },
        timeout,
        `Навигация заблокирована в заданном направлении: ${JSON.stringify(newDirectionState)}`,
    );
};
