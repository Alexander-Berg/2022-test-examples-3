'use strict';

let _ = require('underscore');

async function runMiddleware(middleware, req, res) {
    middleware = _.flatten(middleware);
    
    let callNextMiddleware = async (err) => {
        if (err) {
            console.error(err);
        }
        
        let currentStep = middleware.shift();

        if (currentStep) {
            try {
                await currentStep(req, res, callNextMiddleware);
            } catch (ex) {
                console.error(ex);
            }
        }
    };
    
    await callNextMiddleware();
}

module.exports = runMiddleware;