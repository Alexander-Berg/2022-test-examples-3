#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Promocodes qw/is_promocode_entry_available/;


my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{uid => 501, cid => 301, type => 'text', sum => 1000, sum_last => 1000, sum_spent => 500},
                  {uid => 501, cid => 302, type => 'performance', sum => 1000, sum_last => 1000, sum_spent => 500},
                  {uid => 502, cid => 303, type => 'text', sum => 0, sum_last => 1000, sum_spent => 0},
                  {uid => 515, cid => 320, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
            ],
            2 => [{uid => 503, cid => 304, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 503, cid => 305, type => 'dynamic', sum => 0, sum_last => 0, sum_spent => 0}],
            3 => [{uid => 504, cid => 306, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 505, cid => 307, type => 'text', sum => 0, sum_last => 0, sum_spent => 0}],
            4 => [{uid => 506, cid => 308, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 506, cid => 309, type => 'geo', sum => 1000, sum_last => 0, sum_spent => 0},
                  {uid => 507, cid => 310, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 508, cid => 311, type => 'dynamic', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 509, cid => 312, type => 'mobile_content', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 509, cid => 315, type => 'mobile_content', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 510, cid => 313, type => 'mobile_content', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 510, cid => 314, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 511, cid => 316, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 512, cid => 317, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 513, cid => 318, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  {uid => 514, cid => 319, type => 'text', sum => 0, sum_last => 0, sum_spent => 0},
                  ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{pid => 420, cid => 320}],
            2 => [{pid => 404, cid => 304}],
            3 => [{pid => 406, cid => 306}, {pid => 407, cid => 307}],
            4 => [{pid => 408, cid => 308}, {pid => 410, cid => 310}, {pid => 411, cid => 311},
                  {pid => 412, cid => 312}, {pid => 415, cid => 315}, {pid => 413, cid => 313}, {pid => 414, cid => 314},
                  {pid => 416, cid => 316}, {pid => 417, cid => 317}, {pid => 418, cid => 318}, {pid => 419, cid => 319}],
        },
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{bid => 14, pid => 420, cid => 320, domain => "www.prefixed.com"}],
            2 => [{pid => 404, cid => 304, domain => "lenta.ru"}],
            3 => [{bid => 1, pid => 406, cid => 306, domain => "thirdlevel.lenta.ru"},
                  {bid => 2, pid => 407, cid => 307, domain => undef, vcard_id => 1},
                  {bid => 9, pid => 407, cid => 307, domain => undef, vcard_id => 2}],
            4 => [{bid => 3, pid => 408, cid => 308, domain => "mail.ru"},
                  {bid => 4, pid => 410, cid => 310, domain => "nonmirror.ru"},
                  {bid => 5, pid => 411, cid => 311, domain => "somedomain.ru"},
                  {bid => 6, pid => 412, cid => 312, domain => "lenta.ru"},
                  {bid => 9, pid => 415, cid => 315, domain => "itunes.apple.com"},
                  {bid => 7, pid => 413, cid => 313, domain => "lenta.ru"},
                  {bid => 8, pid => 414, cid => 314, domain => "lenta2.ru"},
                  {bid => 10, pid => 416, cid => 316, domain => "onedomain.ru"},
                  {bid => 11, pid => 416, cid => 316, domain => "anotherdomain.ru"},
                  {bid => 12, pid => 417, cid => 317, domain => "domain.ru"},
                  {bid => 13, pid => 417, cid => 317, domain => "www.domain.ru"},
                  {bid => 14, pid => 418, cid => 318, domain => "??????????????-??????-????????????.????"},
                  {bid => 15, pid => 419, cid => 319, domain => "xn--80atjc.xn--p1ai"},
                  ],
        },
    },
    adgroups_dynamic => {
        original_db => PPC(shard => 'all'),
        rows => {
            4 => [{pid => 411, main_domain_id => 1}],
        },
    },
    domains => {
        original_db => PPC(shard => 'all'),
        rows => {
            4 => [{domain_id => 1, domain => "nonmirror.ru"}],
        },
    },
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            3 => [{vcard_id => 1, phone => "+7#800#725-57-25#"}],
            3 => [{vcard_id => 1, phone => "+7#800#725-57-26#"}],
        },
    },
    api_domain_stat => {
        original_db => PPCDICT,
        rows => [
            {filter_domain => "lenta.ru", shows_approx => 1},
            {filter_domain => "78007255725.phone", shows_approx => 1},
            {filter_domain => "mirror.ru", shows_approx => 1},
            {filter_domain => "mail.ru", accepted_items => 1},
            {filter_domain => "xn-----6kcfcsdqkn3bcmrwr4tc.xn--p1ai", shows_approx => 1},
            {filter_domain => "????????.????", shows_approx => 0},
            {filter_domain => "prefixed.com", shows_approx => 2},
        ],
    },
    mirrors_correction => {
        original_db => PPCDICT,
    },
    mirrors => {
        original_db => PPCDICT,
        rows => [
            {mirror => "mirror.ru", domain => "nonmirror.ru"},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 501, ClientID => 123},
            {uid => 502, ClientID => 124},
            {uid => 503, ClientID => 125},
            {uid => 504, ClientID => 126},
            {uid => 505, ClientID => 127},
            {uid => 506, ClientID => 128},
            {uid => 507, ClientID => 129},
            {uid => 508, ClientID => 130},
            {uid => 509, ClientID => 131},
            {uid => 510, ClientID => 132},
            {uid => 511, ClientID => 133},
            {uid => 512, ClientID => 134},
            {uid => 513, ClientID => 135},
            {uid => 514, ClientID => 136},
            {uid => 515, ClientID => 137},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 123, shard => 1},
            {ClientID => 124, shard => 1},
            {ClientID => 125, shard => 2},
            {ClientID => 126, shard => 3},
            {ClientID => 127, shard => 3},
            {ClientID => 128, shard => 4},
            {ClientID => 129, shard => 4},
            {ClientID => 130, shard => 4},
            {ClientID => 131, shard => 4},
            {ClientID => 132, shard => 4},
            {ClientID => 133, shard => 4},
            {ClientID => 134, shard => 4},
            {ClientID => 135, shard => 4},
            {ClientID => 136, shard => 4},
            {ClientID => 137, shard => 1},
        ],
    },
);

init_test_dataset(\%db);

Test::More::plan(tests => 15);

# ??????????????????, ?????? ???????? ???????? ????????????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(501) == 0, "No promocode on paid campaigns");
ok(is_promocode_entry_available(502) == 0, "No promocode on paid campaigns v2");
# ??????????????????, ?????? ???????? ?????????? ???? ????????, ???? ?????????? ???????? ?? ????????????????????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(503) == 0, "No promocode on used domain");
# ??????????????????, ?????? ???????? ?????????? ???? ????????, ???????????? ?????? ?? ????????????????????, ???? ???? ???????????????? ????????????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(504) == 0, "No promocode on third level domain");
# ??????????????????, ?????? ???????? ?????????? ???? ????????, ???? ?? ???????????????? ???????????? ??????????????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(505) == 0, "No promocode on phones only");
# ??????????????????, ?????? ???????? ?????????? ???? ????????, ?? ?????????????? ???????????? ???????? ?? ????????????????????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(507) == 0, "No promocode on used domain mirror");
# ???????????????????? ?????? ???????????? ???? ????????????????
ok(is_promocode_entry_available(508) == 0, "No promocode on used domain in dynamics");
# ???????? ???????? ???????????? ?????? ????????????????, ???? ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(509) == 0, "No promocode for mobile_content only");
# ??????????????????, ?????? ???????? ?????????? ???? ????????, ?????????? ???????? ?? ????????????????????, ?? ?????????? ?????????????????????? ?????? ?? ?????? ????????????????, ?????? ?? ?? ????-??????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(510) == 0, "No promocode on used domain (include mobile_content)");
# ??????????????????, ?????? ???????? ???????????????????? ???????????? ?????????? ???? ?????????????????? ???????????? ?????????????? - ???????????????? ????????????????????
ok(is_promocode_entry_available(511) == 0, "No promocode on different domain");

# ??????????????????, ?????? ???????? ?????????? ???? ????????, ???????????? ?????? ?? ????????????????????, ???????????????? ?????????????? ??????????
# ?????? ???????? ???????????????????? ???????????????? ???? ???????????????? ??????, ?????? ?????? ?????? ???? ?????????????? ??????????????
ok(is_promocode_entry_available(506) == 1, "Promocode allowed on unused domain and unpaid campaign");
# ??????????????????, ?????? ???????? ?????????? ???? ????????, ???????????? ?????? ?? ????????????????????, ???? ?????????????? ?? ?????????????? ?????????????????????? ???????????????? www, ???????????????? ?????????????? ??????????
ok(is_promocode_entry_available(512) == 1, "Promocode allowed on domain with and without www");
# ??????????????????, ?????? ???????? ???????? ???????????????????? ???? ????????????, ???? ?? ???????????? ??????????????????, ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(513) == 0, "No promocode on used domain with encoding");
# ??????????????????, ?????? ???????? ???????? ?????????????? ???????????????????? ???? ????????????, ???? ?? ???????????? ??????????????????, ???????????????? ?????????????? ??????????
ok(is_promocode_entry_available(514) == 1, "Promocode allowed domain in encoding with zero stat");

# ??????????????????, ?????? ???????? ???????? ???????????????????? ???? ?????????? ?????? www, ???? ???????????????? ?????????????? ????????????
ok(is_promocode_entry_available(515) == 0, "No promocode on used (without www) domain");

1;
