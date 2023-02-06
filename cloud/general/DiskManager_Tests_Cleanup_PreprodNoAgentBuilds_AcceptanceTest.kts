package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = 'DiskManager_Tests_Cleanup_PreprodNoAgentBuilds_AcceptanceTest'
in the project with id = 'DiskManager_Tests_Cleanup_PreprodNoAgentBuilds', and delete the patch script.
*/
create(RelativeId("DiskManager_Tests_Cleanup_PreprodNoAgentBuilds"), BuildType({
    templates(RelativeId("Nbs_YcNbsCiRunYaMakeAgentless"))
    id("DiskManager_Tests_Cleanup_PreprodNoAgentBuilds_AcceptanceTest")
    name = "Acceptance test"
    description = "Remove stale entities from DM Acceptance test"

    params {
        param("sandbox.config_path", "%configs_dir%/runner/dm/cleanup/%cluster%/acceptance.yaml")
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
        testFailure = false
        nonZeroExitCode = false
        javaCrash = false
    }
}))
