import nock from 'nock';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const nockTvmServer = (tvmUrl: string, status: number, result: any, spy?: jest.Mock) => {
    const reqheaders = {
        accept: 'application/json',
        authorization: 'tvmtool-development-access-token',
    };

    nock(tvmUrl, { reqheaders })
        .get((actualUri) => actualUri.startsWith('/tvm/tickets'))
        .reply(status, () => {
            if (spy) {
                spy();
            }

            return result;
        });
};
