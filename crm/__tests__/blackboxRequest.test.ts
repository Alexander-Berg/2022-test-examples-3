import { createGrpc } from '@crm/apphost-test';
import { createApp } from '../createApp';

describe('/blackbox/request', () => {
    const grpc = createGrpc(createApp);

    it('returns correct path', async () => {
        const response = await grpc('/blackbox/request', {
            context: [
                {
                    name: 'REQUEST',
                    results: [
                        {
                            type: 'proto_http_request',
                            binary: {
                                Headers: [
                                    { Name: 'Cookie', Value: 'Session_id=1' },
                                    { Name: 'Host', Value: 'test.site' },
                                ],
                                RemoteIP: 'RemoteIP',
                                Path: '/graphql',
                            },
                            __content_type: 'json',
                        },
                    ],
                },
            ],
        });

        const params = [
            'method=sessionid',
            'sessionid=1',
            'host=test.site',
            'userip=RemoteIP',
            'format=json',
            'get_user_ticket=yes',
        ];
        expect(response.answers[0].Path).toBe(`?${params.join('&')}`);
    });
});
