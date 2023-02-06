const PROMOS_BASE_PATH = '/promos';
const REDIRECT_BY_ANAPLAN_URL_BASE_PATH = `${PROMOS_BASE_PATH}/redirect-by-anaplan-id`;

export function getRedirectByAnaplanIdUrl<P>(browser: WebdriverIO.Client<P>, anaplanId: string) {
  return `${browser.options.baseUrl}/#${REDIRECT_BY_ANAPLAN_URL_BASE_PATH}/${encodeURIComponent(anaplanId)}`;
}

export function getPromoPageUrl<P>(browser: WebdriverIO.Client<P>, promoId: string, query: string | null = null) {
  return `${browser.options.baseUrl}/#${PROMOS_BASE_PATH}/${promoId}${getQueryPartOrEmpty(query)}`;
}

export function getPromosPageUrl<P>(browser: WebdriverIO.Client<P>): string {
  return `${browser.options.baseUrl}/#${PROMOS_BASE_PATH}`;
}

function getQueryPartOrEmpty(query: string) {
  return query ? `?${query}` : '';
}

export function makeQueryPartWithSsku(sskus: string[]) {
  return sskus.map(sskuId => `sskuIds=${sskuId}`).join('&');
}
