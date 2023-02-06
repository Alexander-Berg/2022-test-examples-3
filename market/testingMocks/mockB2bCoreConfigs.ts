export default {
    getConfiguration(): Record<string, unknown> {
        return new Proxy(
            {},
            {
                get: () => {
                    return this.getConfiguration();
                },
            },
        );
    },
};
