window.__hermioneLogger = function(data) {
    window.fetch('/logger', {
        method: 'post',
        body: JSON.stringify(data),
    });
};
