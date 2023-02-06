module.exports = function(route, params) {
    return {
        findFirst: function() {
            return [
                {
                    getData: function() {
                        return route;
                    },
                },
                params,
            ];
        },
    };
};
