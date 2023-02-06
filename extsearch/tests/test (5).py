from extsearch.geo.kernel.event_times.python.datemapper import DateMapper
import yatest.common as yc


def test():
    ofile_path = yc.output_path('test.out')
    ofile = open(ofile_path, "w")

    s_beam = DateMapper("{\"start\": 200000000}")
    ofile.write("s_beam:\n" + s_beam.to_json_str() + '\n')

    assert s_beam.is_included(200000000)
    assert s_beam.is_included(200050000)
    assert not s_beam.is_included(100000000)
    assert not s_beam.is_included(199999999)

    e_beam = DateMapper("{\"end\": 200000000}")
    ofile.write("e_beam:\n" + e_beam.to_json_str() + '\n')

    assert e_beam.is_included(200000000)
    assert e_beam.is_included(100050000)
    assert not e_beam.is_included(300000000)
    assert not e_beam.is_included(200000001)

    seg = DateMapper("{\"start\": 100000000, \"end\": 200000000}")
    ofile.write("seg:\n" + seg.to_json_str() + '\n')

    assert seg.is_included(100050000)
    assert seg.is_included(100000000)
    assert seg.is_included(200000000)
    assert not seg.is_included(300000000)
    assert not seg.is_included(10000000)
    assert not seg.is_included(99999999)
    assert not seg.is_included(200000001)

    # 50-100_300-350_550-600_...
    inf_seg = DateMapper("{\"start\": 50, \"end\": 100, \"period\": {\"value\": 250}}")
    ofile.write("inf_seg:\n" + inf_seg.to_json_str() + '\n')

    assert inf_seg.is_included(75)
    assert inf_seg.is_included(50)
    assert inf_seg.is_included(100)
    assert not inf_seg.is_included(49)
    assert not inf_seg.is_included(101)
    assert inf_seg.is_included(300)
    assert inf_seg.is_included(350)
    assert inf_seg.is_included(325)
    assert inf_seg.is_included(551)
    assert not inf_seg.is_included(549)
    assert not inf_seg.is_included(601)

    # 50-100_250-300_450-500_...
    rec_seg = DateMapper("{\"start\": 50, \"end\": 100, \"period\": {\"value\": 200, \"count\": 3}}")
    ofile.write("rec_seg:\n" + rec_seg.to_json_str() + '\n')

    assert rec_seg.is_included(75)
    assert rec_seg.is_included(50)
    assert rec_seg.is_included(100)
    assert not rec_seg.is_included(115)
    assert rec_seg.is_included(250)
    assert rec_seg.is_included(300)
    assert not rec_seg.is_included(322)
    assert not rec_seg.is_included(423)
    assert rec_seg.is_included(450)
    assert rec_seg.is_included(500)
    assert not rec_seg.is_included(650)
    assert not rec_seg.is_included(670)
