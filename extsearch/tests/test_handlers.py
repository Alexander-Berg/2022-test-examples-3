# -*- coding: utf-8 -*-

import json
import jsondiff
import argparse
import requests
import time

JSON_INDENT = 4
RETRIES = 5
BLOCK_WITH_DATA = "backend_response"


# HANDLERS LISTS START
def videolessons_handlers_list(failing):
    grade_9 = "9-klass/"
    grade_11 = "11-klass/"
    grade_missing = "8-klass/"
    subject_math11 = "profilnaya-matematika/"
    subject_russian11 = "russkij-yazyk/"
    subject_math9 = "matematika/"
    subject_russian9 = "russkij-yazyk/"
    subject_missing = "teoriya-veroyatnostej/"
    variantid_math11_2 = "3917"
    variantid_usual = "310"
    variantid_custom = "4204"
    videoid_math11_1 = "4050c04b4daf5dcbab01ac28a62a3fcf"
    videoid_math11_2 = "4b803e59954f25d1993b33eea84ab3b9"
    lesson_math11_1 = "31-03-matematika-podgotovka-k-egeh-profilnyj-uroven-1-vvodnyj-urok_" + videoid_math11_1 + "/"
    lesson_math11_2 = "01-04-matematika-podgotovka-k-egeh-profilnyj-uroven-2-egeh-profilnyj-uroven-arifmeticheskie-zadachi-zadanie-1_" + videoid_math11_2 + "/"
    lesson_wrong_video_id = "31-03-matematika-podgotovka-k-egeh-profilnyj-uroven-1-vvodnyj-urok_" + videoid_math11_2 + "/"
    lesson_no_video_id = "31-03-matematika-podgotovka-k-egeh-profilnyj-uroven-1-vvodnyj-urok/"
    lesson_missing_video_id = "31-03-matematika-podgotovka-k-egeh-profilnyj-uroven-1-vvodnyj-urok_1/"
    lesson_none = "/"
    report_usualvariant = "report/?report_id=ba3d014325f108029e2580b1afa46284"
    report_customvariant = "report/?report_id=26a09fdcdba74752e4bfe17a3de48461"
    report_missing = "report/?report_id=ba3d014325f108029e2580b1afa46280"
    report_math11_2 = "report/?report_id=4666cdb0ecba91f994eccbce15cf76ed"
    report_none = "report/"

    if not failing:
        return [
            RequestData("/tutor/uroki/" + grade_11),
            RequestData("/tutor/uroki/" + grade_9),

            RequestData("/tutor/uroki/" + grade_11 + subject_math11),
            RequestData("/tutor/uroki/" + grade_11 + subject_russian11),
            RequestData("/tutor/uroki/" + grade_9 + subject_math9),
            RequestData("/tutor/uroki/" + grade_9 + subject_russian9),

            RequestData("/tutor/subject/variant/?subject_id=1&variant_id=" + variantid_usual),
            RequestData("/tutor/subject/variant/?subject_id=1&variant_id=" + variantid_usual + "&embed_video_id=" + videoid_math11_1),
            RequestData("/tutor/subject/variant/?subject_id=1&variant_id=" + variantid_custom),
            RequestData("/tutor/subject/variant/?subject_id=1&variant_id=" + variantid_custom + "&embed_video_id=" + videoid_math11_1),
            RequestData("/tutor/subject/variant/" + report_usualvariant),
            RequestData("/tutor/subject/variant/" + report_customvariant),

            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_1),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_2 + report_math11_2),
        ]
    else:  # if failing
        return [
            RequestData("/tutor/uroki/"),  # redirect
            RequestData("/tutor/uroki/" + grade_missing),
            RequestData("/tutor/uroki/" + grade_11 + subject_missing),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_no_video_id),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_missing_video_id),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_1 + report_usualvariant),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_1 + report_customvariant),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_1 + report_missing),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_missing_video_id + report_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_none + report_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_1 + report_math11_2),  # wrong lesson

            RequestData("/tutor/uroki/other-gr/other-subj/other-title_11_" + videoid_math11_2 + "/"),
            RequestData("/tutor/subject/variant/?subject_id=1&variant_id=" + variantid_math11_2),

            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_wrong_video_id + report_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_no_video_id + report_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_missing_video_id + report_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_none + report_math11_2),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_2 + report_missing),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_2 + report_none),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_2 + report_usualvariant),
            RequestData("/tutor/uroki/" + grade_11 + subject_math11 + lesson_math11_2 + report_customvariant),

            RequestData("/tutor/subject/variant/report/?report_id=" + report_math11_2),  # redirect from report
        ]


def get_get_handlers_list():
    handlers_list = [

        # Main
        RequestData("/tutor/"),

        # Yashchenko
        RequestData("/tutor/subject/lesson/test/?lesson_id=1&level=1&subject_id=1"),
        RequestData("/tutor/subject/lesson/lecture/?lesson_id=1&level=1&subject_id=1"),
        RequestData("/tutor/subject/lesson/tasks/?lesson_id=1&level=1&subject_id=1"),
        RequestData("/tutor/subject/lesson/home/?lesson_id=1&level=1&subject_id=1"),
        RequestData("/tutor/subject/lesson/lecture/?lesson_id=1&level=3&subject_id=1"),

        # Subjects
        RequestData("/tutor/subject/?subject_id=1"),
        RequestData("/tutor/subject/?subject_id=15"),
        RequestData("/tutor/subject/?author=9&subject_id=2&year=2020"),
        RequestData("/tutor/subject/?author=1&subject_id=20&year=2020"),
        RequestData("/tutor/subject/?author=3&subject_id=16&year=2019"),
        RequestData("/tutor/subject/?subject_id=9"),

        # Problems
        RequestData("/tutor/subject/problem/?problem_id=T1"),
        RequestData("/tutor/subject/problem/?problem_id=T302"),
        RequestData("/tutor/subject/problem/?problem_id=T9"),
        RequestData("/tutor/subject/problem/?problem_id=T18"),
        RequestData("/tutor/subject/problem/?problem_id=T46"),
        RequestData("/tutor/subject/problem/?problem_id=T86"),
        RequestData("/tutor/subject/problem/?problem_id=T216"),
        RequestData("/tutor/subject/problem/?problem_id=T9783"),
        RequestData("/tutor/subject/problem/?problem_id=T9784"),
        RequestData("/tutor/subject/problem/?problem_id=T9792"),
        RequestData("/tutor/subject/problem/?problem_id=T8410"),
        RequestData("/tutor/subject/problem/?problem_id=T8530"),
        RequestData("/tutor/subject/problem/?problem_id=T1189"),
        RequestData("/tutor/subject/problem/?problem_id=T9424"),
        RequestData("/tutor/subject/problem/?problem_id=T5698"),
        RequestData("/tutor/subject/problem/?problem_id=T5707"),

        # Tags
        RequestData("/tutor/subject/tag/problems/?ege_number_id=160&tag_id=19"),
        RequestData("/tutor/subject/tag/problems/?ege_number_id=279&tag_id=19"),
        RequestData("/tutor/subject/tag/problems/?ege_number_id=229&tag_id=175"),
        RequestData("/tutor/subject/tag/problems/?ege_number_id=2065&tag_id=19"),

        # Variants
        RequestData("/tutor/subject/variant/?subject_id=1&variant_id=339"),
        RequestData("/tutor/subject/variant/?subject_id=8&variant_id=238"),
        RequestData("/tutor/subject/variant/?subject_id=16&variant_id=177"),
        RequestData("/tutor/subject/variant/?variant_id=123"),
        RequestData("/tutor/subject/theory/?subject_id=1"),

        # Reports
        RequestData("/tutor/subject/variant/report/?report_id=3161456204ed072d61397761bba9bf62"),

        # Statistics
        RequestData("/tutor/user/statistics/"),
        RequestData("/tutor/user/statistics/?subject_id=1"),
        RequestData("/tutor/user/achievements/"),

        # Variants constructor
        RequestData("/tutor/user/variants/"),
        RequestData("/tutor/user/variants/edit/?variant_id=1392"),

        # Schedule
        RequestData("/tutor/journal/raspisanie-ege-2020/"),
        RequestData("/tutor/journal/raspisanie-oge-2020/"),
        RequestData("/tutor/journal/perevod-ballov-ege/"),
    ] + videolessons_handlers_list(failing=False)
    return handlers_list


def get_failing_get_handlers_list():
    handlers_list = [
        RequestData("/tutor/subject/problem/?problem_id=D216"),
        RequestData("/tutor/subject/variant/report/?report_id=not1enough1characters"),
        RequestData("/tutor/subject/variant/report/?report_id=enough1characters1but1falsy1id11"),
        RequestData("/tutor/subject/variant/?subject_id=16&variant_id=177777777"),
        RequestData("/tutor/subject/variant/?subject_id=166666666&variant_id=177"),
        RequestData("/tutor/subject/theory/?subject_id=10"),
        RequestData("/tutor/user/statistics/?subject_id=123456"),

        RequestData("/tutor/subject/lesson/lecture/?lesson_id=1&level=50&subject_id=1"),
        RequestData("/tutor/subject/lesson/lecture/?lesson_id=1&level=4&subject_id=100000"),
        RequestData("/tutor/subject/lesson/lecture/?lesson_id=100000&level=4&subject_id=1"),

        RequestData("/tutor/user/variants/edit/?variant_id=1000000000"),
    ] + videolessons_handlers_list(failing=True)

    for handler in handlers_list:
        handler.should_fail = True

    return handlers_list


def get_post_handlers_list():
    handlers_list = []

    data_check_problem_1 = {"problem_id": "T1",  "user_answer": "3", "subject_id": 1}
    handlers_list.append(RequestData("/tutor/problem_check/", data_check_problem_1))

    data_check_problem_2 = {"problem_id": "T1",  "user_answer": "WRONG", "subject_id": 1}
    handlers_list.append(RequestData("/tutor/problem_check/", data_check_problem_2))

    data_check_variant_1 = {"user_answers": [{"id": "T1", "value": "3"}], "variant_id": 1, "attempt_number": 1, "subject_id": 1}
    handlers_list.append(RequestData("/tutor/subject/variant/finish/", data_check_variant_1))

    data_check_variant_2 = {"user_answers": [{"id": "T1", "value": "3"}, {"id": "T2"}, {"id": "T3"}],
                            "variant_id": 1, "attempt_number": 1, "subject_id": 1}
    handlers_list.append(RequestData("/tutor/subject/variant/finish/", data_check_variant_2))

    create_variant_data = {
        "subject_id": 1,
        "title": "Custom variant title",
        "description": "Custom variant description",
        "duration": 42
    }
    handlers_list.append(RequestData("/tutor/user/variants/create/", create_variant_data))

    update_variant_data = {
        "variant_id": 1392,
        "problems": [1318, 1663, 1584, 296, 5],
        "title": "Custom variant title",
        "description": "Custom variant description",
        "duration": 42,
        "status": "public"
    }
    handlers_list.append(RequestData("/tutor/user/variants/update/", update_variant_data))

    return handlers_list


def get_failing_post_handlers_list():
    handlers_list = []

    data_check_variant_1 = {"user_answers": [{"id": "T21", "value": "WA"}], "variant_id": 1, "attempt_number": 1, "subject_id": 1}
    handlers_list.append(RequestData("/tutor/subject/variant/finish/", data_check_variant_1))

    for handler in handlers_list:
        handler.should_fail = True

    return handlers_list


class BetaData(object):
    def __init__(self, path, srcrwr, alias, server_time, passport_uid, need_dump, dump_file, need_print, check_post):
        self.path = path
        self.srcrwr = srcrwr
        self.alias = alias
        self.server_time = convert_arg_to_cgi("server_time", server_time)
        self.passport_uid = convert_arg_to_cgi("passport_uid", passport_uid)
        self.need_dump = need_dump
        self.need_print = need_print
        self.check_post = check_post

        if need_dump:
            self.dump_file = open(dump_file, "w")
            self.got_first_line = False

        self.session = requests.Session()

        handlers_list = []
        handlers_list += get_get_handlers_list()
        handlers_list += get_failing_get_handlers_list()
        self.urls_list = self.generate_urls_from_handlers(handlers_list)

        if self.check_post:
            post_handlers_list = get_post_handlers_list()
            post_handlers_list += get_failing_post_handlers_list()
            self.urls_list += self.get_post_urls_list(post_handlers_list)

    def append_params(self):
        all_params = self.passport_uid + self.server_time

        return all_params

    def output_message(self, message):
        if self.need_print:
            print message
        if self.need_dump:
            self.dump_file.write("\n" + message)

    def output_json_data(self, data, url):
        out_data = json.dumps(data, ensure_ascii=False, indent=JSON_INDENT).encode("utf-8")
        if self.need_dump:
            if not self.got_first_line:
                self.got_first_line = True
            else:
                self.dump_file.write("\n\n\n")

            self.dump_file.write(url + "\n")
            self.dump_file.write(out_data)
            self.dump_file.write("\n")

    def generate_urls_from_handlers(self, handlers_list):
        urls_list = []

        for handler in handlers_list:
            handler_with_srcrwr = get_url_with_srcrwr(handler.url, self.srcrwr)

            full_url = self.path + handler_with_srcrwr + self.append_params()
            urls_list.append(RequestData(full_url, handler.data, handler.headers, handler.should_fail))

        return urls_list

    def get_post_urls_list(self, handlers_list):
        urls_list = self.generate_urls_from_handlers(handlers_list)

        csrf_token = self.get_csrf_token(self.path, self.srcrwr, self.session)

        if csrf_token is None:
            exception_message = "Couldn't recieve csrf token for: \n  \
                                 path_to_beta: " + self.path + "\n \
                                 srcrwr: " + self.srcrwr + "\n \
                                 alias: " + self.alias
            raise Exception(exception_message)

        headers = {}
        headers["Origin"] = self.path
        headers["Referer"] = self.path
        headers["x-csrf-token"] = csrf_token

        for url in urls_list:
            url.headers = headers

        return urls_list

    def get_response(self, request_data):
        success = False
        internal_code = 0

        for i in range(0, RETRIES):
            try:
                if not request_data.data:
                    req = self.session.get(request_data.url, headers=request_data.headers)
                else:
                    req = self.session.post(request_data.url, json=request_data.data, headers=request_data.headers)
            except:
                continue

            code_info = ""
            if req.status_code == 200:
                response_json = json.loads(req.text)

                data_block = get_block_with_data(response_json)

                internal_code = data_block["code"]

                if "code_info" in data_block:
                    code_info = data_block["code_info"]

                if (internal_code == 200 and not request_data.should_fail) or \
                   (internal_code != 200 and request_data.should_fail):
                    success = True
                    break

        if internal_code != 0:
            self.output_json_data(data_block, request_data.url)

        result = None

        if success:
            result = response_json
        else:
            error_message = "Unexpected answer for url: " + request_data.url

            if req:
                error_message += "\nResponse code: " + str(req.status_code)
            else:
                error_message += "Couldn't get data. Connection was terminated"

            if internal_code != 0:
                error_message += "\nInternal response code: " + str(internal_code)
            error_message += "\n" + code_info

            self.output_message(error_message)

        return result

    def get_csrf_token(self, path_to_beta, srcrwr, session):
        url = path_to_beta + get_url_with_srcrwr("/tutor/subject/?subject_id=1", srcrwr) + self.append_params()
        json_data = self.get_response(RequestData(url))

        if json_data is None:
            return None
        else:
            return json_data["backend_meta"]["sk"]["data"]


class RequestData(object):
    def __init__(self, url, data={}, headers={}, should_fail=False):
        self.url = url
        self.data = data
        self.headers = headers
        self.should_fail = should_fail


def convert_arg_to_cgi(cgi_name, arg):
    if arg == "":
        return ""
    else:
        return '&' + cgi_name + '=' + arg


def get_block_with_data(json_data):
    return json_data[BLOCK_WITH_DATA]


def get_url_with_srcrwr(url, srcrwr):
    if url[len(url) - 1] == '/':
        first_char = '?'
    else:
        first_char = '&'

    if srcrwr != "":
        srcrwr_str = "&srcrwr=EDUCATION_BACKEND:" + srcrwr
    else:
        srcrwr_str = ""

    return url + first_char + "dump=json" + srcrwr_str


def check_responses(beta_1, beta_2, need_print, need_dump, diff_file):
    success = 0

    for i in range(0, len(beta_1.urls_list)):
        req1 = beta_1.urls_list[i]
        req2 = beta_2.urls_list[i]
        if need_print:
            print "\n" + req1.url + "\nVS\n" + req2.url

        response1 = beta_1.get_response(req1)
        response2 = beta_2.get_response(req2)

        if (response1 is None) or (response2 is None):
            print "Checking failed."
            print "URL 1: " + req1.url
            print "URL 2: " + req2.url
            print
            continue

        json1 = response1[BLOCK_WITH_DATA]
        json2 = response2[BLOCK_WITH_DATA]

        diff1 = jsondiff.diff(json1, json2)
        diff2 = jsondiff.diff(json2, json1)

        output_message = ""

        if diff1 != diff2:
            output_message = "\n\nDiff for\n" + req1.url + "\nVS\n" + req2.url
            output_message += "\n\njsondiff for {}:{}\n".format(beta_1.alias, str(diff1))
            output_message += "\n\njsondiff for {}:{}\n".format(beta_2.alias, str(diff2))
            if need_dump:
                diff_file.write(output_message)
        else:
            success += 1
            output_message = "OK"

        if need_print:
            print output_message

    return success


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--path_to_beta_1",
        required=False,
        default='https://hamster.yandex.ru',
        help="Warning: urls are got from this beta! Example: https://hamster.yandex.ru")

    parser.add_argument(
        "--path_to_beta_2",
        required=False,
        default='https://hamster.yandex.ru',
        help="Example: https://hamster.yandex.ru")

    parser.add_argument(
        "--srcrwr1",
        required=False,
        default='',
        help="Example: suggest-dev5.search.yandex.net:10112")

    parser.add_argument(
        "--srcrwr2",
        required=False,
        default='')

    parser.add_argument(
        "--alias1",
        required=False,
        default="beta 1",
        help="Alias for output for beta 1")

    parser.add_argument(
        "--alias2",
        required=False,
        default="beta 2",
        help="Alias for output for beta 2")

    parser.add_argument(
        "--dump_file_1",
        required=False,
        default="dump_1.json",
        help="File to dump json data for beta 1")

    parser.add_argument(
        "--dump_file_2",
        required=False,
        default="dump_2.json",
        help="File to dump json data for beta 2")

    parser.add_argument(
        "--diff_file",
        required=False,
        default="dump_diff.json",
        help="File to dump json diff")

    parser.add_argument(
        "--result_file",
        required=False,
        default="result.txt",
        help="File with result")

    parser.add_argument(
        "--check_post",
        required=False,
        action='store_true',
        help="Check POST handlers")

    parser.add_argument(
        "--need_print",
        required=False,
        action="store_true",
        help="Print diff")

    parser.add_argument(
        "--need_dump",
        required=False,
        action="store_true",
        help="Dump json data to files")

    parser.add_argument(
        "--server_time",
        required=False,
        default=str(int(time.time())),
        help="server_time argument. Default: 1")

    parser.add_argument(
        "--passport_uid",
        required=False,
        default="handlers_test_user",
        help="User that will be passed in passport_uid CGI-parameter")

    args = parser.parse_args()

    beta_1 = BetaData(args.path_to_beta_1,
                          args.srcrwr1,
                          args.alias1,
                          args.server_time,
                          args.passport_uid,
                          args.need_dump,
                          args.dump_file_1,
                          args.need_print,
                          args.check_post)
    beta_2 = BetaData(args.path_to_beta_2,
                          args.srcrwr2,
                          args.alias2,
                          args.server_time,
                          args.passport_uid,
                          args.need_dump,
                          args.dump_file_2,
                          args.need_print,
                          args.check_post)

    if args.need_dump:
        diff_file = open(args.diff_file, "w")
    else:
        diff_file = None

    success = check_responses(beta_1, beta_2, args.need_print, args.need_dump, diff_file)
    total = len(beta_1.urls_list)
    result = ""

    if total == success:
        result += "OK"
    else:
        result += "FAIL"

    result += "\nPASSED: " + str(success) + ' / ' + str(len(beta_1.urls_list))

    with open(args.result_file, "w") as result_file:
        result_file.write(result)
    if args.need_print:
        print "\nResult:"
        print result

    if args.need_dump:
        beta_1.dump_file.close()
        beta_2.dump_file.close()
        diff_file.close()
