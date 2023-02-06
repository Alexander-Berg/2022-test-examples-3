export class MockClient {
    // @ts-expect-error(TS7031) найдено в рамках MARKETPARTNER-16237
    static connect({backend: Backend}) {
        const backend = null;
        class ConnectedClient {
            // @ts-expect-error(TS7008) найдено в рамках MARKETPARTNER-16237
            backend;
            // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
            static factory(ctx) {
                const instance = new this();
                instance.backend = new Backend();
                instance.backend.sk = ctx.sk;
                return instance;
            }
        }
        return ConnectedClient;
    }
}
