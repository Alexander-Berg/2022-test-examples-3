package patches.projects

import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a project with id = 'DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds'
in the project with id = 'DiskManager_Tests_Cleanup', and delete the patch script.
*/
create(RelativeId("DiskManager_Tests_Cleanup"), Project({
    id("DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds")
    name = "hw-nbs-stable-lab (No-agent builds)"

    params {
        param("cluster", "hw_nbs_stable_lab")
    }
    buildTypesOrderIds = arrayListOf(RelativeId("DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds_AcceptanceTestCommon"), RelativeId("DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds_AcceptanceTest"), RelativeId("DiskManager_Tests_Cleanup_HwNbsStableLabNoAgentBuilds_EternalAcceptanceTest"))
}))

