class MarketHttpBackend {
    static setup() {
        return class {};
    }
}

class ConnectedClient {
    backend = {
        fetch() {
            return Promise.resolve({});
        },
    };
}

class MarketClient {
    static connect() {
        return ConnectedClient;
    }
}

module.exports = {MarketHttpBackend, MarketClient};
