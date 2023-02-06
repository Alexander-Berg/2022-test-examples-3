import { ProtobufJSContext } from '@yandex-int/apphost-lib';
import { ResponseSender } from './sender';
import { NAppHostHttp } from '../generated/protos';
import { Req } from './types';

describe('ResponseSender', function() {
    let sender: ResponseSender<NAppHostHttp.THttpResponse>;
    let ctx: ProtobufJSContext;
    const fakeReq = {} as unknown as Req;
    let sendProtoItemMock = jest.fn();
    beforeEach(() => {
        sender = new ResponseSender('res', NAppHostHttp.THttpResponse);
        sendProtoItemMock = jest.fn();
        ctx = {
            sendProtoItem: sendProtoItemMock,
        } as unknown as ProtobufJSContext;
    });
    it('отравляет упакованые объекты в контекст', async function() {
        const wrapped = sender.wrap(() => {
            return {
                StatusCode: 431,
                Headers: [
                    {
                        Name: 'X-test',
                        Value: 'xxx',
                    },
                ],
                Content: Buffer.from('hello world'),
            };
        });

        expect(await wrapped(fakeReq, ctx)).toEqual(true);

        expect(ctx.sendProtoItem).toBeCalled();

        const data = sendProtoItemMock.mock.calls[0][2];

        expect(data).toBeInstanceOf(NAppHostHttp.THttpResponse);
        expect(data.StatusCode).toEqual(431);
    });

    it('не возвращает true, если объект не вернули', async function() {
        const wrapped = sender.wrap(() => {
            return;
        });

        expect(await wrapped(fakeReq, ctx)).toBeFalsy();
    });

    it('если это обработчик ошибок, то кидает исходную ошибку', function() {
        const wrapped = sender.wrap(() => {
            return;
        });
        const e = new Error('test error');

        return expect(wrapped(fakeReq, ctx, e)).rejects.toBe(e);
    });
});
