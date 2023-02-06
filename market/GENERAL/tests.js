function populateTestNameString(test, str = '') {
    if (test?.title) {
        str = test.title + ' ' + str;
    }

    if (test?.parent) {
        str = populateTestNameString(test.parent, str);
    }

    return str;
}

exports.getTestName = function(test) {
    // В h2 тестах, помимо "традиционного места", готовое название может быть в __title
    if (test?.fn?.__title) {
        return test.fn.__title;
    }

    const str = populateTestNameString(test);

    return str.trim();
}
