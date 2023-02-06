import {buildBreadcrumbs, filterTraces, getTraceDescriptionWithFormat, parseQuery} from '../traceUtils';

const expect = require('expect.js');

describe('Test breadcrumbs', () => {
  it('should return breadcrumbs for trace with seq', () => {
    const traceId = {timestampMillis: 10000, hash: 'aaaabbbbcccc', seq: [1, 22, 333]};
    const expected = [
      {title: 10000},
      {title: 'aaaabbbbcccc', url: '/trace/10000/aaaabbbbcccc'},
      {title: '1', url: '/trace/10000/aaaabbbbcccc/1'},
      {title: '22', url: '/trace/10000/aaaabbbbcccc/1/22'},
      {title: '333', url: '/trace/10000/aaaabbbbcccc/1/22/333'},
    ];

    expect(buildBreadcrumbs(traceId)).to.eql(expected);
  });

  it('should return breadcrumbs for trace without seq', () => {
    const traceId = {timestampMillis: 10000, hash: 'aaaabbbbcccc', seq: []};
    const expected = [
      {title: 10000},
      {title: 'aaaabbbbcccc', url: '/trace/10000/aaaabbbbcccc'},
    ];

    expect(buildBreadcrumbs(traceId)).to.eql(expected);
  });
});

describe('Filter traces', () => {
  const traces = [
    { type: 'IN' },
    { type: 'OUT' },
  ];

  it('should filter by exact string', () => {
    const traces = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_front_touch' },
    ];
    const filter = { sourceModule: 'market_front_desktop' };
    const expected = [
      { sourceModule: 'market_front_desktop' },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should filter by exact number', () => {
    const traces = [
      { statusCode: 200 },
      { statusCode: 500 },
    ];
    const filter = { statusCode: '200' };
    const expected = [
      { statusCode: 200 },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should filter by exact array', () => {
    const traces = [
      { idSeq: [44] },
      { idSeq: [45] },
    ];
    const filter = { idSeq: '[44]' };
    const expected = [
      { idSeq: [44] },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should filter by substring in the middle', () => {
    const traces = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_front_touch' },
      { sourceModule: 'market_report' },
    ];
    const filter =  { sourceModule: '%_front_%' };
    const expected = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_front_touch' },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should filter by substring in the beginning', () => {
    const traces = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_front_touch' },
      { sourceModule: 'market_report' },
    ];
    const filter = { sourceModule: 'market_front_%' };
    const expected = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_front_touch' },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should filter by substring in the end', () => {
    const traces = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_front_touch' },
      { sourceModule: 'market_report_desktop' },
    ];
    const filter = { sourceModule: '%_desktop' };
    const expected = [
      { sourceModule: 'market_front_desktop' },
      { sourceModule: 'market_report_desktop' },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should filter by multiple filters', () => {
    const traces = [
      { type: 'IN', sourceModule: 'market_front_desktop' },
      { type: 'OUT', sourceModule: 'market_front_desktop' },
      { type: 'IN', sourceModule: 'market_report' },
    ];
    const filter = { type: 'IN', sourceModule: 'market_front_desktop' };
    const expected = [
      { type: 'IN', sourceModule: 'market_front_desktop' },
    ];
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });

  it('should not throw on partial search by unknown property', () => {
    const traces = [
      { type: 'IN', sourceModule: 'market_front_desktop' },
    ];
    const filter = { unknownProp: '%' };
    const expected = traces;
    const actual = filterTraces(filter, traces);
    expect(actual).to.eql(expected);
  });
});

describe('Parse query', () => {
  it('should not fail on empty query', () => {
    const query = '';
    const expected = {
      filters: {},
      directives: {},
    };
    const actual = parseQuery(query);
    expect(actual).to.eql(expected);
  });

  it('should not parse invalid expression', () => {
    const query = 'not an expression';
    const expected = {
      filters: {},
      directives: {},
    };
    const actual = parseQuery(query);
    expect(actual).to.eql(expected);
  });

  it('should not parse expression with no value', () => {
    const query = 'sourceModule:';
    const expected = {
      filters: {},
      directives: {},
    };
    const actual = parseQuery(query);
    expect(actual).to.eql(expected);
  });

  it('should parse filter', () => {
    const query = 'sourceModule:market_front_desktop';
    const expected = {
      filters: { sourceModule: 'market_front_desktop' },
      directives: {},
    };
    const actual = parseQuery(query);
    expect(actual).to.eql(expected);
  });

  it('should parse directive', () => {
    const query = '!format:{{sourceModule}}';
    const expected = {
      filters: {},
      directives: { format: '{{sourceModule}}' },
    };
    const actual = parseQuery(query);
    expect(actual).to.eql(expected);
  });

  it('should parse multiple expressions', () => {
    const query = 'sourceModule:market_front_desktop !format:{{sourceModule}}';
    const expected = {
      filters: { sourceModule: 'market_front_desktop' },
      directives: { format: '{{sourceModule}}' },
    };
    const actual = parseQuery(query);
    expect(actual).to.eql(expected);
  });
});

describe('Apply format', () => {
  it('should replace placeholders with values for existing props', () => {
    const format = '{{sourceModule}}';
    const trace = { sourceModule: 'market_front_desktop' };
    const expected = 'market_front_desktop';
    const actual = getTraceDescriptionWithFormat(format, trace);
    expect(actual).to.eql(expected);
  });

  it('should replace placeholders with empty string for non-existing props', () => {
    const format = '{{sourceModule}}';
    const trace = { };
    const expected = '';
    const actual = getTraceDescriptionWithFormat(format, trace);
    expect(actual).to.eql(expected);
  });

  it('should keep everything except placeholders as is', () => {
    const format = 'Hello { world }';
    const trace = { };
    const expected = 'Hello { world }';
    const actual = getTraceDescriptionWithFormat(format, trace);
    expect(actual).to.eql(expected);
  });
});
