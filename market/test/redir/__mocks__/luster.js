const info = jest.fn((logString) => logString).mockName('info method');

const logWinston = {
    redir: {
        info,
    },
};

module.exports = {
    logWinston,
};
