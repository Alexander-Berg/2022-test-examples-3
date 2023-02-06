'use strict';

const functions = require('./functions.js');

describe('services/msearch/functions', () => {
    it('serializeSubscriptions', () => {
        const request = {
            params: {
                subscriptions: [
                    {
                        messageTypes: [ 7, 13 ],
                        email: 'foo@example.com',
                        action: 'action'
                    }
                ]
            }
        };

        expect(functions.serializeSubscriptions(request)).toMatchSnapshot();
    });

    it('serializeSubscriptions with moveExisting', () => {
        const request = {
            params: {
                subscriptions: [
                    {
                        messageTypes: [ 7, 13 ],
                        email: 'foo@example.com',
                        moveExisting: true,
                        action: 'action'
                    }
                ]
            }
        };

        expect(functions.serializeSubscriptions(request)).toMatchSnapshot();
    });
});
