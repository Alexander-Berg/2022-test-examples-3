import { pushNotification } from 'src/store/notification/actions';
import { getType } from 'typesafe-actions';
import errorHandler from './errorHandler';

const defaultMessage = 'Неизвестная ошибка';

describe('errorHandler', () => {
  it('should return default notification', () => {
    return errorHandler('default notification')
      .toPromise()
      .then(action => {
        expect(action.type).toEqual(getType(pushNotification));
        expect(action.payload.title).toEqual('Ошибка');
        expect(action.payload.status).toEqual('error');
        expect(action.payload.content).toEqual(defaultMessage);
        expect(action.payload.hasCloser).toEqual(true);
        expect(action.payload.hidden).toEqual(false);
      });
  });

  it('should return notification with error message', () => {
    return errorHandler(new Error('test message'))
      .toPromise()
      .then(action => {
        expect(action.type).toEqual(getType(pushNotification));
        expect(action.payload.content).toEqual('test message');
      });
  });

  it('should return notification with response message from json', () => {
    return errorHandler(new Response('{ "type": "INTERNAL", "message": "Internal error" }'))
      .toPromise()
      .then(action => {
        expect(action.type).toEqual(getType(pushNotification));
        expect(action.payload.content).toEqual('Internal error');
      });
  });

  it('should return notification with invalid response', () => {
    return errorHandler(new Response('{'))
      .toPromise()
      .then(action => {
        expect(action.type).toEqual(getType(pushNotification));
        expect(action.payload.content).toEqual('Unexpected end of JSON input [OK]');
      });
  });

  it('should return notification with default message', () => {
    return errorHandler(new Response('{}'))
      .toPromise()
      .then(action => {
        expect(action.type).toEqual(getType(pushNotification));
        expect(action.payload.content).toEqual(defaultMessage);
      });
  });
});
