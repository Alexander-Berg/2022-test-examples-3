package ru.yandex.market.mdm.metadata.service.grpc

import io.grpc.testing.GrpcCleanupRule
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmTreeEdge
import ru.yandex.market.mdm.fixtures.randomId
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgeList
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgeServiceGrpc
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgesByParentIdRequest
import ru.yandex.market.mdm.http.tree_edges.UpdateMdmTreeEdgeRequest
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.metadata.repository.MdmTreeEdgeRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel

class MdmTreeEdgeGrpcServiceTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmTreeEdgeGrpcService: MdmTreeEdgeGrpcService

    @Autowired
    lateinit var mdmTreeEdgeRepository: MdmTreeEdgeRepository

    @Rule
    @JvmField
    final val grpcCleanupRule = GrpcCleanupRule()

    lateinit var mdmTreeEdgeClient: MdmTreeEdgeServiceGrpc.MdmTreeEdgeServiceBlockingStub

    @Before
    fun initClient() {
        mdmTreeEdgeClient =
            MdmTreeEdgeServiceGrpc.newBlockingStub(
                createTestManagedChannel(
                    grpcCleanupRule,
                    mdmTreeEdgeGrpcService
                )
            )
    }

    @Test
    fun `should return all edges by parentId and treeId`() {
        // given
        val parentId = randomId()
        val anotherParentId = parentId + 1
        val treeId = randomId()
        val mdmTreeEdgeFirstChild = mdmTreeEdgeRepository.insert(mdmTreeEdge(parentId = parentId, treeId = treeId))
        val mdmTreeEdgeSecondChild = mdmTreeEdgeRepository.insert(mdmTreeEdge(parentId = parentId, treeId = treeId))
        val mdmTreeEdgeNonChild = mdmTreeEdgeRepository.insert(mdmTreeEdge(parentId = anotherParentId, treeId = treeId))
        val request = MdmTreeEdgesByParentIdRequest
            .newBuilder()
            .setMdmTreeId(treeId)
            .setParentMdmTreeEdgeId(parentId)
            .build()

        // when
        val response = mdmTreeEdgeClient.getTreeEdgesByParentId(request)

        // then
        assertSoftly {
            response.mdmTreeEdges.mdmTreeEdgesList shouldContain mdmTreeEdgeFirstChild.toProto()
            response.mdmTreeEdges.mdmTreeEdgesList shouldContain mdmTreeEdgeSecondChild.toProto()
            response.mdmTreeEdges.mdmTreeEdgesList shouldNotContain mdmTreeEdgeNonChild.toProto()
        }
    }

    @Test
    fun `should insert tree edge`() {
        // given
        val mdmTreeEdge = mdmTreeEdge(parentId = 0, treeId = 0)

        val request = UpdateMdmTreeEdgeRequest.newBuilder()
            .setUpdates(MdmTreeEdgeList.newBuilder().addMdmTreeEdges(mdmTreeEdge.toProto()).build())
            .setContext(MdmBase.MdmUpdateContext.newBuilder().setCommitMessage("updated by me").build())
            .build()

        // when
        val response = mdmTreeEdgeClient.updateTreeEdges(request)

        // then
        assertSoftly {
            response.results.mdmTreeEdgesCount shouldBe 1
        }

        // and
        val savedEdge = mdmTreeEdgeRepository.findByChildId(mdmTreeEdge.childId, mdmTreeEdge.parentId)
        savedEdge shouldNotBe null
    }
}
