module.exports = function() {
    return this.execute(() => {
        const state = window.quasar.toJSON();

        let pageUrl = state.pageUrl;

        if (pageUrl) {
            const url = new URL(pageUrl);

            ['tpid', 'testRunId'].forEach((key) => {
                url.searchParams.delete(key);
            });

            pageUrl = url.pathname + url.search;
        }

        return {
            ...state,
            pageUrl,
        };
    }).then((result)=> result.value);
};
