module.exports = {
    /**
     * @param {Object} testRun
     * @returns {Object[]}
     */
    getRunCases(testRun) {
        let testCases = [];

        for (let group of testRun.testGroups) {
            for (let testCase of group.testCases) {
                testCases.push(testCase);
            }
        }

        return testCases;
    },

    /**
     * Извлекает testCaseID из названия кейса или его родителя
     * @param {Object} suite
     * @param {String} project
     * @returns {Number|undefined}
     */
    getTestCaseId(suite, project) {
        let id = this.parseId(suite.testpalmId, project);

        if (!id) {
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
        str = String(str);

        if (str.indexOf(project) === 0) {
            let id = Number(str.slice(project.length + 1));
            if (isFinite(id)) {
                return id;
            }
        }
    },
};
