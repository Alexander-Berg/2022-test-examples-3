import { createGrpc } from '@crm/apphost-test';
import { createApp } from '../createApp';

describe('/hello', () => {
    const grpc = createGrpc(createApp, {
        endpoint: '/hello',
    });

    // eslint-disable-next-line
    it.skip('returns hello', async () => {
        const response = await grpc();

        const content = Buffer.from(
            response.answers[0].Content,
            'base64',
        ).toString('ascii');
        expect(content).toBe('<h1>Hello apphost from nodejs!</h1>');
    });
});
