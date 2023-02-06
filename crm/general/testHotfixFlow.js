const { calculateVersion } = require('../jobs/calculateVersion.js');
const { getIssues } = require('../jobs/getIssues.js');
const { updateTickets } = require('../jobs/updateTickets.js');
const { build } = require('../jobs/build.js');
const { deployToMds } = require('../jobs/deployToMds.js');
const { updateDeployStage } = require('../jobs/updateDeployStage.js');
const { mockCommonMdsBucket, mockStages } = require('../common.js');

const testHotfixFlow = {
  title: 'Static TEST hotfix',
  jobs: {
    'calculate-version': {
      ...calculateVersion,
      stage: 'build',
    },
    build: {
      ...build,
      needs: 'calculate-version',
    },
    'deploy-to-mds': {
      ...deployToMds(mockCommonMdsBucket),
      stage: 'deploy-to-mds',
      needs: 'build',
    },
    hotfix: {
      ...updateDeployStage('Deploy to Hotfix', mockStages.hotfixStage, mockCommonMdsBucket),
      needs: 'deploy-to-mds',
      stage: 'hotfix-update',
    },
    'get-issues': {
      ...getIssues,
      needs: 'hotfix',
    },
    'update-tickets': {
      ...updateTickets,
      needs: 'get-issues',
    },
    prod: {
      ...updateDeployStage('Deploy to Prod', mockStages.prodStage, mockCommonMdsBucket, 'yes'),
      needs: 'update-tickets',
      manual: true,
      stage: 'prod',
    },
  },
};

module.exports = { testHotfixFlow };
