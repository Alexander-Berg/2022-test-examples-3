module.exports = function yaCheckCSPViolations() {
    return this
        .execute(function() {
            return window.hermione && window.hermione.cspViolations;
        })
        .then(data => {
            const violations = data.value;

            if (violations && violations.length) {
                throw new Error(`Найдены заблокированные ресурсы CSP: \n\n ${violations.map(JSON.stringify).join('\n')} \n`);
            }
        });
};
