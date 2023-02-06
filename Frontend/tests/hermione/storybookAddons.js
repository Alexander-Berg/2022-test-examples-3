const STORY_BOOK_URL = 'build/storybook/iframe.html';
const FEATURE = 'Widget';

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

function getStorybookUrl(selectedStory, knobValues) {
    let storyUrl = `${STORY_BOOK_URL}?path=/story/${FEATURE}--${selectedStory.replace(' ', '-')}`.toLowerCase();

    if (knobValues) {
        storyUrl += `&${knobQueryString(knobValues)}`;
    }

    return storyUrl;
}

module.exports = function stories(feature, options = {}) {
    const tests = [];

    return {
        add(title, knobs, hooks, { only } = {}) {
            tests.push({
                title,
                knobs: {
                    ...options.knobs,
                    ...knobs,
                },
                hooks,
                only,
            });

            return this;
        },
        run(hooksEach) {
            describe(feature, () => {
                tests.map((story) => {
                    return (story.only ? it.only : it)(story.title, async function () {
                        const { browser } = this;

                        await browser.url(getStorybookUrl(feature, story.knobs));

                        await browser.waitForVisible(global.CONSTANTS.STORY_ROOT);

                        if (story.hooks && story.hooks.before) {
                            await story.hooks.before(browser, story);
                        } else if (hooksEach && hooksEach.before) {
                            await hooksEach.before(browser, story);
                        }

                        await browser.waitForVisible(options.element);
                        // Клик, для того чтобы снять фокус с инпутов
                        await browser.leftClick(global.CONSTANTS.STORY_ROOT, -5, 5);
                        await browser.pause(100);

                        if (story.hooks && story.hooks.after) {
                            await story.hooks.after(browser, story);
                        } else if (hooksEach && hooksEach.after) {
                            await hooksEach.after(browser, story);
                        }

                        await browser.execute(() => {
                            localStorage.clear();
                        });
                    });
                });
            });
        },
    };
};
