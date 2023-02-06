import nock from 'nock';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const nockBlackbox = (blackboxUrl: string, status: number, result: any) => {
    nock(blackboxUrl)
        .persist()
        .get((actualUri) => actualUri.startsWith('/blackbox'))
        .reply(status, result);
};

export const tvmServiceMock = {
    getTicket: jest.fn() as jest.Mock,
};

tvmServiceMock.getTicket.mockReturnValue({ ticket: 'ticket' });
