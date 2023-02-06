module.exports = () => ({
    visitor: {
        Program(path: any) {
            path.scope.rename('__webpack_require__', 'require');
            path.scope.rename('__non_webpack_require__', 'require');
        },
    },
});
