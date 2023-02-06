const testFrontendStaticRelease = {
  title: '(TEST)STATIC release',
  flow: 'test-release-flow',
  'hotfix-flows': 'test-hotfix-flow',
  'start-version': 110,
  description: 'Тестовый релиз.\n[Документация CI](https://docs.yandex-team.ru/ci/)  \n',
  branches: {
    pattern: `releases/experimental/crm_frontend_static/release-2.\${version}.0`,
    'forbid-trunk-releases': true,
  },
  stages: [
    { id: 'build', title: 'Calculate Version and Build', displace: true },
    { id: 'deploy-to-mds', title: 'Deploy To MDS', displace: true },
    { id: 'testing', title: 'Update Testing Stage', displace: true },
    { id: 'update-tickets', title: 'Update tickets', displace: true },
    { id: 'mars', title: 'Update Mars Stage', displace: true },
    { id: 'hotfix-update', title: 'Update Hotfix Stage', displace: true },
    { id: 'prod', title: 'Update Prod Stage', displace: true },
    { id: 'hotfix', title: 'Update Hotfix Stage', displace: true },
    { id: 'update-last-success-version', title: 'Update last success version', displace: true },
  ],
};
module.exports = { testFrontendStaticRelease };
