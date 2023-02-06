multiJob('test-webmail-packages') {
    displayName('Мультиджоба для установки пакетов и запуска тестов')
    description("""Эта джоба сгенерирована автоматически.
            <br/>
            <h2>Время прогона</h2>
            <br/>
            <img src="https://common.jenkins.mail.yandex.net/job/test-webmail-packages/buildTimeGraph/png" />""")

    label('maildev')

    logRotator(30, 200, 30, 200)

    parameters {
        stringParam('VCS', 'svn')
        stringParam('BRANCH', 'trunk')
        stringParam('PROJECTS', '')
        stringParam('PACKAGE_VERSION', '')
        stringParam('JIRA_TASKS', '')
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

        phase('INSTALL') {
            phaseJob('launch-webmail-containers') {
                parameters {
                    currentBuild()
                }
            }
        }

        copyArtifacts('launch-webmail-containers') {
            buildSelector {
                latestSuccessful(true)
            }
        }

        environmentVariables {
            propertiesFile('hosts.props')
        }

        phase('TEST') {
            phaseJob('test-with-aqua') {
                killPhaseCondition('NEVER')
                parameters {
                    currentBuild()
                    propertiesFile('hosts.props')
                }
            }
        }
    }
}

multiJob('test-webmail-transversal') {
    displayName('Мультиджоба для поднятия стенда верстки и запуска смоуков')
    label('maildev')

    parameters {
        stringParam('STAND')
        stringParam('ACCOUNTS', 'accounts-smoke-wmiprodmonitoring')
        stringParam('HOST_AKITA', 'https://akita.mail.yandex.net:443')
        stringParam('HOST_HOUND', 'https://meta.mail.yandex.net:443')
        stringParam('HOST_SENDBERNAR', 'https://sendbernar.mail.yandex.net:443')
        stringParam('HOST_MOPS', 'https://mops.mail.yandex.net:443')
        stringParam('HOST_MBODY', 'https://mbody.mail.yandex.net:443')
        stringParam('HOST_UNSUBSCRIBE_FURITA', 'https://furita.mail.yandex.net:443/unsubscribe')
        stringParam('HOST_TAKSA', 'http://taksa-prod.search.yandex.net:80')
        stringParam('HOST_SETTINGS', 'https://settings.mail.yandex.net:443')
        stringParam('HOST_SANITIZER', 'https://sanitizer2.mail.yandex.net:443')
        stringParam('HOST_RECIPIENTS', 'http://catdog.mail.yandex.net:80')
        stringParam('HOST_POSTMASTER', 'http://postmaster.mail.yandex.net:2000/wmi/event')
        stringParam('HOST_AVA', 'http://ava-l7.mail.yandex.net')
        stringParam('HOST_DIRECTORY', 'https://api-internal.directory.ws.yandex.net/v7')
        stringParam('HOST_FURITA', 'https://furita.mail.yandex.net:443')
        stringParam('HOST_PDD', 'http://setter.mail.yandex.net')
    }

    wrappers {
        timestamps()
        preBuildCleanup()
        colorizeOutput('xterm')
    }

    steps {
        phase('DEPLOY') {
            phaseJob('launch-mail-frontend') {
                parameters {
                    currentBuild()
                }
            }
        }
        phase('TEST') {
            phaseJob('test-with-aqua-transversal') {
                parameters {
                    currentBuild()
                }
            }
        }
    }
}
