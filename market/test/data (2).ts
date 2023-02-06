export const absoluteValues = {
  statement_covered: 440,
  statement_total: 793,
  statement_skipped: 0,

  branches_covered: 71,
  branches_total: 231,
  branches_skipped: 0,

  functions_covered: 118,
  functions_total: 280,
  functions_skipped: 0,

  lines_covered: 412,
  lines_total: 750,
  lines_skipped: 0,
};

export const GOOD_COVERAGE = {
  id: '1',
  branch: 'mater',
  pull_request: '',
  project: 'ir-ui',
  subproject: '',
  changed: new Date().toDateString(),
  statements_coverage: 65.49,
  branches_coverage: 60.74,
  functions_coverage: 62.14,
  lines_coverage: 61.93,
  ...absoluteValues,
};

export const LOW_COVERAGE = {
  id: '1',
  branch: 'mater',
  pull_request: '',
  project: 'ir-ui',
  subproject: '',
  changed: new Date().toDateString(),
  statements_coverage: 35.49,
  branches_coverage: 30.74,
  functions_coverage: 32.14,
  lines_coverage: 31.93,
  ...absoluteValues,
};
