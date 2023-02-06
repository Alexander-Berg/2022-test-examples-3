window.Daria = {
    Config: {},

    Page: {},

    UA: {},

    api: {
        models: '/models/'
    },

    uid: '123',

    themeId: 'colorful',

    DEBUG: false,

    commonRequestParams: function() {
        return {};
    },

    Statusline: {
        show: function() {}
    }
};

window.Jane = {

    ErrorLog: {
        send: function(name, exception, data) {},

        sendException: function(name, exception, data) {}
    },

    Services: {
        hasNewVersion: function() {
            return false;
        },

        run: function(route, callback) {
            callback();
        }
    }
};
