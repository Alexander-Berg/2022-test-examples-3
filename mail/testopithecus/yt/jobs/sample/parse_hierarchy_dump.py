import base64
import json
import os
import sys
from bs4 import BeautifulSoup


def main():
    input_dir = sys.argv[1]
    date=sys.argv[2]
    print >> sys.stderr, "starting script with params input_dir={} and date={}".format(input_dir, date)
    for input_path in os.listdir(input_dir):
        print >> sys.stderr, "current input_path={}".format(input_dir)
        id = input_path.split(".", 2)[0]
        result_list = []
        if input_path.endswith(".xml"):
            with open(os.path.join(input_dir,input_path), 'r') as dump_xml_file:
                dump_xml_file_contents=dump_xml_file.read()
                y = BeautifulSoup(dump_xml_file_contents, features="html.parser")
                candidates_holder_node = y.findAll("node",{
                    "resource-id": "com.google.android.inputmethod.latin:id/softkey_holder_fixed_candidates"
                })
                if len(candidates_holder_node) == 1:
                    print >> sys.stderr, "found candidates holder"
                    candidates_holder_node = candidates_holder_node[0]
                    dump_xml_file_contents = str(candidates_holder_node)
                    candidates = candidates_holder_node.findAll("node",{ "class": "android.widget.FrameLayout" })

                    for candidate in candidates:
                        if len(candidate['content-desc']) > 0:
                            result_list.append(candidate['content-desc'].encode("utf-8"))
                else:
                    print >> sys.stderr, "Not found candidates holder!!"
                result_str = ",".join(result_list)
                if len(result_list) > 0:
                    result_dict = {
                        "date": date,
                        "Id": id,
                        "suggests": result_str
                    }
                    print >> sys.stdout, json.dumps(result_dict)


if __name__ == "__main__":
    main()