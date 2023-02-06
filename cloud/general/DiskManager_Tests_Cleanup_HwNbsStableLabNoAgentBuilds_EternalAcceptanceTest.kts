package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = 'DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds_EternalAcceptanceTest'
in the project with id = 'DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds', and delete the patch script.
*/
create(RelativeId("DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds"), BuildType({
    templates(RelativeId("Nbs_YcNbsCiRunYaMakeAgentless"))
    id("DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds_EternalAcceptanceTest")
    name = "Eternal acceptance test"
    description = "Remove stale entities from DM eternal acceptance test"

    params {
        param("sandbox.config_path", "%configs_dir%/runner/dm/cleanup/%cluster%/eternal.yaml")
    }

    triggers {
        schedule {
            id = "TRIGGER_1461"
            schedulingPolicy = cron {
                hours = "0/4"
                dayOfMonth = "?"
                dayOfWeek = "*"
            }
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false
            param("hour", "16")
        }
    }

    failureConditions {
        executionTimeoutMin = 60
    }
}))
