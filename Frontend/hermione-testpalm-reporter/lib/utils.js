module.exports = {
    /**
     * @param {Object} testRun
     * @returns {Object[]}
     */
    getRunCases(testRun) {
        const testCases = [];

        for (const group of testRun.testGroups) {
            for (const testCase of group.testCases) {
                testCases.push(testCase);
            }
        }

        return testCases;
    },

    /**
     * @param {Object} suite
     * @param {String} project
     * @returns {Number|undefined}
     */
    getTestCaseId(suite, project) {
        let id = this.parseId(suite.testpalmId, project);

        if (!id && suite.title) {
            id = this.parseId(suite.title.split(':')[0], project);
        }

        if (id) {
            return id;
        }

        if (suite.parent) {
            return this.getTestCaseId(suite.parent, project);
        }
    },

    /**
     * @param {String} str
     * @param {String} project
     * @returns {Number|undefined}
     */
    parseId(str, project) {
        str = str || '';
        if (str.indexOf(project) === 0) {
            const id = Number(str.slice(project.length + 1));
            if (isFinite(id)) {
                return id;
            }
        }
    },

    /**
     * @param {Object} suite
     * @returns {Object[]}
     */
    getAllSuites(suite) {
        return (suite.suites || [])
            .map(child => this.getAllSuites(child))
            .reduce((suites, children) => {
                return suites.concat(children);
            }, [suite]);
    },

    /**
     * @param {String} text
     * @returns {String}
     */
    ident(text) {
        return text
            .split('\n')
            .map(line => `    ${line}`)
            .join('\n');
    },
};
