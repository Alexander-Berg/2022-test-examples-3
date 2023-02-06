from __future__ import print_function

import os.path
import glob
import sys

class TestException(Exception):
    """Throws on critical errors
    """
    pass

def testbase(config):
    result_files_new = get_result_files(config["output"]["dir_result"])
    result_files_old = get_result_files(config["test"]["dir_old_result"])

    fnames = set(result_files_new.keys()) | set(result_files_old.keys())
    for fn in fnames:
        BaseComparer(config, result_files_old, result_files_new, fn)

    testrequests(config)

def testrequests(config):
    unittests_output_file = config["output"]["dir_output"] + "errorlog_unittest.txt"

    with open(config["output"]["dir_output"] + "errorlog_test_reqs.txt", "w") as errtest:
        config.timerunner.run([sys.executable,
            config["programs"]["root"] + "/testbase/test_requests.py",
            "--autobase", config["output"]["dir_result"],
            "--exec", config["programs"]["assistant"],
            "--requests", config["test"]["test_requests"]
            ], None,
            unittests_output_file, errtest,
            "analyze test answers")

    if not config["test"]["allow_uniterror"] and os.path.getsize(unittests_output_file):
        raise TestException("Error on unit requests")

def testrequests_old(config):
    assistant_res_file_name = config["output"]["dir_tmp"] + "printguruassq.txt"
    with open(config["output"]["dir_output"] + "errorlog_printguruass.txt", "w") as errguruass:
        config.timerunner.run([config["programs"]["assistant"],
            "-d", config["output"]["dir_result"]],
            config["test"]["test_requests"], assistant_res_file_name, errguruass,
            "test some requests")

    with open(config["output"]["dir_output"] + "errorlog_test_reqs.txt", "w") as errtest:
        config.timerunner.run([sys.executable,
            config["programs"]["root"] + "/testbase/test_hasfilters.py"],
            assistant_res_file_name, config["output"]["dir_output"] + "errorlog_unittest.txt", errtest,
            "analyze test answers")

    if not config["test"]["allow_uniterror"] and os.path.getsize(config["output"]["dir_output"] + "errorlog_unittest.txt"):
        raise TestException("Error on unit requests")

class BaseComparer:
    def __init__(self, config, result_files_old, result_files_new, fname):
        self.__fname = fname
        self.__config = config
        self.__fnew = result_files_new.get(self.__fname)
        self.__fold = result_files_old.get(self.__fname)

        self.__error_log = self.__open_error_log()

        self.__run_tests()

    def __run_tests(self):
        if not self.__fnew:
            print("file", self.__fname, "disappeared", file=self.__error_log);
            if not self.__config["test"]["allow_disappear"]:
                raise TestException("Some files disappeared")
            return

        self.__test_assert_size()

        if not self.__fold:
            print("there is new file:", self.__fname, file=self.__error_log);
            return

        self.__test_compare_old_new()

    def __open_error_log(self):
        error_log_name = self.__config["output"]["dir_output"] + "errorlog_test_" + self.__fname + ".txt"
        return open(error_log_name, "w")

    def __test_assert_size(self):
        sz = os.path.getsize(self.__fnew)
        if os.path.getsize(self.__fnew) < self.__config["test"]["min_result_size"]:
            print("file", self.__fname, "is too small:", sz, file=self.__error_log)

    def __test_compare_old_new(self):
        output_name = self.__config["output"]["dir_output"] + "cmp_" + self.__fname + ".txt"
        self.__config.timerunner.run([sys.executable,
            self.__config["programs"]["root"] + "/testbase/compare_base.py",
            self.__fnew, self.__fold],
            "", output_name, self.__error_log,
            "compare old and new " + self.__fname)

        #TODO: test diff size


def get_result_files(path):
    res = {}
    for fn in glob.glob(path + "*.json.gz"):
        res[os.path.basename(fn)[:-3]] = fn

    for fn in glob.glob(path + "*.json"):
        res[os.path.basename(fn)] = fn
    return res
