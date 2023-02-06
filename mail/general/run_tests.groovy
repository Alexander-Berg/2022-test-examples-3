freeStyleJob('run-tests') {
    description('Запуск тестов (ut и интеграционные)')

    label('maildev')

    logRotator(-1, 10)

    parameters {
        stringParam('JIRA_TASKS', '', 'Jira task numbers, separated by space. For example: "MPROTO-1991"')
        stringParam('REVIEW_ID', '', 'Arcanum review id, e.g. 2018132')
        stringParam('REVISION', 'HEAD', 'Arcanum revision, e.g. 8569782')

        booleanParam('RUN_SMALL', false, 'Run small tests')
        booleanParam('RUN_MEDIUM', false, 'Run medium tests')
        booleanParam('RUN_LARGE', false, 'Run large tests')
        stringParam('RUN_LARGE_TEST_TAGS', '', """Large tests test tags (space is a delimiter)<br/>For nwsmtp "manual infra"<br/>For notsolitesrv "" """)

        choiceParam('PROJECT', ['nwsmtp', 'notsolitesrv'])
    }

    concurrentBuild(true)

    wrappers {
        buildName('[\${JIRA_TASKS}] run tests')
        sshAgent('robot-gerrit-ssh-key')
        preBuildCleanup()
        timestamps()
    }

    environmentVariables {
        env('USERNAME', 'robot-gerrit')
        env('BUILD_RESULT', '!!(red)fail!!')
        env('PACKAGE_VERSION', '')
    }

    steps {
        shell(readFileFromWorkspace('run_tests.sh'))
        environmentVariables {
            propertiesFile('result')
        }
    }

    publishers {
        updateStartrekIssues {
            byIssuesSeparated('${JIRA_TASKS}', '\\s')
            addComment("\${RESULT}\r\n((https://common.jenkins.mail.yandex.net/job/run-tests/\${BUILD_ID}/console console))")
        }
    }
}
