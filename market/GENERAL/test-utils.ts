export function getNumValue(value: number | string) {
  return { numericValue: String(value) };
}

export function getStringValue(value: string) {
  return { stringValue: [{ value, isoCode: 'ru' }] };
}
