const STORY_BOOK_URL = '/static/storybook';

function knobsToQuery(knobs) {
    if (!knobs) return '';

    return knobs.reduce((acc, knob) => acc + `&knob-${knob.name}=${knob.value}`, '&');
}

/**
 * Открытие компонента мессенджера в Storybook
 *
 * @param {String} path
 * @param {Boolean} [withPlatform = false]
 * @param {Array<{ name: string, value: string}>} [knobs]
 */
module.exports = async function yaOpenComponent(path, withPlatform = false, knobs) {
    const [storiesName, storyName] = path.split('--');
    let storyUrl = '';
    const knobsParameters = knobsToQuery(knobs);

    if (withPlatform) {
        const platform = await this.getMeta('platform');

        storyUrl = `${STORY_BOOK_URL}/iframe.html?id=${storiesName}-${platform}--${storyName}${knobsParameters}`;
        await this.url(storyUrl);
    } else {
        storyUrl = `${STORY_BOOK_URL}/iframe.html?id=${storiesName}--${storyName}${knobsParameters}`;
        await this.url(storyUrl);
    }
};
