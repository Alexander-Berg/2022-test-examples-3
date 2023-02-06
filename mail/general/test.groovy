// **************
// HELPFUL LINKS
// **************
// https://jenkinsci.github.io/job-dsl-plugin
// https://github.yandex-team.ru/qatools/startrek-jenkins-plugin/blob/fa16a72deed91b3cab7493717a87156f63076202/README.md

def create_job = { name, envs, desc ->
freeStyleJob(name) {
  description("""
Сборка докер-образов для сборки проектов
<br/>
Эта джоба сгенерирована автоматически. <br/>
Подробности смотреть в <a href=${JOB_URL}>Seed Job</a>
<br/>""")

  label('xiva_deb')
  logRotator(14, 200)
  concurrentBuild(false)
  parameters {
      stringParam('BRANCH', '', 'ветка в репозитории, которую собирать')
      stringParam('SUFFIX', '', 'для экспериментальных версий или хотфиксов. Версия будет иметь вид: DATETIME<suffix>. Рекомендуется начинать suffix с "-"')
      booleanParam('CLEAN_WORKING_DIR', false, 'снести сборочную директорию')
  }
  environmentVariables(envs)
  
  wrappers {
      colorizeOutput('xterm')
      sshAgent('robot-gerrit-ssh-key')
    
      credentialsBinding {
          usernamePassword('DOCKER_REGISTRY_USER', 'DOCKER_REGISTRY_TOKEN', 'robot-sender-docker-registry-creds') 
      }
    
      preBuildCleanup {
          deleteDirectories()
          cleanupParameter('CLEAN_WORKING_DIR')
      }
  }
  
  scm {
      git {
          remote {
              url('git@github.yandex-team.ru:xiva/project.git')
              credentials('robot-gerrit-ssh-key')
          }
          branch('${BRANCH}')
          extensions {
              // dot not shallow clone because shallow updates are not allowed in git repos
              submoduleOptions {
                  recursive()
              }
          }
      }
  }
  steps {
      shell('''
        DT=`date "+%Y-%m-%d-%H-%M"`;
        VERSION="${DT}${SUFFIX}"
        BUILDNAME="${name}:r${VERSION}${suffix}";
        echo "VERSION=${VERSION}" > variables.txt
        echo "GITTAG=${name}-${VERSION}" > variables.txt
        echo "BUILDNAME=${BUILDNAME}" >> variables.txt
        echo "BUILDTAG=registry.yandex.net/mail/xiva/${BUILDNAME}" >> variables.txt
      ''')

      environmentVariables {
          propertiesFile('variables.txt')
      }

      shell('''
        mkdir -p build
        cd build
        export CXXFLAGS="${CXXFLAGS} --std=c++11"
        echo ${CXXFLAGS}
        cmake ../ -DCMAKE_INSTALL_PREFIX=/usr -DCMAKE_INSTALL_SYSCONFDIR=/etc -DCMAKE_INSTALL_LIBDIR=lib -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTS=ON -DCMAKE_INSTALL_LOCALSTATEDIR=/var -DTARGET_PROJECT=${cmake_target}
        make -j `nproc` || make -j 1
        rm -rf destdir
        make install DESTDIR=destdir
      ''')

      shell('''
        cd build
        ctest --no-compress-output -T Test
        result=$?
        rm -rf ctest2junit
        git clone https://github.yandex-team.ru/proowl/ctest2junit.git
        ./ctest2junit/convert.py -x ctest2junit/conv.xsl -t ./ > UnitTests.xml
        exit ${result}
      ''')

      shell('''
        cd build
        mkdir -p run_test_dir
        ln -f -s `pwd`/destdir/usr/bin run_test_dir/bin
        ln -f -s `pwd`/destdir/etc run_test_dir/etc
        ln -f -s `pwd`/destdir/usr/lib run_test_dir/lib64
        mkdir -p run_test_dir/var
        export LD_LIBRARY_PATH=`pwd`/run_test_dir/lib64:`pwd`/lib64/modules
        export BUILD_ROOT_DIR=`pwd`/run_test_dir
        [ "${SKIP_SYSTEM_TESTS}" == "1" ] || make test_system || make test_system
      ''')

    
      shell('docker build --pull --build-arg installdir="build/destdir" --build-arg deploydir="${project_directory}/deploy" --tag ${BUILDTAG} -f ${project_directory}/Dockerfile .')
      shell('docker push ${BUILDTAG}')

      publishers {
          git {
              pushOnlyIfSuccess()
              tag('origin', '${GITTAG}') {
                  create(true)
                  update(true)
                  message('text')
              }
          }
      }
    
      publishers {
          updateStartrekIssues() {
              addPerformConditionsStatus('SUCCESS')
              byQuery('KEY:XIVA-1738')
            addComment('''
${BUILDNAME} from ${BRANCH}

testing https://platform.yandex-team.ru/projects/mail/${qloud_project}/testing?component=${qloud_component}&update=yes
stress https://platform.yandex-team.ru/projects/mail/${qloud_project}/stress?component=${qloud_component}&update=yes
sandbox https://platform.yandex-team.ru/projects/mail/${qloud_project}/sandbox?component=${qloud_component}&update=yes
corp https://platform.yandex-team.ru/projects/mail/${qloud_project}/corp?component=${qloud_component}&update=yes
production https://platform.yandex-team.ru/projects/mail/${qloud_project}/production?component=${qloud_component}&update=yes
''')
          }
      }
    
      publishers {
          buildDescription('', '''
testing https://platform.yandex-team.ru/projects/mail/${qloud_project}/testing?component=${${qloud_component}}&update=yes
stress https://platform.yandex-team.ru/projects/mail/${qloud_project}/stress?component=${${qloud_component}}&update=yes
sandbox https://platform.yandex-team.ru/projects/mail/${qloud_project}/sandbox?component=${${qloud_component}}&update=yes
corp https://platform.yandex-team.ru/projects/mail/${qloud_project}/corp?component=${${qloud_component}}&update=yes
production https://platform.yandex-team.ru/projects/mail/${qloud_project}/production?component=${qloud_component}&update=yes
''')
      }
  }
  
  wrappers {
    buildName('${BUILDNAME}')
  }

}
}



create_job('xiva-server', [
  'name':'xiva-server',
  'project_directory':'server',
  'cmake_target':'server',
  'qloud_project': 'xiva-server',
  'qloud_component': 'xiva'
], '')
//create_job('xiva-mobile', [
//  'name':'xiva-mobile',
//  'project_directory':'mobile',
//  'cmake_target':'mobile',
//  'qloud_project': 'xivamob',
//  'qloud_component': 'mobile'
//], '')
create_job('xiva-eq', [
  'name':'xiva-eq',
  'project_directory':'equalizer',
  'cmake_target':'equalizer',
  'qloud_project': 'equalizer',
  'qloud_component': 'equalizer',
  'SKIP_SYSTEM_TESTS': '1'
], '')
//create_job('xiva-mesh', [
//  'name':'xiva-mesh',
//  'project_directory':'mesh',
//  'cmake_target':'mesh',
//  'qloud_project': 'xivamesh',
//  'qloud_component': 'mesh'
//], '')
create_job('xiva-conf', [
  'name':'xiva-conf',
  'project_directory':'conf',
  'cmake_target':'conf',
  'qloud_project': 'xivaconf',
  'qloud_component': 'xivaconf',
  'SKIP_SYSTEM_TESTS': '1'
], '')
//create_job('reaper', [
//  'name':'reaper',
//  'project_directory':'reaper',
//  'cmake_target':'reaper',
//  'qloud_project': 'reaper',
//  'qloud_component': 'reaper'
//], '')
