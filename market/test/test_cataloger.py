import yatest.common
import tarfile


def make_test(name, input_path):
    config = """
        SERVANT_NAME\tmarketcataloger

        CATALOG_FILENAME\tcataloger_data/catalog.xml
        NAVIGATION_INFO\tcataloger_data/navigation_info.xml
        REDIRECTS_FILE\tcataloger_data/navigation-redirects.xml
        REGION_STATS_FILENAME\tcataloger_data/category_region_stats.csv
        BLUE_REGION_STATS_FILENAME\tcataloger_data/blue_category_region_stats.csv
        REGION_TREE_FILENAME\tcataloger_data/geo.c2p
        GEOBASE_FILENAME\tcataloger_data/geobase.xml
        GLOBAL_VENDORS_FILENAME\tcataloger_data/global.vendors.xml
        OFFERS_FILENAME\tcataloger_data/data.xml
        CATEGORY_RESTRICTIONS\tcataloger_data/category-restrictions.json
        POPULAR_VENDORS_FILENAME\tcataloger_data/popular-vendors.xml
        VENDOR_CATEGORY_STATS_FILENAME\tcataloger_data/vendor_category_stats.pbuf.sn
        BLUE_VENDOR_CATEGORY_STATS_FILENAME\tcataloger_data/blue_vendor_category_stats.pbuf.sn
        SHOP_CATEGORIES\tcataloger_data/shop_regional_categories.csv

        FIXED_TIME\t20151022_1829
        FROM_TESTS\t1
        """

    config_file_path = "printcataloger.cfg"
    with open(config_file_path, "w") as config_file:
        config_file.write(config)

    with tarfile.open("cataloger_data.tar.gz", 'r:gz') as tarredgzippedFile:
        tarredgzippedFile.extractall()

    command = [
        yatest.common.binary_path("market/cataloger/bin/offline_cataloger/offline_cataloger"),
        "-c", config_file_path, input_path
    ]

    out_path = "cataloger_print.out"
    with open(out_path, "w") as out:
        with open(input_path) as input:
            yatest.common.execute(command, stdout=out, stdin=input)

    return yatest.common.canonical_file(out_path, diff_tool_timeout=60)


def make_json_reqs(input_file, output_file):
    with open(output_file, "w") as out:
        with open(input_file) as input:
            for r in input:
                if '?' in r:
                    out.write(r.rstrip() + "&format=json\n")
                else:
                    out.write(r.rstrip() + "?format=json\n")


def test_cataloger_xml():
    input_path = yatest.common.source_path("market/cataloger/bin/offline_cataloger/test/cataloger_test_req.txt")
    return make_test('test cataloger xml', input_path)


def test_cataloger_json():
    input_path = yatest.common.source_path("market/cataloger/bin/offline_cataloger/test/cataloger_test_req.txt")
    json_file = "cataloger_test_json_req.txt"
    make_json_reqs(input_path, json_file)
    return make_test('test cataloger json', json_file)
