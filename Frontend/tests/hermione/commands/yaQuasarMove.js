module.exports = async function(direction, { tillEnd } = {}, timeout = 5000) {
    const { suggests: oldSuggests } = await this.yaQuasarGetAll();

    await this.yaWaitForNavigationState({ [direction]: true });

    await this.yaQuasarRunCommand('navigation', {
        direction,
        origin: 'voice',
        scroll_amount: tillEnd ? 'till_end' : undefined,
    });

    return this.waitUntil(
        async () => {
            const { suggests } = await this.yaQuasarGetAll();

            return suggests.some((item, i) => oldSuggests[i] !== item);
        },
        timeout,
        `Стейт Quasar не изменился после навигации "${direction}"`,
    );
};
