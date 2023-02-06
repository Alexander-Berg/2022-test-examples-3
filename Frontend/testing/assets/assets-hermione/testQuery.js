(function() {
    function filterParams(locationQuery) {
        return locationQuery
            .map(function(el) { return el.split('=') })
            .filter(function(data) {
                return data[0] === 'tpid' || data[0] === 'exp_flags';
            })
            .map(function(params) {
                return params.join('=');
            })
            .join('&');
    }

    window.Ya = window.Ya || {};
    var query = location.href.split('?')[1];

    window.Ya.testQuery = query ? filterParams(query.split('&')) : '';
})();
