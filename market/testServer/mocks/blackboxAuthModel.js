jest.mock('@yandex-market/mandrel/bcm/blackbox/BlackBoxAuthModel', () => {
    class BlackBoxAuthModel {
        static factory() {
            return new this();
        }

        // eslint-disable-next-line class-methods-use-this
        async isAuth() {
            return true;
        }

        // eslint-disable-next-line class-methods-use-this
        async getAuth() {
            return {};
        }
    }

    return {
        BlackBoxAuthModel,
        BLACKBOX_AUTH_TYPE: {
            TVM: 'TVM',
            OAUTH: 'OAUTH',
            SESSION: 'SESSION',
        },
    };
});
