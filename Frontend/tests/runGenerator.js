const yeoman = require('yeoman-environment');
const { AutoAdapter } = require('yeoman-automation-adapter');

const answers = {
    inMonorepo: false,
    projectName: 'generator-3000',
    tankerProject: 'generator-3000',
    s3Path: 'generator-3000',
    dockerTag: 'generator-3000',
};

const silent = false;
const adapter = new AutoAdapter(answers, silent);
const env = yeoman.createEnv([], {}, adapter);

env.run(require.resolve('../generators/nest'));
