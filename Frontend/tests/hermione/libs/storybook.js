module.exports = function stories(feature, options = {}) {
    const tests = [];

    return {
        add(title, knobs) {
            tests.push({
                title,
                knobs: {
                    ...options.knobs,
                    ...knobs,
                },
            });

            return this;
        },
        run(handler) {
            specs({
                feature,
            }, () => {
                tests.map((story) => {
                    return it(story.title, async function () {
                        const { browser } = this;

                        await browser.chatOpenComponent(feature, options.story, story.knobs);
                        await browser.waitForVisible(options.element);
                        await browser.assertView(story.title, options.element, { allowViewportOverflow: true });

                        if (handler) {
                            await handler(browser, story);
                        }
                    });
                });
            });
        },
    };
};
