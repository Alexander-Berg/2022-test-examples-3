package ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.mdm.fixtures.mdmTreeEdge
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgeList
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgeServiceGrpc
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgesByParentIdRequest
import ru.yandex.market.mdm.http.tree_edges.MdmTreeEdgesByParentIdResponse
import ru.yandex.market.mdm.http.tree_edges.UpdateMdmTreeEdgeRequest
import ru.yandex.market.mdm.http.tree_edges.UpdateMdmTreeEdgeResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmTreeEdgeSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcMdmTreeEdgeMetadataServiceTest {

    @Mock
    lateinit var mdmTreeEdgeService: MdmTreeEdgeServiceGrpc.MdmTreeEdgeServiceBlockingStub

    lateinit var treeEdgeMetadataService: GrpcMdmTreeEdgeMetadataService

    @Before
    fun init() {
        treeEdgeMetadataService =
            GrpcMdmTreeEdgeMetadataService(mdmTreeEdgeService)
    }

    @Test
    fun `should return tree edges by filter`() {
        // given
        val filter = MdmTreeEdgeSearchFilter(
            treeId = 1L,
            parentId = 2L,
        )
        val returnedEdge = mdmTreeEdge()
        given(mdmTreeEdgeService.getTreeEdgesByParentId(any()))
            .willReturn(
                MdmTreeEdgesByParentIdResponse.newBuilder()
                    .setMdmTreeEdges(
                        MdmTreeEdgeList.newBuilder()
                            .addAllMdmTreeEdges(listOf(returnedEdge.toProto()))
                    )
                    .build()
            )

        // when
        val result = treeEdgeMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<MdmTreeEdgesByParentIdRequest>()
        verify(mdmTreeEdgeService).getTreeEdgesByParentId(requestCaptor.capture())
        requestCaptor.firstValue.parentMdmTreeEdgeId shouldBe 2L
        requestCaptor.firstValue.mdmTreeId shouldBe 1L

        // and
        result shouldContain returnedEdge
    }

    @Test
    fun `should update tree edges`() {
        // given
        val update = Update(mdmTreeEdge())
        val returnedTreeEdge = mdmTreeEdge()
        given(mdmTreeEdgeService.updateTreeEdges(any()))
            .willReturn(
                UpdateMdmTreeEdgeResponse.newBuilder()
                    .setResults(
                        MdmTreeEdgeList.newBuilder()
                            .addAllMdmTreeEdges(listOf(returnedTreeEdge.toProto()))
                    )
                    .build()
            )
        // when
        val result = treeEdgeMetadataService.update(listOf(update),
            UpdateContext(commitMessage = "commit", userLogin = "user"))

        // then
        val requestCaptor = argumentCaptor<UpdateMdmTreeEdgeRequest>()
        verify(mdmTreeEdgeService).updateTreeEdges(requestCaptor.capture())
        requestCaptor.firstValue.updates.mdmTreeEdgesList shouldContain update.update.toProto()
        requestCaptor.firstValue.context.commitMessage shouldBe "commit"
        requestCaptor.firstValue.context.userLogin shouldBe "user"

        // and
        result.results shouldContain returnedTreeEdge
    }
}

