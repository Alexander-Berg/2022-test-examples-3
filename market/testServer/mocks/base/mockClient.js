// eslint-disable-next-line max-classes-per-file
export class MockClient {
    static connect(connectParams) {
        let context;
        function ConnectedClient() {}

        Object.keys(connectParams).forEach(key => {
            const dependency = new connectParams[key]();
            ConnectedClient.prototype[key] = dependency;

            Object.defineProperty(dependency, 'context', {
                get() {
                    return context;
                },
            });
        });

        Object.defineProperty(ConnectedClient.prototype, 'context', {
            get() {
                return context;
            },
        });

        ConnectedClient.factory = function(stoutContext) {
            const instance = new this();
            context = {stoutContext};

            return instance;
        };

        return ConnectedClient;
    }
}
