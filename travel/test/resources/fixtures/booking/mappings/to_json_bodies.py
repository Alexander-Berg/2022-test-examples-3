#!/usr/bin/env python

import codecs
import json
import os


def main():
    for fname in os.listdir("."):
        if not fname.endswith(".json"):
            continue
        with open(fname, "r") as fp0:
            fp = codecs.getreader("utf-8")(fp0)
            mapping = json.load(fp)
        rewrite = False
        if "body" in mapping["response"] and "jsonBody" not in mapping["response"]:
            mapping["response"]["jsonBody"] = json.loads(mapping["response"]["body"])
            del mapping["response"]["body"]
            rewrite = True
        with open(fname, "w") as fp0:
            fp = codecs.getwriter("utf-8")(fp0)
            json.dump(mapping, fp, ensure_ascii=False, sort_keys=True, indent=2)
            fp.write("\n")


if __name__ == "__main__":
    main()
