package ru.yandex.direct.core.testing.mock

import org.joda.time.Duration
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import ru.yandex.direct.common.mds.MdsHolder
import ru.yandex.direct.core.testing.data.TestMdsConstants.TEST_MDS_FILENAME
import ru.yandex.direct.core.testing.data.TestMdsConstants.TEST_MDS_GROUP
import ru.yandex.direct.dbschema.ppc.enums.MdsMetadataStorageHost
import ru.yandex.inside.mds.MdsFileKey
import ru.yandex.inside.mds.MdsHosts
import ru.yandex.inside.mds.MdsInternalProxies
import ru.yandex.inside.mds.MdsNamespace
import ru.yandex.inside.mds.MdsPostResponse
import ru.yandex.inside.passport.tvm2.Tvm2
import ru.yandex.misc.io.InputStreamSource
import ru.yandex.misc.ip.HostPort
import java.lang.String.join

const val MDS_NAMESPACE = "direct-files"
val HOST: String = MdsMetadataStorageHost.storage_int_mdst_yandex_net.literal
const val PORT = 8088

class MdsHolderStub : MdsHolder(HOST, HOST, MDS_NAMESPACE, 0, 0) {
    override fun delete(mdsFileKey: MdsFileKey) {
    }

    override fun downloadUrl(action: String, fileKey: MdsFileKey): String = join(
        "/",
        "http://" + MdsMetadataStorageHost.storage_int_mdst_yandex_net.literal,
        action + "-" + MDS_NAMESPACE,
        fileKey.group.toString(),
        fileKey.filename
    )

    override fun getNamespace(): MdsNamespace {
        return spy(MdsNamespace(MDS_NAMESPACE, mock(Tvm2::class.java), 1));
    }

    override fun getHosts(): MdsHosts {
        return spy(MdsHosts(HostPort(HOST, PORT), HostPort(HOST, PORT), mock(MdsInternalProxies::class.java)))
    }

    override fun upload(filename: String, source: InputStreamSource): MdsPostResponse = MdsPostResponseStub()

    override fun upload(filename: String, source: InputStreamSource, expire: Duration): MdsPostResponse =
        spy(MdsPostResponseStub())
}

class MdsPostResponseStub : MdsPostResponse() {
    val fileKey: MdsFileKey = MdsFileKey(TEST_MDS_GROUP, TEST_MDS_FILENAME)

    override fun getKey(): MdsFileKey = fileKey
}
