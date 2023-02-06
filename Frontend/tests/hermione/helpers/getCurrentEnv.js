module.exports = context => {
    const { file } = context.currentTest;

    return file.match(/suites\/(\w+)\//)[1];
};
