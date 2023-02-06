module.exports = function(fullSnippet) {
    return {
        type: 'snippet',
        data_stub: {
            num: 0,
            snippets: {
                full: fullSnippet
            }
        }
    };
};
