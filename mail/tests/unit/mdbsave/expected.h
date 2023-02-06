#pragma once

#include <string>

namespace {

std::string MakeMdbSaveRequestBody() {
        return R"({"rcpts":[{"id":"0","rcpt":{"user":{"uid":"uid_0","suid":"suid_0"},"message":{"old_mid":"old_mid_0",)"
        R"("ext_imap_id":0,"firstline":"fl_0","size":1,"lids":["lid_00","lid_10"],"label_symbols":["sym_00","sym_10"],)"
        R"("labels":[{"name":"label_name_00","type":"label_type_00"},{"name":"label_name_10","type":"label_type_10"}],)"
        R"("tab":"tab_0","storage":{"stid":"stid_0","offset":2},"headers":{"recieved_date":3,"date":4,)"
        R"("subject":"subject_0","msg_id":"msg_id_0","reply_to":"reply_to_0","in_reply_to":"in_reply_to_0",)"
        R"("from":[{"local":"local_from_00","domain":"domain_from_00","display_name":"display_name_from_00"},)"
        R"({"local":"local_from_10","domain":"domain_from_10","display_name":"display_name_from_10"}],)"
        R"("to":[{"local":"local_to_00","domain":"domain_to_00","display_name":"display_name_to_00"},)"
        R"({"local":"local_to_10","domain":"domain_to_10","display_name":"display_name_to_10"}],"cc":[{"local":"local_cc_00",)"
        R"("domain":"domain_cc_00","display_name":"display_name_cc_00"},{"local":"local_cc_10","domain":"domain_cc_10",)"
        R"("display_name":"display_name_cc_10"}],"bcc":[{"local":"local_bcc_00","domain":"domain_bcc_00",)"
        R"("display_name":"display_name_bcc_00"},{"local":"local_bcc_10","domain":"domain_bcc_10",)"
        R"("display_name":"display_name_bcc_10"}]},"attachments":[{"hid":"hid_00","name":"name_00","type":"type_00","size":5},)"
        R"({"hid":"hid_10","name":"name_10","type":"type_10","size":5}],"mime_parts":[{"hid":"hid_00","content_type":"type_00",)"
        R"("content_subtype":"subtype_00","boundary":"boundary_00","name":"name_00","charset":"charset_00",)"
        R"("encoding":"encoding_00","content_disposition":"dispos_00","file_name":"file_00","content_id":"id_00","offset":6,)"
        R"("length":7},{"hid":"hid_10","content_type":"type_10","content_subtype":"subtype_10","boundary":"boundary_10",)"
        R"("name":"name_10","charset":"charset_10","encoding":"encoding_10","content_disposition":"dispos_10",)"
        R"("file_name":"file_10","content_id":"id_10","offset":6,"length":7}],)"
        R"("thread_info":{"hash":{"namespace":"ns_0","value":"value_0"},"limits":{"days":8,"count":9},)"
        R"("rule":"rule_0","reference_hashes":["hash_00","hash_10"],"message_ids":["msg_00","msg_10"],)"
        R"("in_reply_to_hash":"in_reply_to_hash_0","message_id_hash":"msg_id_hash_0"}},"folders":{"destination":)"
        R"({"fid":"fid_dest_0","path":{"path":"path_dest_0","delimeter":"|"}},"original":{"fid":"fid_orig_0",)"
        R"("path":{"path":"path_orig_0","delimeter":"|"}}},"actions":{"duplicates":{"ignore":true,"remove":false},)"
        R"("use_filters":true,"disable_push":false,"original":{"store_as_deleted":true,"no_such_folder":"fail"},)"
        R"("rules_applied":{"store_as_deleted":false,"no_such_folder":"create"}},"added_lids":["lid_00","lid_10"],)"
        R"("added_symbols":["sym_00","sym_10"],"imap":true}},{"id":"1","rcpt":{"user":{"uid":"uid_1","suid":"suid_1"},)"
        R"("message":{"old_mid":"old_mid_1","ext_imap_id":0,"firstline":"fl_1","size":1,)"
        R"("lids":["lid_01","lid_11"],"label_symbols":["sym_01","sym_11"],"labels":[{"name":"label_name_01",)"
        R"("type":"label_type_01"},{"name":"label_name_11","type":"label_type_11"}],"tab":"tab_1",)"
        R"("storage":{"stid":"stid_1","offset":2},"headers":{"recieved_date":3,"date":4,"subject":"subject_1",)"
        R"("msg_id":"msg_id_1","reply_to":"reply_to_1","in_reply_to":"in_reply_to_1","from":[{"local":"local_from_01",)"
        R"("domain":"domain_from_01","display_name":"display_name_from_01"},{"local":"local_from_11","domain":"domain_from_11",)"
        R"("display_name":"display_name_from_11"}],"to":[{"local":"local_to_01","domain":"domain_to_01",)"
        R"("display_name":"display_name_to_01"},{"local":"local_to_11","domain":"domain_to_11",)"
        R"("display_name":"display_name_to_11"}],"cc":[{"local":"local_cc_01","domain":"domain_cc_01","display_name":)"
        R"("display_name_cc_01"},{"local":"local_cc_11","domain":"domain_cc_11","display_name":"display_name_cc_11"}],)"
        R"("bcc":[{"local":"local_bcc_01","domain":"domain_bcc_01","display_name":"display_name_bcc_01"},)"
        R"({"local":"local_bcc_11","domain":"domain_bcc_11","display_name":"display_name_bcc_11"}]},)"
        R"("attachments":[{"hid":"hid_01","name":"name_01","type":"type_01","size":5},{"hid":"hid_11",)"
        R"("name":"name_11","type":"type_11","size":5}],"mime_parts":[{"hid":"hid_01","content_type":"type_01",)"
        R"("content_subtype":"subtype_01","boundary":"boundary_01","name":"name_01","charset":"charset_01",)"
        R"("encoding":"encoding_01","content_disposition":"dispos_01","file_name":"file_01","content_id":"id_01",)"
        R"("offset":6,"length":7},{"hid":"hid_11","content_type":"type_11","content_subtype":"subtype_11",)"
        R"("boundary":"boundary_11","name":"name_11","charset":"charset_11","encoding":"encoding_11",)"
        R"("content_disposition":"dispos_11","file_name":"file_11","content_id":"id_11","offset":6,"length":7}],)"
        R"("thread_info":{"hash":{"namespace":"ns_1","value":"value_1"},"limits":{"days":8,"count":9},"rule":"rule_1",)"
        R"("reference_hashes":["hash_01","hash_11"],"message_ids":["msg_01","msg_11"],"in_reply_to_hash":"in_reply_to_hash_1",)"
        R"("message_id_hash":"msg_id_hash_1"}},"folders":{"destination":{"fid":"fid_dest_1","path":{"path":"path_dest_1",)"
        R"("delimeter":"|"}},"original":{"fid":"fid_orig_1","path":{"path":"path_orig_1","delimeter":"|"}}},"actions":{)"
        R"("duplicates":{"ignore":true,"remove":false},"use_filters":true,"disable_push":false,"original":{"store_as_deleted")"
        R"(:true,"no_such_folder":"fail"},"rules_applied":{"store_as_deleted":false,"no_such_folder":"create"}},)"
        R"("added_lids":["lid_01","lid_11"],"added_symbols":["sym_01","sym_11"],"imap":true}}],"sync":false})";
    }

std::string MakeMdbSaveResponseBody() {
        return R"({"rcpts":[{"id":"0","rcpt":{"uid":"uid_0","status":"ok","mid":"mid_0","imap_id":"imap_id_0",)"
        R"("tid":"tid_0","duplicate":false,"folder":{"fid":"fid_0","name":"name_0","type":"type_0",)"
        R"("type_code":123},"labels":[{"lid":"lid_0","symbol":"symbol_0"},{"lid":"lid_1","symbol":"symbol_1"}]}},)"
        R"({"id":"1","rcpt":{"uid":"uid_1","status":"perm error","description":"perm error happened"}},)"
        R"({"id":"2","rcpt":{"uid":"uid_2","status":"temp error","description":"temp error happened"}}]})";
}

}
