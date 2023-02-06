from search.geo.tools.social_links.extract_location.lib.locationextractor import LocationExtractor
import yatest.common as yc
import json


def test_unit():
    le = LocationExtractor()
    output_path = yc.output_path("test_unit.out")
    with open(output_path, "w") as ofile:
        simple = [
            "город Москва",
            "Жулебино",
            "Кривой Рог",
            "Уфа",
            "Батман",
            "В Москве",
        ]
        for item in simple:
            ofile.write(item + " -> " + json.dumps(le.resolve_str(item, True), ensure_ascii=False).encode("utf-8") + '\n')
    return yc.canonical_file(output_path, local=True)
