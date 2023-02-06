export function getTestIdsInUrl() {
    const testIds = window.location.search.match(/test-id=([_\d]+)/);

    if (testIds && testIds.length > 1) {
        return [...new Set(testIds[1].split('_').map(Number))];
    }
    return [];
}

export function cleanUrlExps() {
    document.location.href = window.location.search.replace(/&?test-id=[_\d]+/, '');
}

function getSearchWithTestIds(testId: number) {
    const regExp = /(test-id=[_\d]+)/;
    const search = window.location.search;

    if (regExp.test(search)) {
        return search.replace(regExp, '$1_' + testId);
    }
    if (search === '') {
        return `?test-id=${testId}`;
    }
    return `${search}&test-id=${testId}`;
}

export function onUrlStuckExp(testId: number) {
    document.location.href = `${window.location.pathname}${getSearchWithTestIds(testId)}`;
}

export function onUrlUnstuckExp(testId: number) {
    const newTestIdsInUrl = getTestIdsInUrl()
        .filter(id => id !== testId)
        .join('_');

    if (newTestIdsInUrl === '') {
        cleanUrlExps();
    } else {
        document.location.href = window.location.search.replace(/(test-id=)[_\d]+/, '$1' + newTestIdsInUrl);
    }
}
