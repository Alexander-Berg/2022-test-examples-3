package ru.yandex.market.pricingmgmt.loaders.SskuVatLoader

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.SskuVatLoader
import ru.yandex.market.pricingmgmt.loaders.helpers.MssqlDbHelper
import javax.sql.DataSource

class SskuVatLoaderTest : ControllerTest() {

    @Autowired
    lateinit var sskuVatLoader: SskuVatLoader

    @Autowired
    @Qualifier("axaptaDataSource")
    lateinit var testDataSource: DataSource

    @MockBean
    lateinit var mssqlDbHelper: MssqlDbHelper

    private val fakeReplica = "fakeReplica"
    private val fakeDb = "fakeDb"

    @Test
    @DbUnitDataSet(
        before = ["SskuVatLoaderTest_importVat.before.csv"],
        after = ["SskuVatLoaderTest_importVat.after.csv"]
    )
    fun importSskusVat() {
        Mockito.`when`(mssqlDbHelper.getMssqlReplicaNodeName())
            .thenReturn(fakeReplica)

        Mockito.`when`(mssqlDbHelper.getMssqlReplicaDataSource(fakeReplica, fakeDb))
            .thenReturn(testDataSource)

        sskuVatLoader.run()
    }
}
