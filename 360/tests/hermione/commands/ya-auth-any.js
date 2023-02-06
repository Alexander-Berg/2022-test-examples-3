const GROUP_OF_LOGINS = [
  'e2e-fix-01',
  'e2e-fix-02',
  'e2e-fix-03',
  'e2e-fix-04',
  'e2e-fix-05',
  'e2e-fix-06',
  'e2e-fix-07',
  'e2e-fix-08',
  'e2e-fix-09',
  'e2e-fix-10'
];

/**
 * @param {string} [groupLogin]
 * @param {Object} [options]
 * @returns {Promise<any>}
 */
module.exports = function yaAuthAny(
  groupLogin = GROUP_OF_LOGINS,
  options = {}
) {
  return this.authAny(groupLogin, options);
};
