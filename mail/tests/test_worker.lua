package.path = "../worker.lua;" .. package.path
require 'worker'


assert(url_handler("") == "")
assert(url_handler("save_draft") == "save_draft")
assert(url_handler("///save_draft") == "_save_draft")
assert(url_handler("save_draft///") == "save_draft_")
assert(url_handler("callback/send_delayed_message") == "callback_send_delayed_message")
assert(url_handler("/////callback/send_delayed_message///???foo=bar") == "_callback_send_delayed_message_")


assert(status_to_category(600) == "strange_code")
assert(status_to_category(99) == "strange_code")
assert(status_to_category(500) == "5xx")
assert(status_to_category(501) == "5xx")
assert(status_to_category(400) == "4xx")
assert(status_to_category(401) == "4xx")
assert(status_to_category(300) == "3xx")
assert(status_to_category(301) == "3xx")
assert(status_to_category(200) == "2xx")
assert(status_to_category(201) == "2xx")
assert(status_to_category(100) == "1xx")
assert(status_to_category(101) == "1xx")


assert(with_ctype({uri="/save_draft", arg_sc="js_ololo", arg_v="1", arg_caller="caller"}, "name") == "ctype=caller@1@js_ololo@_save_draft;name")
assert(with_ctype({uri="", arg_sc="js_ololo", arg_v="1", arg_caller="caller"}, "name") == "ctype=caller@1@js_ololo@;name")
assert(with_ctype({uri="/save_draft", arg_sc="", arg_v="1", arg_caller="caller"}, "name") == "ctype=caller@1@@_save_draft;name")
assert(with_ctype({uri="/save_draft", arg_sc="js_ololo", arg_v="", arg_caller="caller"}, "name") == "ctype=caller@@js_ololo@_save_draft;name")
assert(with_ctype({uri="/save_draft", arg_sc="js_ololo", arg_v="1", arg_caller=""}, "name") == "ctype=@1@js_ololo@_save_draft;name")
assert(with_ctype({uri="/save_draft", arg_sc="js_ololo", arg_v="1", arg_caller=""}, "__name") == "ctype=@1@js_ololo@_save_draft;_name")


print("Test succeed!")
