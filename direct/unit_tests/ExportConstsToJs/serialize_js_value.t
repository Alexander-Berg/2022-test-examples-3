#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;

use Test::More;

use ExportConstsToJs;

*serialize_js_value=\&ExportConstsToJs::serialize_js_value;

is(serialize_js_value({xxx => {aaa => "bbb", ccc => ["ddd", 666, "тест"], ddd => '', 222 => 333.444}}), q/{
    "xxx" : {
        "222" : 333.444,
        "aaa" : "bbb",
        "ccc" : [
            "ddd",
            666,
            iget("тест")
        ],
        "ddd" : ""
    }
}/, 'сериализация структуры');
is(serialize_js_value({xxx => "yyy"}, ident => 1, no_iget => 1), q/{
     "xxx" : "yyy"
 }/, 'сериализация с отключенным iget и дополнительным identом');

done_testing();
