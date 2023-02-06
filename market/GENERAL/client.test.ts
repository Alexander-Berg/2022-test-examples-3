/* eslint-disable max-classes-per-file */

import {HttpTransport} from '../http/transport';
import {Backend} from './backend';
import {Client} from './client';
import {Context} from './context';

test('Client connects with Backend', function () {
    class SomeBackend extends Backend.setup(
        HttpTransport,
        'someName',
        {host: 'http://localhost'},
    ) {
        public someBackMethod() { return 'success'; }
    }

    class SomeClient extends Client.connect({
        backend: SomeBackend,
    }) {
        public someClientMethod() {
            return this.backend.someBackMethod();
        }
    }

    const ctx = new Context(null);
    const client = SomeClient.factory(ctx);

    expect(client.someClientMethod()).toBe('success');
});
