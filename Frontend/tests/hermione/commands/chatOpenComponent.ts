const STORY_BOOK_URL = '/storybook';

/**
 * Открытие компонента мессенджера в Storybook
 *
 * @param {String} selectedKind
 * @param {String} selectedStory
 * @param {object} [knobValues]
 */
module.exports = async function chatOpenComponent(
    selectedKind: string,
    selectedStory: string,
    knobValues: Record<string, any>
) {
    let storyUrl = `${STORY_BOOK_URL}/iframe.html?selectedKind=${selectedKind}&selectedStory=${selectedStory}`;

    if (knobValues) {
        storyUrl += `&${knobQueryString(knobValues)}`;
    }

    await this.url(storyUrl);
};

function knobQueryString(knobValues) {
    return Object.keys(knobValues)
        .map((name) => `knob-${name}=${encodeURIComponent(serializeKnobValue(knobValues[name]))}`)
        .join('&');
}

function serializeKnobValue(value) {
    if (typeof value === 'object' && !Array.isArray(value)) {
        return JSON.stringify(value);
    }

    return String(value);
}
