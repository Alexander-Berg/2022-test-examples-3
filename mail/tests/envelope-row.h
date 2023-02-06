#ifndef MACS_PG_TESTS_ENVELOPE_ROW_H_153910072015
#define MACS_PG_TESTS_ENVELOPE_ROW_H_153910072015

#include <internal/reflection/envelope.h>

namespace tests {

inline void fillDefaultEnvelopeRowData( macs::pg::reflection::Envelope & data) {
    data.mid = 12345;
    data.fid = 1;
    data.tid = 2;
    data.imap_id = 111;
    data.seen = false;
    data.recent = false;
    data.deleted = false;
    data.st_id = "mulca:2:23234.62776296.3123456844257369311306655180";
    data.received_date = 1;
    data.size = 1024;
    data.subject = "Into the cave";
    data.firstline = "Here must be dragons...";
    data.hdr_date = 22;
    data.hdr_message_id = "<NM62F0599D400D232BCozon_prod_mid1@news.ozon.ru>";
    data.extra_data = "Here can be any shit";
    data.in_reply_to = "<1814792997.8636801336707460201.JavaMail.web@wbid002cnc.rim.net>";
    data.revision = 5;
}

} // namespace tests

#endif /* MACS_PG_TESTS_ENVELOPE_ROW_H_153910072015 */
