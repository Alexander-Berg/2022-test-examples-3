#include <crypta/lib/native/proto_serializer/utf8_escape_transform.h>

#include <library/cpp/testing/gtest/gtest.h>

TEST(NProtoSecrets, RemoveSecrets) {
    NCrypta::NProtoSerializer::TUtf8EscapeTransform transform;

    TString transformee = "\n\tА я ü ö ä ß";
    transform.Transform(transformee);
    EXPECT_EQ("\\n\\t\\u0410 \\u044F \\u00FC \\u00F6 \\u00E4 \\u00DF", transformee);
}
