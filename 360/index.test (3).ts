import {
    TUaasRequest,
    TUaasResponse
} from './'

const uaasRequest = {
    UsersplitRequestParams: {
        Experiments: {
            AddExperiments: [
                '12345'
            ]
        },
        Ip: '1.2.3.4',
        Restrictions: {
            Service: 'mail'
        },
        SplitIds: {
            Uuid: 'deadbeef'
        }
    }
};

const uaasResponse = '' +
    'IgAqEG1haWwtbW9iaWxlLWFwcHMyrQMSCQoFODA1NTYQChIKCgY0NTQ1MzQQOiKVAnsiQ09O' +
    'VEVYVCI6eyJNQUlMIjp7ImV4cGVyaW1lbnQiOnsibmFtZSI6InByb21vc19jb25maWciLCJ0' +
    'ZXN0SWQiOiI4MDU1NiIsImlzRW5hYmxlZCI6dHJ1ZSwiZGF0YSI6eyJkYXJrX3RoZW1lIjp7' +
    'InNob3dzIjoxMH0sInR1cm5fb25fbm90aWZpY2F0aW9ucyI6eyJzaG93cyI6MTB9LCJtYW5h' +
    'Z2Vfc3Vic2NyaXB0aW9ucyI6eyJzaG93cyI6MTB9LCJsaW5rX3Bob25pc2giOnsic2hvd3Mi' +
    'OjEwfSwiZXh0ZXJuYWxfbWFpbHMiOnsic2hvd3MiOjEwfX19fX0sIkhBTkRMRVIiOiJNQUlM' +
    'In0ifHsiQ09OVEVYVCI6eyJNQUlMIjp7ImV4cGVyaW1lbnQiOnsibmFtZSI6ImZvbGRlcmxp' +
    'c3Rfc3Vic2NyaXB0aW9ucyIsInRlc3RJZCI6IjQ1NDUzNCIsImlzRW5hYmxlZCI6ZmFsc2V9' +
    'fX0sIkhBTkRMRVIiOiJNQUlMIn06BTE2NDM2QgBKIHZ1SGY1LVRtYW5RcW1QR29jQ0NKZHo2' +
    'VFZ1LTBsbFIx';

test('request', () => {
    const req = TUaasRequest.fromPartial(uaasRequest);

    expect(TUaasRequest.encode(req).finish()).toMatchSnapshot();
});

test('response', () => {
    const res = Buffer.from(uaasResponse, 'base64');

    expect(TUaasResponse.decode(res)).toMatchSnapshot();
});
