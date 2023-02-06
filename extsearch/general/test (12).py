#! /usr/bin/env python
"""
 .. py:module:: test.py

    Application for assessment of embeded videos extraction algorithms.
"""

import argparse
import errno
import logging
import os
import pprint
import shutil
import subprocess
import sys
import tempfile
import time
import urllib
import urlparse
import zipfile

sys.path.append(os.path.join(os.path.dirname(os.path.realpath(__file__)), "..", "lib"))
import quality_record_pb2
from google.protobuf import text_format

config = {
    "kiwi_server": "kiwi1500.search.yandex.net",
    "toloka_n_urls": 10,
    "toloka_n_texts": 15
}


"""
.. py:data:: binary is path to quality_test binary.
"""
binary = None


"""
.. py:data:: qt_config is path to config for quality_test binary
"""
qt_config = None


def initialize_logger():
    """
     .. py:function:: initialize_logger()

        Initializes global logger.
    """
    global logger
    if logger:
        return
    levels_map = {
        "error": logging.ERROR,
        "debug": logging.DEBUG,
        "info": logging.INFO,
        "warning": logging.WARNING,
        "critical": logging.CRITICAL
    }
    str_level = os.environ.get("LOG", "warning")
    level = levels_map[str_level.lower()]
    log_handler = logging.StreamHandler()
    log_handler.setFormatter(logging.Formatter("[%(levelname)s] %(message)s"))

    logger = logging.getLogger("vt_extractor_quality_tests")
    logger.addHandler(log_handler)
    if level is None:
        raise RuntimeError("bad error logging level: {0}. Available levels {1}".format(str_level, mapp.keys()))
    logger.setLevel(level)


logger = None
initialize_logger()


def find_script(name):
    """
     .. py:function::   find_script(name)

        Finds path to bash script used in this python module.

        :param name:    Script name.
        :return: Path to a script.
        :rtype: str
        :raises RuntimeError: if script wasn't found.
    """
    python_script_path = os.path.dirname(os.path.realpath(sys.argv[0]))
    script_path = os.path.join(python_script_path, name)
    if not os.path.exists(script_path):
        raise RuntimeError("Failed to find script {0}. Searched in path: {1}".format(name, script_path))
    return script_path



def set_quality_tests_bin(hint):
    """
     .. py:function::   set_quality_tests_bin(hint)

        Finds quality_tests binary.

        :param str hint: User hint to search binary at.
        :return: Path to the binary.
        :rtype: str
        :raises RuntimeError: if wasn't able to find a binary.
    """
    global binary
    if binary:
        return

    paths_to_search = [
        os.path.join(os.path.dirname(os.path.realpath(sys.argv[0])), "quality_tests")
    ]
    if hint:
        paths_to_search.insert(0, hint)
    for examinee in paths_to_search:
        if os.path.exists(examinee):
            binary = examinee
            break
    if not binary:
        raise RuntimeError("Failed to find binary in any of the following paths:\n{0}.".format(pprint.pformat(paths_to_search)))
    logger.debug("binary set to {}".format(binary))



def slurp_file(filename):
    """
     .. py:function:: slurp_file(filename)

        Return all contents of a file.

        :param filename: path to the file to read.
        :return: contents of the file.
        :rtype: str
    """
    with open(filename, "r") as fd:
        data = fd.read()
    return data


class TestCase:
    """
     .. py:class::  TestCase

        Represents single test case to run on. It usually have an etalon output and some data
        (html or url or both) to run testing algorithm on.
    """
    __slots__ = ["_filesdict", "_noextname"]
    __allowed_extensions = frozenset(("html", "etalon", "url"))

    def __init__(self):
        """
         .. py:method:: __init__(self)

            Object construction.
        """
        self._filesdict = {} # dictionary {file_extension: filepath}
        self._noextname = None


    def __getitem__(self, ext):
        """
         .. py:method:: __getitem__(self, ext)

            Gets filepath for file with extension ext

            :param str ext: extension of requested file.
            :return: Path to file.
            :rtype: str
            :raises RuntimeError: if test case does not have file with requested extension.
        """
        if ext not in self._filesdict:
            raise RuntimeError("file with extension '{0}' is not in test case".format(ext))
        return self._filesdict[ext]


    def __str__(self):
        """
         .. py:method:: __str__(self)

            Get string representation of an object.
        """
        return "TestCase(\n{0}\n)".format(pprint.pformat(self._filesdict))


    def noextname(self):
        """
         .. py:method:: noextname(self)

            :return: Returns filename without extensions.
        """
        return self._noextname


    def add_file(self, filepath):
        """
         .. py:method:: add_file(self, filepath)

            Adds file to test case

            :param filepath:    Path to the file to add.
            :raises RuntimeError:   If file with the same extension was already added or
                                    file with this extension can not be added to test case.
        """
        filename = os.path.basename(filepath)
        noextname, extension = os.path.splitext(filename)
        if self._noextname is None:
            self._noextname = noextname
        elif noextname != self._noextname:
            raise RuntimeError("Failed to add file {0} to the test case: noextname is not {1}".format(filepath, self._noextname))
        extension = extension[1:] if extension[0] == "." else extension
        if extension not in TestCase.__allowed_extensions:
            raise RuntimeError("Failed to add file {0} to the test case: extension {1} is not allowed".format(filepath, extension))
        if extension in self._filesdict:
            raise RuntimeError("Failed to add file {0} to the test case: there is already file with extention: {1}".format(filepath, extension))
        self._filesdict[extension] = filepath


    def is_complete(self):
        """
         .. py:method:: is_complete(self)

            Checks that all files have been passed by :py:meth:`add_file` function and test case is complete.

            :return:    True if test case is complete, False otherwise.
        """
        return frozenset(self._filesdict.keys()) == TestCase.__allowed_extensions



def create_test_cases(dirpath, filenames):
    """
     .. py:function:: create_test_cases(dirpath, filenames)

        Creates list of :py:class:`TestCase` objects from list of files in directory.

        :param str dirpath: path to a directory with files.
        :param filenames: list of filenames in dirpath directory.
        :type filenames: list of strings.
        :raises RuntimeError: in case of any test case is incomplete.
    """
    test_cases_dict = {}
    logger.debug("filenames: {0}".format(pprint.pformat(filenames)))
    for filename in filenames:
        noextname = os.path.splitext(filename)[0]
        test_case = test_cases_dict.get(noextname, TestCase())
        fullpath = os.path.join(dirpath, filename)
        test_case.add_file(fullpath)
        test_cases_dict[noextname] = test_case
    test_cases_list = list(test_cases_dict.values())
    for tc in test_cases_list:
        if not tc.is_complete():
            raise RuntimeError("test case is incomplete:\n{0}".format(tc))
    return test_cases_list



def zip_dir(directory_path, zipped_path):
    """
     .. py:function:: zip_dir(directory_path, zipped_path)

        Zip a directory.

        :param str directory_path:  etalon directory path.
        :param str zipped_path:     output file with zip archive.
        :raises RuntimeError:   in case of output file already exists or input directory does not exists.
    """
    if not os.path.isdir(directory_path):
        raise RuntimeError("directory {0} requested to be zipped does not exists.".format(directory_path))

    if os.path.exists(zipped_path):
        raise RuntimeError("output {0} is already exists, and I do not dare to overwrite it.".format(zipped_path))

    with zipfile.ZipFile(zipped_path, 'w') as output:
        for root, dirs, files in os.walk(directory_path):
            for filename in files:
                path_to_add = os.path.join(root, filename)
                logger.debug("adding file {0} to {1}".format(path_to_add, zipped_path))
                output.write(path_to_add)



def test_all():
    # TODO
    raise NotImplementedError("mode is not implemented yet.")



def compare(result_file, etalon_file):
    """
     .. py:function:: compare(result_file, etalon_file)

        Compares 2 result and etalon with external binary and returns diff results.

        :param str result_file: file with result.
        :param str etalon_file: file with etalon.

        :return:    tuple (texts_result_only, texts_etalon_only, texts_common). All values are in bytes.
        :raises RuntimeError: in case of errors.
    """
    if binary is None:
        raise RuntimeError("binary is not specified")

    logger.info("comparing {0} with {1}".format(result_file, etalon_file))
    output = subprocess.check_output([binary, "OneTest", "-r", result_file, "-e", etalon_file])
    logger.debug("{0} output:\n{1}".format(binary, output))
    allowed_keys = ["textResultOnly", "textEtalonOnly", "textCommon", "urlResultOnly", "urlEtalonOnly", "urlCommon"]
    res = {}
    for line in output.splitlines():
        key, value = line.split(":")
        if key in allowed_keys:
            res[key] = int(value)
        else:
            raise RuntimeError("bad line in diff output: {0}".format(line))

    for key in allowed_keys:
        if key not in res:
            raise RuntimeError("compare: missing key {} in output:\n{}".format(key, output))
    return res



def calculate_quality(path):
    """
    .. py:function:: calculate_quality runs test on directory with parsed results and etalons.

       :param   path    path to directory with etalons and extraction results.
    """

    logger.info("starting quality calculation stage")

    texts_result_only = 0 # size of text that is only in result.
    texts_etalon_only = 0 # size of text that is only in etalon.
    texts_common = 0 # size of text that is both in the etalon and result.
    urls_result_only = 0 # amount of urls that is only in result.
    urls_etalon_only = 0 # amount of urls that is only in etalon.
    urls_common = 0 # amount of urls that are both in etalon and result.
    total_docs = 0

    compare_count = 0
    def throw(exception):
        raise exception
    for dirpath, dirnames, filenames in os.walk(path, onerror = throw):
        for filename in filenames:
            noextname, extension = os.path.splitext(filename)

            fullname = os.path.join(dirpath, filename)
            result_file = os.path.join(dirpath, noextname + ".result")
            etalon_file = os.path.join(dirpath, noextname + ".etalon")

            if extension == ".result":
                if not os.path.isfile(etalon_file):
                    raise RuntimeError("No etalon for file {0}".format(fullname))
                diff = compare(result_file, etalon_file)
                compare_count += 1
                texts_result_only += diff["textResultOnly"]
                texts_etalon_only += diff["textEtalonOnly"]
                texts_common += diff["textCommon"]
                urls_result_only += diff["urlResultOnly"]
                urls_etalon_only += diff["urlEtalonOnly"]
                urls_common += diff["urlCommon"]
                total_docs += 1
            elif extension == ".etalon":
                result_file = os.path.join(dirpath, noextname + ".result")
                if not os.path.isfile(result_file):
                    raise RuntimeError("Result file {0} does not exists.".format(result_file))
            else:
                raise RuntimeError("Unexpected file {0}. Expected only *.result and *.etalon files".format(fullname))

    if compare_count == 0:
        raise RuntimeError("No files were compared with etalon in directory {0}".format(path))

    texts_result_total = texts_common + texts_result_only
    texts_etalon_total = texts_common + texts_etalon_only
    urls_result_total = urls_common + urls_result_only
    urls_etalon_total = urls_common + urls_etalon_only

    texts_recall = texts_common / float(texts_etalon_total) if texts_etalon_total else 0
    texts_accuracy = texts_common / float(texts_result_total) if texts_result_total else 0
    urls_recall = urls_common / float(urls_etalon_total) if urls_etalon_total else 0
    urls_accuracy = urls_common / float(urls_result_total) if urls_result_total else 0

    print "Tested {0} document{1}.".format(total_docs, "s" if total_docs > 1 else "")
    print
    print "TextsResultOnly: {}".format(texts_result_only)
    print "TextsEtalonOnly: {}".format(texts_etalon_only)
    print "TextsCommon: {}".format(texts_common)
    print "UrlsResultOnly: {}".format(urls_result_only)
    print "UrlsEtalonOnly: {}".format(urls_etalon_only)
    print "UrlsCommon: {}".format(urls_common)
    print
    print "TextsRecall: {0}".format(texts_recall)
    print "TextsAccuracy: {0}".format(texts_accuracy)
    print "UrlsRecall: {}".format(urls_recall)
    print "UrlsAccuracy: {}".format(urls_accuracy)



def calc_qual(args):
    """
    .. py:function:: calc_qual  command to run calculation quality on directory with parsing result and etalons.

        :param  args    command line arguments without programm anme and command name
    """
    parser = argparse.ArgumentParser(description = "run on specified directory with results and etalons.", add_help = True)
    parser.add_argument("-d", "--dir", type = str, required = True, help = "directory with results of parsing and etalons.")
    parser.add_argument("-b", "--binary", type = str, help = "path to quality_test binary")

    parsed_args = parser.parse_args(args)
    set_quality_tests_bin(parsed_args.binary)
    calculate_quality(parsed_args.dir)



def indexarch_kiwi_parser_callback(test_case):
    """
     .. py:function:: indexarch_kiwi_parser_callback(test_case)

        Parses test_case on kiwi with indexarch trigger.

        :param TestCase test_case:  one test case.
    """
    url_path = test_case["url"]
    with open(url_path, "r") as url_file:
        url = url_file.read()
    if url == "":
        raise RuntimeError("Empty url in file: {}".format(url_path))
    parsed_content = kiwi_get_indexarch_etalon(url)
    return parsed_content


def vt_extractor_parse_file(html_path):
    """
     .. py:function:: vt_extractor_parse_file(html_path)

        Call vt_extractor on html_path and return output.

        :param str html_path: path to html.
    """
    logger.debug("binary: {}".format(binary))
    if qt_config:
        parsed_content = subprocess.check_output([binary, "ParseHtml", "-i", html_path, "-c", qt_config])
    else:
        parsed_content = subprocess.check_output([binary, "ParseHtml", "-i", html_path])

    return parsed_content


def vt_extractor_parse_content(html_content):
    """
     .. py:function:: vt_extractor_parse_content(html_content)

        Parse html with vt_extractor.

        :param str html_content: html data to parse with vt_extractor.
    """
    file_descriptor, filepath = tempfile.mkstemp()
    try:
        logger.debug("vt_extractor_parse_content: created temp file {}".format(filepath))
        written_bytes = os.write(file_descriptor, html_content)
        if written_bytes != len(html_content):
            raise RuntimeError("failed to write all data to file {}, only {} of {}".format(filepath, written_bytes, len(html_content)))
        os.close(file_descriptor)
        parsed_content = vt_extractor_parse_file(filepath)
    finally:
        os.remove(filepath)
    return parsed_content



def vt_extractor_parser_callback(test_case):
    """
     .. py:function:: vt_extractor_parser_callback(test_case)

        Parses test_case by embed extractor.

        :param TestCase test_case:  one test case.
    """
    html_path = test_case["html"]
    logger.info("parse file {0}".format(html_path))
    parsed_content = vt_extractor_parse_file(html_path)
    return parsed_content



def parse_case(path, output_path, parser_callback):
    """
    .. py:function:: parse_case parses html to results and prepares direcotory with parsed results and etalons.
        :param  path        directory with htmls and etalons
        :param  output_patn path to create directory with parsed results and etalons.
    """
    logger.info("starting parsing stage")
    # make shure output directory is clean.
    if os.path.isdir(output_path):
        shutil.rmtree(output_path)
    elif os.path.exists(output_path):
        raise RuntimeError("{0} exists and is not a directory. Probabaly you messed up in command line optinos.".format(output_path))

    total_parsed = 0
    def throw(exception):
        raise exception
    for dirpath, dirnames, filenames in os.walk(path, onerror = throw):
        # create directory for parsed results.
        cur_dir = os.path.relpath(dirpath, path)
        logger.debug("cur_dir: {0}".format(cur_dir))
        output_dir = os.path.normpath(os.path.join(output_path, cur_dir))
        logger.info("creating directory {0}".format(output_dir))
        os.mkdir(output_dir)

        test_cases = create_test_cases(dirpath, filenames)
        for test_case in test_cases:
            # parse test case to get result file.
            parsed_case = parser_callback(test_case)
            result_filename = os.path.join(output_dir, test_case.noextname() + ".result")
            with open(result_filename, "w") as result_file:
                result_file.write(parsed_case)

            # copy etalon to output dir.
            etalon_path = test_case["etalon"]
            logger.info("copy etalon {0}".format(etalon_path))
            etalon_filename = os.path.basename(etalon_path)
            dst_filename = os.path.join(output_dir, etalon_filename)
            shutil.copyfile(etalon_path, dst_filename)

        total_parsed += len(test_cases)

    if not total_parsed:
        raise RuntimeError("no files were parsed from: {0}".format(path))



def test_case(args):
    """
    .. py:function:: test_case  command to run case on one directory with htmls and etalons.

        :param args command line params without programm name and command name.
    """
    parser = argparse.ArgumentParser(description = "Run on specified directory with htmls and etalons.", add_help = True)
    parser.add_argument("-d", "--dir", type = str, required = True, help = "Directory with quality test case.")
    parser.add_argument("-c", "--config", type = str, help = "Config for parsing html, just as config in vt_extractor.")
    parser.add_argument("-b", "--binary", type = str, help = "Path to quality_test binary.")
    parser.add_argument("-x", "--indexarch", action = "store_true", default = False, help = "Use indexarch trigger on kiwi for comparisons.")

    parsed_args = parser.parse_args(args)
    set_quality_tests_bin(parsed_args.binary)
    global qt_config
    qt_config = parsed_args.config

    parsed_dir = "test.{}.{}".format(os.path.basename(parsed_args.dir.rstrip("/")), int(time.time()))
    parser_callback = vt_extractor_parser_callback if not parsed_args.indexarch else indexarch_kiwi_parser_callback
    parse_case(parsed_args.dir, parsed_dir, parser_callback)
    calculate_quality(parsed_dir)



def download_case(args):
    """
     .. py:function:: download_case(args)

        Downloads test case from sandbox.

        :param args command line params without programm name and command name.
    """
    parser = argparse.ArgumentParser(description = "Download test case from sandbox.", add_help = True)
    parser.add_argument("-d", "--dir", type = str, default = "vt_extract_tc", help = "Output directory to save test case to.")
    parser.add_argument("-r", "--resourse", type = str, required = True, help = "Sandbox resource id")
    parsed_args = parser.parse_args(args)

    if os.path.exists(parsed_args.dir):
        raise RuntimeError("{0} path is already exists".format(parsed_args.dir))

    download_case.url = "http://proxy.sandbox.yandex-team.ru/{0}"
    url = download_case.url.format(parsed_args.resourse)
    local_resource, headers = urllib.urlretrieve(url, parsed_args.dir)
    print local_resource
    # TODO
    raise NotImplementedError()



def upload_case(args):
    """
     .. py:function:: upload_case    upload test case to sandbox.

        :param args: command line params without programm name and command name.
    """
    parser = argparse.ArgumentParser(description = "Upload test case to sandbox.", add_help = True)
    parser.add_argument("-d", "--dir", type = str, default = "vt_extract_tc", help = "Test case directory.")
    parser.add_argument("-y", "--ya", type = str, help = "Path to ymake tool.")
    parsed_args = parser.parse_args(args)

    directory_name = parsed_args.dir
    zipped_dir_path = directory_name + ".zip"
    zip_dir(directory_name, zipped_dir_path)
    # TODO
    raise NotImplementedError()



def get_noextnames_from_dir_content(dirpath):
    files = os.listdir(dirpath)
    noextnames = (os.path.splitext(f)[0] for f in files)
    numbers = set([int(x) for x in noextnames])
    max_number = len(numbers) + 1
    min_unused_number = next((x for x in xrange(max_number) if x not in numbers))
    noextname = str(min_unused_number)
    logger.debug("get_noextnames_from_dir_content: basename {0}".format(noextname))
    return noextname



def kiwi_get_media_json(url):
    """
     .. py:function:: kiwi_get_media_json(url)

        Returns media json from kiwi.

        :return: kiwi_get_media_json
    """
    indexarch_on_kiwi_sh = find_script("indexarch_on_kiwi.sh")
    try:
        media_json = subprocess.check_output([indexarch_on_kiwi_sh, "-u", url]).strip()
    except subprocess.CalledProcessError as err:
        logger.critical("failed to parse '{}' through indexarch on kiwi".format(url))
        return ""

    logger.debug("media_json: {0}".format(media_json))
    return media_json



def kiwi_get_indexarch_etalon(url):
    """
    .. py:function:: kiwi_get_indexarch_etalon  get etalon as the result of running indexarch on kiwi."
    """
    media_json = kiwi_get_media_json(url)

    if binary is None:
        raise RuntimeError("binary is not specified")

    file_descriptor, filepath = tempfile.mkstemp()
    try:
        written_bytes = os.write(file_descriptor, media_json)
        if written_bytes != len(media_json):
            raise RuntimeError("failed to write all data to file {}, only {} of {}".format(filepath, written_bytes, len(media_json)))
        os.close(file_descriptor)

        etalon_creator_process = subprocess.Popen([binary, "EtalonFromMedia", "-i", filepath], stdout = subprocess.PIPE)
        etalon = etalon_creator_process.communicate(media_json)[0]
        logger.debug("etalon:\n{0}".format(etalon))

        rc = etalon_creator_process.returncode
        if rc != 0:
            raise RuntimeError("etalon creator return code: {0}.\nFailed to create etalon from kiwi output.".format(rc))
    finally:
        os.remove(filepath)
    return etalon




def save_test_case(test_collection_dir, url, html, test_case):
    """
     .. py:function::   save_test_case(test_collection_dir, url, html, test_case)
                        Saves test case to the test collection dir.

        :param test_collection_dir: path to test collection.
        :param url:                 url of the page that was used to create test.
        :param html:                html of the page.
        :param test_case:           test case as object of quality_record_pb2.TTestCase type.
    """

    # create directory for domain.
    domain = urlparse.urlparse(url).netloc
    dirpath = os.path.join(test_collection_dir, domain)
    try:
        os.makedirs(dirpath)
    except OSError as error:
        if error.errno != errno.EEXIST or not os.path.isdir(dirpath):
            raise RuntimeError("Failed to create directory for etalon {0}. Reason: {1}".format(dirpath, str(error)))


    # check that this url is not in collection.
    urlfiles = [os.path.join(dirpath, f) for f in os.listdir(dirpath) if os.path.splitext(f)[1] == ".url"]
    same_urlfiles = sorted([urlfile for urlfile in urlfiles if slurp_file(urlfile).strip() == url])
    if same_urlfiles:
        msg = "There is already same url in the test collection: {}".format(pprint.pformat(same_urlfiles))
        raise RuntimeError(msg)

    # create names for etalon files.
    noextname = get_noextnames_from_dir_content(dirpath)
    etalon_filename = os.path.join(dirpath, noextname + ".etalon")
    html_filename = os.path.join(dirpath, noextname + ".html")
    url_filename = os.path.join(dirpath, noextname + ".url")

    # check that we're not going to override existing data and then save all the data.
    serialized_test_case = text_format.MessageToString(test_case, as_utf8 = True)
    tosave = [
        (etalon_filename, serialized_test_case),
        (html_filename, html),
        (url_filename, url)
    ]
    for filename, _ in tosave:
        if os.path.exists(filename):
            raise RuntimeError("File already exists: {0}. I would not override it!".format(filename))
    for filename, data in tosave:
        with open(filename, "w") as outfile:
            outfile.write(data)

    # notify users of what we've done.
    print "Added etalon file: {0}".format(etalon_filename)
    print "Added html file: {0}".format(html_filename)
    print "Added url file: {0}".format(url_filename)




def kiwi_get_html(url):
    """
     .. py:function::    kiwi_get_html   return html for url from kiwi.

     .. note:    Raises exception if there is no html for url of there is no url in kiwi.
    """
    html_from_kiwi_sh = find_script("html_from_kiwi.sh")
    html = subprocess.check_output([html_from_kiwi_sh, "-u", url]).strip()
    logger.debug("kiwi_get_html({0}):\n{1}".format(url, html))
    if html == "":
        raise RuntimeError("Kiwi does not have the url '{}' or html responce does not contain html.".format(url))
    return html


def add_etalon_command(args):
    """
     .. py:function:: add_etalon_command   add etalon to collection.

        :params args:   command line params without programm name and command name.
    """
    parser = argparse.ArgumentParser(description = "Run indexarch on kiwi and add new etalon to test case.")
    parser.add_argument("-d", "--dir", type = str, required = True, help = "Test case directory")
    parser.add_argument("-k", "--kiwi", type = str, default = config["kiwi_server"], help = "Kiwi server")
    parser.add_argument("-u", "--url", type = str, required = True, help = "url to parse.")
    parser.add_argument("-x", "--indexarch", action = "store_true", help = "use indexarch output as etalon.")
    parser.add_argument("-b", "--binary", type = str, help = "path to quality_test binary")
    parser.add_argument("-c", "--config", type = str, help = "Config for parsing html, just as config in vt_extractor")
    parser.add_argument("-e", "--allow-empty", dest = "allow_empty", action = "store_true", help = "Do not throw on empty etalon.")
    parser.add_argument("-s", "--allow-same", dest = "allow_same_url", action = "store_true", help = "Allow adding test case with url that already in test collection.")
    parsed_args = parser.parse_args(args)
    set_quality_tests_bin(parsed_args.binary)
    global qt_config
    qt_config = parsed_args.config

    if not os.path.isdir(parsed_args.dir):
        raise RuntimeError("There is no directory: {0}".format(parsed_args.dir))

    html = kiwi_get_html(parsed_args.url)
    etalon = kiwi_get_indexarch_etalon(parsed_args.url) if parsed_args.indexarch else vt_extractor_parse_content(html)
    if etalon.strip() == "":
        msg = "Etalon is empty."
        if parsed_args.allow_empty:
            logger.warning(msg)
        else:
            raise RuntimeError(msg)

    # @todo: make the next function work.
    save_test_case(parsed_args.dir, parsed_args.url, html, etalon)



def video_pool(args):
    """
     .. py:function: video_pool  parse result of detection video on page from toloka.

        :params args:   commandline params without programm name and command name.
    """
    parser = argparse.ArgumentParser(description = "Parse result of detection video on page from toloka.")
    parser.add_argument("-i", "--input", type = str, required = True, help = "Input file")
    parser.add_argument("-o", "--output", type = str, required = True, help = "Output file")
    parser.add_argument("-e", "--errors", type = str, required = True, help = "Errors file")

    parsed_args = parser.parse_args(args)
    input_filename = parsed_args.input
    output_filename = parsed_args.output
    error_filename = parsed_args.errors

    votes = {}
    with open(output_filename, "w") as output_file, open(error_filename, "w") as error_file:
        first_line = True
        for line in open(input_filename, "r"):
            if first_line:
                first_line = False
                continue

            line = line.rstrip()

            # extract boolean assessments.
            assessments = []
            found = True
            while found:
                false_ind = line.rfind("false")
                true_ind = line.rfind("true")
                if false_ind == len(line) - len("false"):
                    assessments = [False] + assessments
                    line = line[:false_ind]
                elif true_ind == len(line) - len("true"):
                    assessments = [True] + assessments
                    line = line[:true_ind]
                else:
                    found = False

                if found:
                    if line[-1] == "|":
                        line = line[:-1]
                    else:
                        found = False

            # skip empty fields.
            if line[-1] != "|":
                print >> error_file, line
                continue
            line = line[:-1]

            # extract urls from line
            urls = []
            curend = 0
            while curend != -1:
                ind = line.find("|http", curend)
                if ind == -1:
                    print >> error_file, line
                curend = line.find("|http", ind + 1)
                url = line[ind + 1 : ] if curend == -1  else line[ind + 1 : curend]
                urls.append(url)
            urls = urls[1:] # first url is the url to toloka assignment

            if len(urls) != len(assessments):
                print >> error_file, line
                continue

            for ind in xrange(len(urls)):
                url = urls[ind]
                vote = assessments[ind]
                this_votes = votes.get(url, [0, 0])
                this_votes[vote] += 1
                votes[url] = this_votes

        for url, vote in votes.iteritems():
            print >> output_file, "{}\t{}\t{}".format(url, vote[0], vote[1])



def read_toloka_first_line(line):
    """
     .. py:function: read_toloka_first_line(line)

        :param args: line is a first line of toloka results for video text assessments.

        :return:    indexes in line of the urls, texts and assessments
    """
    entities = line.split("|")
    urls_idxes = {}
    text_idxes = {}
    assessment_idxes = {}

    for idx, entity in enumerate(entities):
        if entity.find("IV__url__") == 0:
            urls_idxes[int(entity[len("IV__url__"):])] = idx
        elif entity.find("IV__text_") == 0:
            text_n, url_n = entity[len("IV__text_"):].split("__")
            text_idxes[(int(url_n), int(text_n))] = idx
        elif entity.find("OV__out_text_") == 0:
            text_n, url_n = entity[len("OV__out_text_"):].split("__")
            assessment_idxes[(int(url_n), int(text_n))] = idx

    n_urls = len(urls_idxes)
    n_texts = len(text_idxes)
    n_assessments = len(assessment_idxes)
    expected_n_texts = config["toloka_n_urls"] * config["toloka_n_texts"]
    if n_urls != config["toloka_n_urls"]:
        raise RuntimeError("expected {} urls in input".format(config["toloka_n_urls"]))
    if n_texts != expected_n_texts:
        raise RuntimeError("expected {} texts in input".format(expected_n_texts))
    if n_assessments != expected_n_texts:
        raise RuntimeError("expected {} assessments in input".format(expected_n_texts))

    return urls_idxes, text_idxes, assessment_idxes



def split_toloka_line(line, expected_number_of_entities = -1):
    """
     .. py:function:    split_toloka_line(line)
                        Entities in toloka line are split by | delimiter, however when value has |
                        symbol in it, the value it contained in doulbe quotes.

        :param line:    line from toloka file.
        :return:        list of values from this line.
    """
    line = line.strip()
    entities = []
    while len(line):
        if line[0] == "\"":
            end = line.find("\"", 1)
            if end == -1 or line[end + 1] != "|":
                raise RuntimeError("bad line: failed to find paired \" for quoted string")
            entities.append(line[1:end])
            line = line[end + 2:]
        else:
            end = line.find("|")
            if end == -1:
                value = line
                line = ""
            else:
                value = line[:end]
                line = line[end + 1:]
            entities.append(value)

    if expected_number_of_entities != -1 and len(entities) != expected_number_of_entities:
        raise RuntimeError("bad line: too few entities")
    return entities



def etalons_from_toloka(args):
    """
     .. py:function: etalons_from_toloka

        :params args:   commandline params without programm name and command name.
    """
    parser = argparse.ArgumentParser(description = "Parse result of text and video correspondense from toloka.")
    parser.add_argument("-i", "--input", type = str, required = True, help = "Input file")
    parser.add_argument("-o", "--output", type = str, required = True, help = "Output directory")
    parser.add_argument("-e", "--errors", type = str, required = True, help = "File with errors")

    parsed_args = parser.parse_args(args)
    input_filename = parsed_args.input
    error_filename = parsed_args.errors

    # process input file.
    assessment_results = {}
    urls_count = {}
    first_line = True
    line_number = 0
    with open(error_filename, "w") as error_file:
        for line in open(input_filename, "r"):
            line_number += 1
            if first_line:
                urls_idxes, text_idxes, assessment_idxes = read_toloka_first_line(line)
                first_line = False
                entities_size = len(line.split("|"))
                continue

            entities = split_toloka_line(line, entities_size)

            # get urls
            urls = []
            for url_idx in xrange(config["toloka_n_urls"]):
                idx = urls_idxes[url_idx]
                urls.append(entities[idx])
            if not all((url.find("http") == 0 for url in urls)):
                print >> error_file, "bad urls: {}".format(urls)
                print >> error_file, line
                continue

            # get assessments
            assessments = {}
            fail = False
            for url_idx in xrange(config["toloka_n_urls"]):
                for text_idx in xrange(1, config["toloka_n_texts"] + 1):
                    idx = (url_idx, text_idx)
                    linear_idx = assessment_idxes[idx]
                    assessments[idx] = entities[linear_idx]
                    try:
                        assessments[idx] = {"true": True, "false": False}[assessments[idx]]
                    except:
                        print >> error_file, "bad assessment value: {}".format(assessments[idx])
                        print >> error_file, line
                        fail = True
                        break
                if fail:
                    break
            if fail:
                continue

            # extract urls and texts from the line
            for url_idx in xrange(config["toloka_n_urls"]):
                url = urls[url_idx]
                urls_count[url] = urls_count.get(url, 0) + 1
                for text_idx in xrange(1, config["toloka_n_texts"] + 1):
                    idx = (url_idx, text_idx)
                    candidate_text = entities[text_idxes[idx]]
                    if candidate_text == "<empty>":
                        if assessments[idx]:
                            print >> error_file, "<empty> assessed as true"
                            print >> error_file, line
                            continue
                    else:
                        text_dict = assessment_results.get(url, {})

                        value = text_dict.get(candidate_text, 0)
                        value += int(assessments[idx])
                        text_dict[candidate_text] = value

                        assessment_results[url] = text_dict

    # select relevant texts for url.
    relevant_texts = {} # map url -> quality_record_pb2.TTestCase objects
    for url, texts in assessment_results.iteritems():
        n_assessments = urls_count[url]
        threshold = max(1, n_assessments - 2)
        for text, count in texts.iteritems():
            if count >= threshold:
                text = text.decode("utf8")
                test_case = relevant_texts.setdefault(url, quality_record_pb2.TTestCase())
                if len(test_case.Records) < 1:
                    test_case.Records.add()
                test_case.Records[0].Texts.append(text)

    """ debug output of texts:
    for url, test_case in relevant_texts.iteritems():
        print url
        for qr in test_case.Records:
            for txt in qr.Texts:
                print "\t", txt
    """

    # dump collection
    test_collection_dir = "toloka_test_collection"
    if os.path.exists(test_collection_dir):
        raise RuntimeError("directory to save collection to already exists: {}. I would not override it.".format(test_collection_dir))
    for url, test_case in relevant_texts.iteritems():
        try:
            html_content = kiwi_get_html(url)
        except RuntimeError as re:
            print "Failed to add etalon: {}".format(str(re))
        save_test_case(test_collection_dir, url, html_content, test_case)






"""
 .. py:data:: calls is a dictionary with program running modes.
"""
calls = {
    "test_all": {
        "func": test_all,
        "help": "Just do the goddamn tests. No questions asked."
    },
    "test_case": {
        "func": test_case,
        "help": "Run tests on specified test case."
    },
    "calc_qual": {
        "func": calc_qual,
        "help": "Calculate quality on parsed htmls."
    },
    "prepare_case": {
        "func": download_case,
        "help": "Prepare test case to run testing on."
    },
    "store_case": {
        "func": upload_case,
        "help": "store case in some global place."
    },
    "add_etalon": {
        "func": add_etalon_command,
        "help": "download parse html on kiwi and add the result as etalon."
    },
    "video_pool": {
        "func": video_pool,
        "help": "parser result of detection video on page from toloka."
    },
    "etalons_from_toloka": {
        "func": etalons_from_toloka,
        "help": "create etalons from toloka videos assessments."
    }
}



def usage():
    print "Usage: {0} command [args]\n\nWhere commands may be:".format(sys.argv[0])
    for call, value in calls.iteritems():
        print "\t{0}\t{1}".format(call, value["help"])



def main():
    if len(sys.argv) == 1 or sys.argv[1] not in calls:
        usage()
        sys.exit(1)
    mode = sys.argv[1]
    call = calls[mode]["func"]
    call(sys.argv[2:])



if __name__ == "__main__":
    main()
