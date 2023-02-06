const PLATFORMS = ['desktop', 'touch-phone'];

module.exports = async function(componentName, storyName, params) {
    if (!componentName) {
        throw new TypeError('Invalid parameter: componentName.');
    }

    if (!storyName) {
        throw new TypeError('Invalid parameter: storyName.');
    }

    const { platform } = await this.getMeta();

    if (!PLATFORMS.includes(platform)) {
        throw new TypeError('Invalid platform.');
    }

    const searchParams = new URLSearchParams({
        id: [
            'components',
            componentName.toLowerCase(),
            platform,
            storyName.replace(/[A-Z]/g, char => '-' + char.toLowerCase()),
        ].join('-'),
        viewMode: 'story',
        ...params,
    });

    await this.url('/build/storybook/iframe.html' + '?' + searchParams);
};
