multiJob('test-with-aqua') {
    displayName('Мультиджоба для установки тестирования пакетов')
    description("""Эта джоба сгенерирована автоматически.
            <br/>
            <h2>Время прогона</h2>
            <br/>
            <img src="https://common.jenkins.mail.yandex.net/job/test-with-aqua/buildTimeGraph/png" />""")

    label('maildev')

    logRotator(30, 200, 30, 200)

    parameters {
        stringParam('BRANCH', 'trunk')
        stringParam('PACKAGE_VERSION', '')
        stringParam('JIRA_TASKS', '')
        stringParam('HOST_AKITA', 'akita-test.mail.yandex.net')
        stringParam('HOST_HOUND', 'meta-test.mail.yandex.net')
        stringParam('HOST_SENDBERNAR', 'sendbernar-test.mail.yandex.net')
        stringParam('HOST_MOPS', 'mops-test.mail.yandex.net')
        stringParam('HOST_RETRIEVER', 'webattach-test.mail.yandex.net')
        stringParam('HOST_MBODY', 'mbody-test.mail.yandex.net')
        stringParam('HOST_MBODY_B2B', 'mbody-test.mail.yandex.net')
        booleanParam('FORCE_RUN_MBODY_B2B', false, 'Принудительно запустить тесты для mbody b2b')
        stringParam('ZK_ROOT_NODE', '')
    }

    concurrentBuild(true)

    wrappers {
        timestamps()
        preBuildCleanup()
        buildName('$BRANCH')
        colorizeOutput('xterm')
    }

    steps {
        buildDescription('', '$PACKAGE_VERSION')

        phase('TEST WEBMAIL') {
            phaseJob('test-with-aqua-akita-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-hound-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-sendbernar-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-mops-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-asyncmops-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-retriever-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-mbody-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-mbodyb2b-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
                configure { phaseJobConfig ->
                    phaseJobConfig / enableCondition << 'true'
                    phaseJobConfig / condition << '"${BUILD_MBODY}"=="true" || ${FORCE_RUN_MBODY_B2B}'
                }
            }
        }
    }
}

multiJob('test-with-aqua-pg') {
    displayName('Мультиджоба для установки тестирования пакетов wmi в проде')
    description("""Эта джоба сгенерирована автоматически.
            <br/>
            <h2>Время прогона</h2>
            <br/>
            <img src="https://common.jenkins.mail.yandex.net/job/test-with-aqua-pg/buildTimeGraph/png" />""")

    label('maildev')

    logRotator(30, 200, 30, 200)

    parameters {
        stringParam('BRANCH', 'trunk')
        stringParam('PACKAGE_VERSION', '')
        stringParam('JIRA_TASKS', '')
        stringParam('HOST_AKITA', 'akita-qa.mail.yandex.net')
        stringParam('HOST_HOUND', 'meta-qa.mail.yandex.net')
        stringParam('HOST_SENDBERNAR', 'sendbernar-qa.mail.yandex.net')
        stringParam('HOST_MOPS', 'mops-qa.mail.yandex.net')
        stringParam('HOST_RETRIEVER', 'retriever-qa.mail.yandex.net')
        stringParam('HOST_MBODY', 'mbody-qa.mail.yandex.net')
        stringParam('ZK_ROOT_NODE', '')
    }

    concurrentBuild(true)

    wrappers {
        timestamps()
        preBuildCleanup()
        buildName('$BRANCH')
        colorizeOutput('xterm')
    }

    steps {
        buildDescription('', '$PACKAGE_VERSION')

        phase('TEST WEBMAIL') {
            phaseJob('test-with-aqua-akita-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-hound-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-sendbernar-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-mops-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-asyncmops-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-retriever-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-mbody-pg') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}

multiJob('test-webmail-production') {
    disabled()


    displayName('Джоба для прогона тестов в проде по расписанию')

    label('maildev')

    wrappers {
        timestamps()
        colorizeOutput('xterm')
    }

    triggers {
        cron('0 */1 * * *')
    }

    steps {
        phase('TEST WEBMAIL') {
            phaseJob('test-with-aqua-pg') {
                parameters {
                
                }
            }
        }
    }

    publishers {
        mailer('webmail-prod-tests@yandex-team.ru')
    }
}

multiJob('test-with-aqua-smtpgate') {
    displayName('Мультиджоба для прогона автотестов Webmail')
    description("""Эта джоба сгенерирована автоматически.
            <br/>
            <h2>Время прогона</h2>
            <br/>
            <img src="https://common.jenkins.mail.yandex.net/job/test-with-aqua-smtpgate/buildTimeGraph/png" />""")

    label('maildev')

    logRotator(30, 200, 30, 200)

    parameters {
        stringParam('BRANCH', 'trunk')
        stringParam('PACKAGE_VERSION', '')
        stringParam('JIRA_TASKS', '')
        stringParam('HOST_AKITA', 'akita-test.mail.yandex.net')
        stringParam('HOST_HOUND', 'meta-test.mail.yandex.net')
        stringParam('HOST_SENDBERNAR', 'sendbernar-test.mail.yandex.net')
        stringParam('HOST_MOPS', 'mops-test.mail.yandex.net')
        stringParam('HOST_RETRIEVER', 'webattach-test.mail.yandex.net')
        stringParam('HOST_MBODY', 'mbody-test.mail.yandex.net')
        stringParam('ZK_ROOT_NODE', '')
    }

    concurrentBuild(true)

    wrappers {
        timestamps()
        preBuildCleanup()
        buildName('$BRANCH')
        colorizeOutput('xterm')
    }

    steps {
        buildDescription('', '$PACKAGE_VERSION')

        phase('TEST WEBMAIL') {
            phaseJob('test-with-aqua-hound-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-sendbernar-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
            phaseJob('test-with-aqua-mbody-test') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}

