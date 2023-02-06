/* global nextTick */
const _ = require('lodash');
let requestsHistory;

global.createFakeXHR = function() {
    this.deleteFakeXHR();

    this.xhr = sinon.useFakeXMLHttpRequest();
    const requests = this.requests = [];
    const responses = this.responses = [];

    this.xhr.onCreate = function(xhr) {
        if (responses.length) {
            nextTick(() => {
                const response = responses.shift();

                if (!response) {
                    throw 'Необработынный запрос на сервер (пропустили addResponse)';
                }

                xhr.respond(
                    response.status || 200,
                    response.header || { 'Content-Type': 'application/json' },
                    response.body
                );
                // console.log('request response', xhr, response);
            });
        } else {
            requests.push(xhr);
        }

        requestsHistory.push(xhr);
    };

    this.addResponse = function(response) {
        if (requests.length) {
            const request = requests.shift();

            request.respond(
                response.status || 200,
                response.header || { 'Content-Type': 'application/json' },
                response.body
            );

            // console.log('request response', request, response);
        } else {
            responses.push(response);
        }
    };

    this.addResponseModel = function(responseData) {
        if (responseData instanceof Array) {
            responseData.forEach(function(item) {
                this.addResponseModel(item);
            }, this);

            return;
        }

        let body = {
            models: [{
                data: responseData
            }]
        };

        body = JSON.stringify(body);
        this.addResponse({
            body: body
        });
    }.bind(this);

    this.getLastRequest = function() {
        return _.last(requestsHistory);
    };
}.bind(global);

global.deleteFakeXHR = function() {
    requestsHistory = [];

    delete this.addResponse;
    delete this.addResponseModel;
    delete this.getLastRequest;

    this.xhr && this.xhr.restore();
    delete this.xhr;
}.bind(global);

global.getRequests = function() {
    return requestsHistory;
};

global.nextTick = function(fn) {
    setTimeout(fn, 0);
};
