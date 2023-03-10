// Makes the script crash on unhandled rejections instead of silently
// ignoring them. In the future, promise rejections that are not handled will
// terminate the Node.js process with a non-zero exit code.
process.on('unhandledRejection', (err) => {
    throw err;
});

const argv = process.argv.slice(2);

argv.push(
    '--env=jsdom',
    '--projects=.',
    // allow console.*
    '--verbose',
);

// Watch unless on CI or in coverage mode
if (!process.env.CI && argv.indexOf('--coverage') < 0) {
    argv.push('--watch');
}

require('jest').run(argv);
