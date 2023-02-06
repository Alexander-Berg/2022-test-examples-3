import {gluePathname} from './glue-pathname';

test('head + tail', function () {
    expect(gluePathname('head', 'tail')).toBe('/head/tail');
});

test('head + tail/', function () {
    expect(gluePathname('head', 'tail/')).toBe('/head/tail/');
});

test('head + /tail', function () {
    expect(gluePathname('head', '/tail')).toBe('/head/tail');
});

test('head + /tail/', function () {
    expect(gluePathname('head', '/tail/')).toBe('/head/tail/');
});

test('head/ + tail', function () {
    expect(gluePathname('head/', 'tail')).toBe('/head/tail');
});

test('head/ + tail/', function () {
    expect(gluePathname('head/', 'tail/')).toBe('/head/tail/');
});

test('head/ + /tail', function () {
    expect(gluePathname('head/', '/tail')).toBe('/head/tail');
});

test('head/ + /tail/', function () {
    expect(gluePathname('head/', '/tail/')).toBe('/head/tail/');
});

test('/head + tail', function () {
    expect(gluePathname('/head', 'tail')).toBe('/head/tail');
});

test('/head + tail/', function () {
    expect(gluePathname('/head', 'tail/')).toBe('/head/tail/');
});

test('/head + /tail', function () {
    expect(gluePathname('/head', '/tail')).toBe('/head/tail');
});

test('/head + /tail/', function () {
    expect(gluePathname('/head', '/tail/')).toBe('/head/tail/');
});

test('/head/ + tail', function () {
    expect(gluePathname('/head/', 'tail')).toBe('/head/tail');
});

test('/head/ + tail/', function () {
    expect(gluePathname('/head/', 'tail/')).toBe('/head/tail/');
});

test('/head/ + /tail', function () {
    expect(gluePathname('/head/', '/tail')).toBe('/head/tail');
});

test('/head/ + /tail/', function () {
    expect(gluePathname('/head/', '/tail/')).toBe('/head/tail/');
});

test('head + /', function () {
    expect(gluePathname('head', '/')).toBe('/head/');
});

test('head/ + /', function () {
    expect(gluePathname('head/', '/')).toBe('/head/');
});

test('/ + tail', function () {
    expect(gluePathname('/', 'tail')).toBe('/tail');
});

test('/ + /tail', function () {
    expect(gluePathname('/', '/tail')).toBe('/tail');
});

test('"" + ""', function () {
    expect(gluePathname('', '')).toBe('/');
});

test('/head/ + ""', function () {
    expect(gluePathname('/head/')).toBe('/head/');
});

test('/head + ""', function () {
    expect(gluePathname('/head')).toBe('/head');
});

test('/head + "/"', function () {
    expect(gluePathname('/head', '/')).toBe('/head/');
});

test('/head/ + "/"', function () {
    expect(gluePathname('/head/', '/')).toBe('/head/');
});

test('"" + "tail"', function () {
    expect(gluePathname('', 'tail')).toBe('/tail');
});

test('/ + "/tail"', function () {
    expect(gluePathname('/', '/tail')).toBe('/tail');
});
