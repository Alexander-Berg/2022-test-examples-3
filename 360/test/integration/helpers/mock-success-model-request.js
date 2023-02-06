import nock from 'nock';

export default function successModelRequest(models, data = {}) {
  return nock(window.location.origin)
    .post('/api/models')
    .query({_models: models})
    .reply(200, {
      models: [
        {
          name: models,
          data: data,
          status: 'ok'
        }
      ]
    });
}
