def test_with_aqua(name, pack_id, scope) {
    freeStyleJob('test-with-aqua-'+name+'-'+scope) {
        displayName('Запуск тестов AQuA на Ubuntu для '+name)
        description("""Эта джоба сгенерирована автоматически.
                Тестируем в окружении """ + scope + """
                <br/>
                <h2>Время прогона</h2>
                <br/>"""+
                '<img src="https://common.jenkins.mail.yandex.net/job/test-with-aqua-'+name+'-'+scope+'/buildTimeGraph/png" />')

        logRotator(100, 200)

        label('maildev')

        parameters {
            stringParam('BRANCH', 'trunk')
            stringParam('JIRA_TASKS', '')

            stringParam('HOST_AKITA', 'akita-test.mail.yandex.net')
            stringParam('HOST_BARBET', 'barbet-test.mail.yandex.net')
            stringParam('HOST_SPANIEL', 'spaniel-test.mail.yandex.net')
            stringParam('HOST_HOUND', 'meta-test.mail.yandex.net')
            stringParam('HOST_SENDBERNAR', 'sendbernar-test.mail.yandex.net')
            stringParam('HOST_MOPS', 'mops-test.mail.yandex.net')
            stringParam('HOST_RETRIEVER', 'webattach-test.mail.yandex.net')
            stringParam('HOST_MBODY', 'mbody-test.mail.yandex.net')
            stringParam('HOST_MBODY_B2B', 'mbody-test.mail.yandex.net')

            stringParam('PORT_AKITA', '80')
            stringParam('PORT_BARBET', '80')
            stringParam('PORT_SPANIEL', '80')
            stringParam('PORT_HOUND', '80')
            stringParam('PORT_SENDBERNAR', '80')
            stringParam('PORT_MOPS', '80')
            stringParam('PORT_RETRIEVER', '80')
            stringParam('PORT_MBODY', '80')
            stringParam('PORT_MBODY_B2B', '80')

            stringParam('ZK_ROOT_NODE', '')
            stringParam('PACKAGE_VERSION', '')
        }

        concurrentBuild(true)

        wrappers {
            timestamps()
            preBuildCleanup()
            buildName('${BRANCH}')
            colorizeOutput('xterm')

            environmentVariables {
                env('CURRENT_DOG', name)
            }
        }

        steps {
            shell('''
BRANCH_ESCAPED=`tr "#" "_" <<<"$BRANCH"`
echo "NAME="`tr "/" "_" <<<"$BRANCH_ESCAPED"` > name.props

DOG=`echo "$CURRENT_DOG" | sed 's|async||g' | sed 's|b2b||g'`
N="HOST_$DOG"
N=`printf '%s\n' "$N" | awk '{ print toupper($0) }'`
echo "TARGET_HOST=${!N}" >> name.props

if fgrep 'mail.yandex.net' -q <<<"${!N}"; then
    echo "SSH_IGNORE=true" >> name.props
else
    echo "SSH_IGNORE=false"  >> name.props
fi
            ''')
            environmentVariables {
                propertiesFile('name.props')
            }

            buildDescription('', '${NAME} ${TARGET_HOST}')
            aqua {
                timeout(15, 'MINUTES')
                packs {
                    pack(pack_id) {
                        parallel()

                        tag('${NAME}')
                        threshold(60)

                        prop('testing.scope', scope)

                        prop('ssh_tests.ignore', '${SSH_IGNORE}')

                        prop('hound.host', 'http://$HOST_HOUND')
                        prop('akita.host', 'http://$HOST_AKITA')
                        prop('barbet.host', 'http://$HOST_BARBET')
                        prop('spaniel.host', 'http://$HOST_SPANIEL')
                        prop('sendbernar.host', 'http://$HOST_SENDBERNAR')
                        prop('mops.host', 'http://$HOST_MOPS')
                        prop('mbody.host', 'http://$HOST_MBODY')
                        prop('mbody.b2b.host', 'http://$HOST_MBODY_B2B')

                        prop('webattach.host', 'http://$HOST_RETRIEVER')

                        prop('hound.port', '$PORT_HOUND')
                        prop('akita.port', '$PORT_AKITA')
                        prop('barbet.port', '$PORT_BARBET')
                        prop('spaniel.port', '$PORT_SPANIEL')
                        prop('sendbernar.port', '$PORT_SENDBERNAR')
                        prop('mops.port', '$PORT_MOPS')
                        prop('mbody.port', '$PORT_MBODY')
                        prop('mbody.b2b.port', '$PORT_MBODY_B2B')

                        prop('zookeeper.root', '$ZK_ROOT_NODE')
                    }
                }
            }

            publishers {
                updateStartrekIssues {
                    byIssuesSeparated('${JIRA_TASKS}', '\\s')
                    addComment('Тесты на **\${TARGET_HOST}** версию **\${PACKAGE_VERSION}** завершились\n'+
                "((\${URL_AQUA_0} \${NAME_AQUA_0})) - OK: !!(green)**\${SUCCESS_AQUA_0}**!!, ПОТРАЧЕНО: !!**\${FAIL_AQUA_0}**!!\n")
                }
            }
        }
    }
}

sendbernar = '585d2973e4b04506a0ac5ec2'
hound = '5a980bfe6412986e2b6e9c8a'
akita = '5ae3220e4a94db72c9e80a68'
barbet = '605dcc9f8a90fe6cefeb3bc5'
spaniel = '612520fd8a90449c9e78d3b9'
mops = '5a2698836412ad01a99e2021'
asyncmops = '5dae047e8a900e03944427e5'
retriever = '550c4201e4b0fb0c29be7632'
mbody = '5af9368d4a94db72c9ecae25'
mbodyb2b = '51a8a62684ae47ee5cb9756d'

test_with_aqua('sendbernar', sendbernar, 'test')
test_with_aqua('hound', hound, 'test')
test_with_aqua('akita', akita, 'test')
test_with_aqua('barbet', barbet, 'test')
test_with_aqua('spaniel', spaniel, 'test')
test_with_aqua('mops', mops, 'test')
test_with_aqua('asyncmops', asyncmops, 'test')
test_with_aqua('retriever', retriever, 'test')
test_with_aqua('mbody', mbody, 'test')
test_with_aqua('mbodyb2b', mbodyb2b, 'test')

test_with_aqua('sendbernar', sendbernar, 'pg')
test_with_aqua('hound', hound, 'pg')
test_with_aqua('akita', akita, 'pg')
test_with_aqua('barbet', barbet, 'pg')
test_with_aqua('mops', mops, 'pg')
test_with_aqua('asyncmops', asyncmops, 'pg')
test_with_aqua('retriever', retriever, 'pg')
test_with_aqua('mbody', mbody, 'pg')

freeStyleJob('test-with-aqua-xeno-mops') {
    displayName('Запуск тестов AQuA на Ubuntu для Xeno Mops')
    description("""Эта джоба сгенерирована автоматически.
            Тестируем в тестовом окружении
            <br/>
            <h2>Время прогона</h2>
            <br/>"""+
            '<img src="https://common.jenkins.mail.yandex.net/job/test-with-aqua-xeno-mops/buildTimeGraph/png" />')

    logRotator(100, 200)

    label('maildev')

    concurrentBuild(true)

    wrappers {
        timestamps()
        preBuildCleanup()
        buildName('default_mops_xeno')
        colorizeOutput('xterm')
    }

    steps {
        buildDescription('', 'default_mops_xeno')
        aqua {
            timeout(15, 'MINUTES')
            packs {
                pack('5c6bc5da8a903c18051f9fd2') {
                    parallel()

                    tag('default_mops_xeno')
                    threshold(60)

                    prop('testing.scope', 'test')

                    prop('xeno.use_accounts', 'true')
                    prop('mailboxes.yaml.file', 'accounts-web-xeno.yaml')
                }
            }
        }
    }
}

freeStyleJob('test-with-aqua-settings') {
    displayName('Запуск тестов AQuA на Ubuntu для settings')
    description("""Эта джоба сгенерирована автоматически.
<br/>""")

    logRotator(100, 200, 100, 200)

    parameters {
        stringParam('TARGET_TESTING_HOST', 'settings-1.settings.testing.settings.mail.stable.qloud-d.yandex.net', 'Целевой хост в окружении testing')
        stringParam('JIRA_TASKS', '', 'Список задач через пробел, например "WMI-111 DARIA-222 WMI-333"')
        stringParam('TAG', '', 'Версия Docker-образа или любая строчка для группировки запуска тестов')
    }

    concurrentBuild(false)

    wrappers {
        timestamps()
        preBuildCleanup()
        buildName('${TAG} : ${TARGET_TESTING_HOST}')
        colorizeOutput('xterm')
    }

    steps {
        aqua {
            timeout(10, 'MINUTES')
            parallel()
            packs {
                pack('519cfd8084ae4395d8f33c0c') {
                    tag('${TAG}')
                    threshold(30)
                    prop('akita.host', 'http://akita-test.mail.yandex.net')
                    prop('akita.port', '80')
                    prop('passport.host', 'https://passport-test.yandex.ru/')
                    prop('settings.accounts', 'accounts-web-test.yml')
                    prop('settings.scope', 'test')
                    prop('settings.uri', 'http://${TARGET_TESTING_HOST}')
                }
            }
        }

        publishers {
            updateStartrekIssues {
                byIssuesSeparated('${JIRA_TASKS}', '\\s')
                addComment("Тесты на **\${TARGET_TESTING_HOST}** (testing) завершились\\n" +
            "((\${URL_AQUA_0} \${NAME_AQUA_0})) - OK: !!(green)**\${SUCCESS_AQUA_0}**!!, ПОТРАЧЕНО: !!**\${FAIL_AQUA_0}**!!\\n")
            }
        }
    }
}

freeStyleJob('test-with-aqua-transversal') {
    displayName('Запуск сквозных тестов AQuA')
    label('maildev')

    parameters {
        stringParam('STAND')
        stringParam('ACCOUNTS', 'accounts-smoke-transversal')
    }

    wrappers {
        timestamps()
        preBuildCleanup()
        colorizeOutput('xterm')
    }

    steps {
        aqua {
            timeout(30, 'MINUTES')
            packs {
                pack('5cdc197e8a903c1805202113') {
                    tag('transversal')

                    prop('webdriver.base.url', 'https://stand-${STAND}.verstka-qa.mail.yandex.ru')
                    prop('description', 'Сквозной тест')
                    prop('accounts.json.file', '${ACCOUNTS}.json')
                }
            }
        }
    }
}
