import * as http from 'http';

export function httpGetJSONRequest<TResult>(requestUrl): Promise<TResult> {
  return new Promise((resolve, reject) => {
    http.get(requestUrl, (res) => {
      let body = '';

      res.on('data', (chunk) => {
        body += chunk;
      });

      res.on('end', () => {
        try {
          const json: TResult = JSON.parse(body);
          resolve(json);
        } catch (error) {
          reject(error.message);
        }
      });
    });
  });
}

export function createGetActivePromosUrl(offer) {
  return `http://datacamp.blue.tst.vs.market.yandex.net/shops/${offer.supplierId}/offers?offer_id=${offer.ssku}&whid=${offer.warehouseId}&format=json`
}
