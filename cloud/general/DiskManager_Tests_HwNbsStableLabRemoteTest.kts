package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.exec
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = 'DiskManager_Tests_HwNbsStableLabRemoteTest'
in the project with id = 'DiskManager_Tests_RemoteTest', and delete the patch script.
*/
create(RelativeId("DiskManager_Tests_RemoteTest"), BuildType({
    templates(RelativeId("DiskManager_RunWithScript"))
    id("DiskManager_Tests_HwNbsStableLabRemoteTest")
    name = "[HW-NBS-STABLE-LAB] Remote Test"
    description = "Disk Manager Remote Test"
    paused = true

    artifactRules = """
        %system.teamcity.build.workingDir%/arts/*=>arts.zip
        %system.teamcity.build.workingDir%/coverage.report=>coverage.zip
    """.trimIndent()
    maxRunningBuilds = 1

    params {
        param("env.kill_timeout", "18000")
        param("env.ya_timeout", "14400")
        param("env.arcadia_branch", "trunk")
        param("env.ya_make_targets", "cloud/disk_manager/test/remote")
        param("env.ya_make_test_tag", """
            ya:manual
            ya:notags
        """.trimIndent())
        param("env.test_threads", "32")
        param("env.ya_make_test_params", """
            disk-manager-client-config=cloud/disk_manager/test/remote/configs/hw-nbs-stable-lab/disk-manager-client-config.txt
            nbs-client-config=cloud/disk_manager/test/remote/configs/hw-nbs-stable-lab/nbs-client-config.txt
        """.trimIndent())
        param("env.branch_path", "trunk")
        param("env.sandbox_task_owner", "TEAMCITY")
    }

    steps {
        script {
            name = "Checkout and build teamcity tools"
            id = "RUNNER_14876"
            scriptContent = """
                set -ue
                
                rm -rf ./arcadia
                
                svn cat svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia/ya | python - clone arcadia 1>/dev/null
                cd arcadia
                
                ./ya make --checkout -j32 -q cloud/disk_manager/build/ci
            """.trimIndent()
        }
        exec {
            name = "Run teamcity-run-test"
            id = "RUNNER_15022"
            workingDir = "arcadia/cloud/disk_manager/build/ci/teamcity_new/teamcity-run-tests"
            path = "./teamcity-run-tests"
        }
    }

    triggers {
        schedule {
            id = "TRIGGER_1288"
            schedulingPolicy = daily {
                hour = 0
            }
            branchFilter = ""
            triggerBuild = always()
        }
    }
}))

