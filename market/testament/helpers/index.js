// @flow

// flowlint-next-line unclear-type:off
export const createCollection = (mocks: any[], defaultMock: any = {}) => mocks
    .reduce((acc, {id, ...collectionProps}) => ({
        ...acc,
        [id]: {
            ...defaultMock,
            ...collectionProps,
            id,
        },
    }), {});
