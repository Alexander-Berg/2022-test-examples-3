window.hermione.appsApiMethodsStorage = {};
window.hermione.appsApiEventsStorage = {};

window.Ya.AppsApi.BackendBridge.get(function(backend) {
    backend._log = function(event, data) {
        if (!window.hermione) {
            return;
        }

        var triggeredData = window.hermione.appsApiEventsStorage[event] || [];

        triggeredData.push(data);

        window.hermione.appsApiEventsStorage[event] = triggeredData;
    };
});
