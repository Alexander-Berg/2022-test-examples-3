function createTestTagSelector(testTag, dataTag) {
    const selector = testTag.includes('[data-test-tag=') ?
        [testTag] :
        [`[data-test-tag="${testTag}"]`];

    if (dataTag) {
        selector.push(`[data-test-data="${dataTag}"]`);
    }

    return selector.join('');
}

module.exports = {
    createTestTagSelector,
};
