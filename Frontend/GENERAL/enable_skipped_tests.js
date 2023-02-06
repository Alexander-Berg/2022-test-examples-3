const { execSync } = require('child_process');

const options = {
    stdio: 'inherit',
};

execSync(`
    BRANCH=$(node -p "Object($checkout_config).base.branch");
    npx testcop-cli enable --projects "intrasearch-www" --branch "$BRANCH" --pull-request-number "$TRENDBOX_PULL_REQUEST_NUMBER"
`, options);
