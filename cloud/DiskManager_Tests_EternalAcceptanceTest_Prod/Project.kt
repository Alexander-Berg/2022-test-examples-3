package DiskManager_Tests_EternalAcceptanceTest_Prod

import DiskManager_Tests_EternalAcceptanceTest_Prod.buildTypes.*
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

object Project : Project({
    id("DiskManager_Tests_EternalAcceptanceTest_Prod")
    name = "prod"
    archived = true

    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest4GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest512GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest32GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest128GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest256GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest2GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest8GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest2TiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest16GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest64GiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest8TiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest4TiB)
    buildType(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest1TiB)

    params {
        param("env.CLUSTER", "prod")
        param("env.INSTANCE_RAM", "8")
        param("env.INSTANCE_CORES", "8")
    }
    buildTypesOrder = arrayListOf(DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest2GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest4GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest8GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest16GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest32GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest64GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest128GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest256GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest512GiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest1TiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest2TiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest4TiB, DiskManager_Tests_EternalAcceptanceTest_Prod_EternalAcceptanceTest8TiB)
})
