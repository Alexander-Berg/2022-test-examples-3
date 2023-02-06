(function() {
    var urlParams = new URLSearchParams(window.location.search);
    var userTime = urlParams.get('user_time');

    if (!userTime) {
        return;
    }

    var userDate = new Date(userTime);
    if (userDate.toString() === 'Invalid Date') {
        return;
    }

    var time = userDate.getTime();
    Date.now = function() {
        return time;
    };
})();
