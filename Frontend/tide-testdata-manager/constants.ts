const urlChars = '\\w%.@\\/-';

// Демо: https://regex101.com/r/8jaPTt/1
export const FULL_URL_REGEXP_STRING = `https?:\\/(\\/[\\w%.@-]+)+\\/?(\\?(([${urlChars}]+(=[${urlChars}]+)?)?&)*(([${urlChars}]+(=[${urlChars}]+)?)?))?(#[${urlChars}]*)?`;

// Демо: https://regex101.com/r/vVgBYT/1
export const PATH_URL_REGEXP_STRING = `(\\/[\\w%.@-]+)+\\/?(\\?(([${urlChars}]+(=[${urlChars}]+)?)?&)*(([${urlChars}]+(=[${urlChars}]+)?)?))?(#[${urlChars}]*)?`;

// Демо: https://regex101.com/r/5uBylc/1
export const QUERY_URL_REGEXP_STRING = `\\??(([${urlChars}]+=[${urlChars}]+)?&)*(([${urlChars}]+=[${urlChars}]+))(#[${urlChars}]*)?`;

export const ANY_NODE = Symbol('ANY_NODE');
