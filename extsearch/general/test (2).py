import yatest.common


def _codegen():
    return yatest.common.binary_path("extsearch/geo/tools/proto2mms_codegen/proto2mms_codegen")


def _gen_to_output(in_file, proto_name, converter_name, mms_name):
    yatest.common.execute(
        [
            _codegen(),
            "proto",
            in_file,
            proto_name,
            "NGeosearch.NProtos",
        ],
        cwd=yatest.common.output_path(""),
    )
    yatest.common.execute(
        [
            _codegen(),
            "converter",
            in_file,
            converter_name,
            "NGeosearch/NCodegen",
        ],
        cwd=yatest.common.output_path(""),
    )
    yatest.common.execute(
        [
            _codegen(),
            "mms",
            in_file,
            mms_name,
            "NGeosearch/NMmsTypes/NBusiness",
        ],
        cwd=yatest.common.output_path(""),
    )


def test_all():
    _gen_to_output(yatest.common.test_source_path("test.in"), "company.proto", "proto2mms.h", "gen_types.h")
    proto = yatest.common.canonical_file(yatest.common.output_path("company.proto"))
    proto2mms = yatest.common.canonical_file(yatest.common.output_path("proto2mms.h"))
    mms = yatest.common.canonical_file(yatest.common.output_path("gen_types.h"))
    return [proto, proto2mms, mms]


def test_vector_2d():
    _gen_to_output(
        yatest.common.test_source_path("vector_2d.in"), "vector_2d.proto", "vector_2d.proto2mms.h", "vector_2d.h"
    )
    proto = yatest.common.canonical_file(yatest.common.output_path("vector_2d.proto"))
    proto2mms = yatest.common.canonical_file(yatest.common.output_path("vector_2d.proto2mms.h"))
    mms = yatest.common.canonical_file(yatest.common.output_path("vector_2d.h"))
    return [proto, proto2mms, mms]
