multiJob('test-smtpgate-packages') {
    displayName('Мультиджоба для тестирования релизов доставки')
    description("""Эта джоба сгенерирована автоматически.
            <br/>
            <h2>Время прогона</h2>
            <br/>
            <img src="https://common.jenkins.mail.yandex.net/job/test-smtpgate-packages/buildTimeGraph/png" />""")

    label('maildev')

    logRotator(30, 200, 30, 200)

    parameters {
        stringParam('PACKAGE_VERSION', '', 'Версия пакетов доставки, нужно для отображения в интерфейсе')
        stringParam('HOST_SMTPGATE', 'https://mxback-unstable.mail.yandex.net:2443', 'Хост доставки с новой версией пакетов')
        stringParam('JIRA_TASKS', '', 'Куда отписаться о результате прогона тестов')
        choiceParam('ENVIRONMENT_TYPE', ['testing', 'prestable'])
    }

    environmentVariables {
        env('VCS', 'svn')
        env('BRANCH', 'trunk')
    }

    concurrentBuild(true)

    wrappers {
        timestamps()
        preBuildCleanup()
        buildName('$BRANCH')
        colorizeOutput('xterm')
    }

    steps {
        conditionalSteps {
            condition {
                stringsMatch('${ENVIRONMENT_TYPE}', 'testing', false)
            }
            steps {
                buildDescription('', '$PACKAGE_VERSION')

                phase('INSTALL') {
                    phaseJob('launch-smtpgate-containers') {
                        parameters {
                            currentBuild()
                        }
                    }
                }

                copyArtifacts('launch-smtpgate-containers') {
                    buildSelector {
                        latestSuccessful(true)
                    }
                }

                environmentVariables {
                    propertiesFile('hosts.props')
                }

                phase('TEST') {
                    phaseJob('test-with-aqua-smtpgate') {
                        killPhaseCondition('NEVER')
                        parameters {
                            currentBuild()
                            propertiesFile('hosts.props')
                        }
                    }
                }
            }
        }
        conditionalSteps {
            condition {
                stringsMatch('${ENVIRONMENT_TYPE}', 'prestable', false)
            }
            steps {
                buildDescription('', '$PACKAGE_VERSION')

                phase('INSTALL') {
                    phaseJob('launch-smtpgate-containers-prestable') {
                        parameters {
                            currentBuild()
                        }
                    }
                }

                copyArtifacts('launch-smtpgate-containers-prestable') {
                    buildSelector {
                        latestSuccessful(true)
                    }
                }

                environmentVariables {
                    propertiesFile('hosts.props')
                }

                phase('TEST') {
                    phaseJob('test-with-aqua-smtpgate') {
                        killPhaseCondition('NEVER')
                        parameters {
                            currentBuild()
                            propertiesFile('hosts.props')
                        }
                    }
                }
            }
        }
    }
}
