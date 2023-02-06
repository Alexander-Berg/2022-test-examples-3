from crypta.ltp.viewer.lib.compact_index.py import pack_index


def test_unpack():
    index = {
        "LtpEcom": ["2022-10-03", "2021-10-03"],
        "LtpWatch": ["2022-11-01", "2022-11-02"],
    }

    return pack_index.unpack(pack_index.pack(index))
