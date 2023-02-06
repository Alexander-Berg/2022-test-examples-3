module.exports = async function yaCheckCSPViolations() {
    const violations = await this.execute(function () {
        // @ts-ignore
        return window.hermione?.cspViolations;
    });

    if (violations?.length) {
        throw new Error(`Найдены заблокированные ресурсы CSP: \n\n ${violations.map(JSON.stringify).join('\n')} \n`);
    }
};
