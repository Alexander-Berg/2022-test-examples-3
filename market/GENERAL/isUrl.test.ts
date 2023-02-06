import { isUrl } from './isUrl';

test('test isUrl', () => {
  expect(isUrl('https://www.example.com')).toBeTruthy();
  expect(isUrl('http://www.example.com')).toBeTruthy();
  expect(isUrl('http://invalid.com/perl.cgi?key= | http://web-site.com/cgi-bin/perl.cgi?key1=value1&key2')).toBeFalsy();
  expect(isUrl('https://www.example.com, https://www.example.com, https://www.example.com')).toBeFalsy();
  expect(isUrl('try2.MARKETPARTNER-15344')).toBeFalsy();
});
