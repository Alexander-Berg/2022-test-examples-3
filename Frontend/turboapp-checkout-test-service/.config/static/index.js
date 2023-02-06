const prNumber = process.env.TRENDBOX_PULL_REQUEST_NUMBER || '';
const branchName = process.env.TRENDBOX_BRANCH || '';
const headCommitSha = process.env.HEAD_COMMIT_SHA || '';

const service = 'checkout-test-service';

module.exports = {
    bucket: 'tap-test',
    service,
    static: {
        path: 'build',
        sources: ['**/*', '!**/*.LICENSE'],
        target: getTargetDir(),
        overwrite: true,
    },
    s3: {
        accessKeyId: process.env.TAP_S3_ACCESS_KEY_ID,
        secretAccessKey: process.env.TAP_S3_SECRET_ACCESS_KEY,
    },
};

function getTargetDir() {
    if (prNumber) {
        return [service, `pr-${prNumber}`].join('/');
    }

    if (branchName && headCommitSha) {
        return [service, branchName, headCommitSha].join('/');
    }

    throw new Error('Не удалось определить директорию для заливки статики');
}
