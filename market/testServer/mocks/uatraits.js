jest.mock(
    'uatraits',
    () => ({
        Detector: class {
            // eslint-disable-next-line class-methods-use-this
            detectByHeaders() {
                return {};
            }
        },
    }),
    {virtual: true},
);
