/**
 * Takes a screenshot of the passed selector and compares the received screenshot with the reference.
 *
 * @remarks
 * For more details, see {@link https://github.com/gemini-testing/hermione#assertview documentation}.
 *
 * @example
 * ```ts
 *
 * it('some test', function() {
 *     return this.browser
 *         .url('some/url')
 *         .assertView(
 *             'plain',
 *             '.button',
 *             {
 *                 ignoreElements: ['.link'],
 *                 tolerance: 2.3,
 *                 antialiasingTolerance: 4,
 *                 allowViewportOverflow: true,
 *                 captureElementFromTop: true,
 *                 compositeImage: true,
 *                 screenshotDelay: 600,
 *                 selectorToScroll: '.modal'
 *             }
 *         )
 *});
 * ```
 *
 * @param state state name, should be unique within one test
 * @param selectors DOM-node selector that you need to capture
 * @param opts additional options, currently available:
 * "ignoreElements", "tolerance", "antialiasingTolerance", "allowViewportOverflow", "captureElementFromTop",
 * "compositeImage", "screenshotDelay", "selectorToScroll"
 */
module.exports = async function yaAssertView(state, selectors, opts) {
    await this.assertView(state, selectors, opts);
    await this.yaToggleDarkTheme(true);
    await this.assertView(state + '_dark_theme', selectors, opts);
    await this.yaToggleDarkTheme(false);
};
