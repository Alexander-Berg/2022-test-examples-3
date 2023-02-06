package ru.yandex.market.mbi.feed.processor.test

import com.google.protobuf.Message
import org.assertj.core.api.RecursiveComparisonAssert
import ru.yandex.market.common.test.util.ProtoTestUtil
import java.lang.invoke.MethodHandles

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */

inline fun <reified PROTO_TYPE : Message> getProto(path: String): PROTO_TYPE =
    ProtoTestUtil.getProtoMessageByJson(PROTO_TYPE::class.java, path, MethodHandles.lookup().lookupClass())

inline fun <reified PROTO_TYPE : Message> protoAssert(
    actual: PROTO_TYPE,
    path: String,
    assert: RecursiveComparisonAssert<*>.() -> Unit
) {
    val expected = getProto<PROTO_TYPE>(path)
    ProtoTestUtil.assertThat(actual).assert()
}
