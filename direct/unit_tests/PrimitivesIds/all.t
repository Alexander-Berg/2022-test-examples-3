#!/usr/bin/perl

use strict;
use warnings;

use Test::Exception;
use Test::More;
use Test::Deep;
use List::MoreUtils qw/natatime/;
use Data::Dumper;
use JSON;

use Yandex::DBUnitTest qw/:all/;

use my_inc '../..';
use Settings;

use utf8;

my $dataset = {
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1, },
            { ClientID => 2, shard => 2, },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 6, ClientID => 1, },
            { uid => 7, ClientID => 2, },
            { uid => 8, ClientID => 1, },
        ],
    },
    shard_login => {
        original_db => PPCDICT,
        rows => [
            { login => 'l-6', uid => 6, },
            { login => 'l-7', uid => 7, },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 666, ClientID => 1, },
            { cid => 777, ClientID => 2, },
        ],
    },
    shard_order_id => {
        original_db => PPCDICT,
        rows => [
            { OrderID => 1666, ClientID => 1, },
            { OrderID => 1777, ClientID => 2, },
        ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [
            { bid => 81, ClientID => 1, },
            { bid => 82, ClientID => 1, },
            { bid => 83, ClientID => 2, },
        ],
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [
            { vcard_id => 101, ClientID => 1, },
            { vcard_id => 102, ClientID => 1, },
            { vcard_id => 103, ClientID => 2, },
        ],
    },
    shard_inc_pid => {
        original_db => PPCDICT,
        rows => [
            { pid => 71, ClientID => 1, },
            { pid => 72, ClientID => 1, },
            { pid => 73, ClientID => 2, },

            { pid => 41, ClientID => 1, },
            { pid => 42, ClientID => 1, },
            { pid => 43, ClientID => 2, },
        ],
    },
    shard_inc_mbid => {
        original_db => PPCDICT,
        rows => [
            { mbid => 61, ClientID => 1, },
            { mbid => 62, ClientID => 1, },
            { mbid => 63, ClientID => 2, },
        ],
    },
    shard_inc_mediaplan_bid => {
        original_db => PPCDICT,
        rows => [
            { mediaplan_bid => 91, ClientID => 1, },
            { mediaplan_bid => 92, ClientID => 1, },
            { mediaplan_bid => 93, ClientID => 2, },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 112, ClientID => 1, },
            { ret_cond_id => 113, ClientID => 2, },
            { ret_cond_id => 114, ClientID => 2, },
        ],
    },


    clients => {
        original_db => PPC(shard => 'all'),
        rows => {1 => [ { ClientID => 1, chief_uid => 6, role => 'client', subrole => undef, agency_client_id => 99, agency_uid => 999, } ],
                 2 => [ { ClientID => 2, chief_uid => 7, role => 'client', subrole => undef, agency_client_id => undef, agency_uid => undef, } ],
                },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {1 => [ 
                     { uid => 6, rep_type => 'chief', login => 'l-6', ClientID => 1 }, 
                     { uid => 8, rep_type => 'main', login => 'l-8', ClientID => 1 }, 
                     ],
                 2 => [ { uid => 7, rep_type => 'chief', login => 'l-7', ClientID => 2 } ]
                },
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {1 => [ { cid => 666, uid => 6, ClientID => 1, OrderID => 1666 }, ],
                 2 => [ { cid => 777, uid => 7, ClientID => 2, OrderID => 1777 },
                        { cid => 778, uid => 7, ClientID => 2, OrderID => 0 }
                     ]
                },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [ { pid => 71, cid => 666 },
                         { pid => 72, cid => 666 } ],
                  2 => [ { pid => 73, cid => 777 } ]
                },
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [ { bid => 81, pid => 71, BannerID => 111, cid => 666, vcard_id => 101 },
                         { bid => 82, pid => 71, BannerID => 0, cid => 666 } ],
                  2 => [ { bid => 83, pid => 73, BannerID => 113, cid => 777, vcard_id => 103 } ]
                },
    },
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [ { cid => 666, vcard_id => 101 },
                         { cid => 666, vcard_id => 102 } ],
                  2 => [ { cid => 777, vcard_id => 103 } ]
                },
    },                      
    bids => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [ { id => 51, cid => 666 },
                         { id => 52, cid => 666 } ],
                  2 => [ { id => 53, cid => 777 } ]
                },
    },
    mediaplan_banners => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [{ mbid => 91, cid => 666 },
                        { mbid => 92, cid => 666 } ],
                  2 => [{ mbid => 93, cid => 777 }],
                },
    },
    media_groups => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [ { mgid => 41, cid => 666 },
                         { mgid => 42, cid => 666 } ],
                  2 => [ { mgid => 43, cid => 777 } ]
                },
    },
    media_banners => {
        original_db => PPC(shard => 'all'),
        rows => { 1 => [ { mbid => 61, mgid => 41 },
                         { mbid => 62, mgid => 42 } ],
                  2 => [ { mbid => 63, mgid => 43 } ]
                },
    },
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {1 => [ { ret_cond_id => 112, ClientID => 1 } ],
                 2 => [ { ret_cond_id => 113, ClientID => 2 } ],
                 3 => [ { ret_cond_id => 114, ClientID => 2 } ]
                },
    },
};

my %test_cases = (
    # func => [
    #     key => val => result (\undef for exception)
    get_cids => [
        asdf => 32 => \undef,
        asdf => [32] => \undef,

        bid => undef => \undef,
        bid => 24352345 => [],
        bid => 81 => [666],
        bid => [81,82,83] => [666,777],

        pid => [73] => [777],
        media_bid => [61] => [666],
        media_gid => [43] => [777],        
        mediaplan_bid => 93 => [777],
        OrderID => 1666 => [666],
        vcard_id => 103 => [777],
        uid => [7] => [777, 778],
        ClientID => [2] => [777, 778],
    ],

    get_cid => [
        asdf => 32 => \undef,
        asdf => [32] => \undef,
        bid => [81] => \undef,

        bid => 81 => 666,
        pid => 73 => 777,
        media_bid => 61 => 666,
        media_gid => 43 => 777,
        mediaplan_bid => 93 => 777,
        OrderID => 1666 => 666,
    ],

    get_bid2cid => [
        bid => [81,71,83] => {81=>666, 83=>777},
    ],

    get_pid2cid => [
        pid => [71,72,73] => {71=>666, 72=>666, 73=>777},
    ],

    get_cid2orderid => [
        cid => 666 => {666 => 1666},
        cid => [666, 534534] => {666 => 1666},
    ],

    get_orderid => [
        cid => 666 => 1666,
        pid => 71 => 1666,
        bid => 83 => 1777,
    ],

    get_orderids => [
        ClientID => 1 => [1666],
        uid => [7] => [1777],
        cid => [666, 777, 8888] => [1666, 1777],
        pid => 71 => [1666],
        bid => 83 => [1777],
    ],

    get_orderid2cid => [
        OrderID => [1666, 1777, 234, 0] => {1666 => 666, 1777 => 777},
        uid => [7] => {1777 => 777},
        cid => [666,777,778] => {1666 => 666, 1777 => 777},
    ],

    get_pid => [
        bid => 81 => 71,
        bid => 82 => 71,
        bid => 71 => undef,
    ],
    get_pids => [
        bid => 81 => [71],
        bid => [81,82] => [71],
        bid => [81, 83, 83, 84 ] => [71,73],
        cid => 666 => [71, 72],
        cid => [666,777] => [71, 72, 73],
    ],

    get_bids => [
        cid => 666 => [81,82],
        pid => 73 => [83],
    ],

    get_bid2pid => [
        cid => 666 => {81 => 71, 82 => 71},
        cid => 3245 => {},
        cid => [666,777] => {81 => 71, 82 => 71, 83 => 73},
        pid => 71 => {81 => 71, 82 => 71},
        pid => [71,72,73] => {81 => 71, 82 => 71, 83 => 73},
        bid => [81] => {81 => 71},
        bid => [81,83] => {81 => 71, 83 => 73},
    ],

    get_pid2bids => [
        pid => 71 => {71 => [81,82]},
        pid => [71,72,73] => {71 => [81,82], 73 => [83]},
        cid => 666 => {71 => [81,82]},
        cid => [666, 777] => {71 => [81,82], 73 => [83]},
    ],

    get_bannerids => [
        bid => [81,888] => [111],
    ],

    get_clientid => [
        uid => 6 => 1,
        login => 'L.6' => 1,
        cid => 777 => 2, 
        OrderID => 1777 => 2,
        pid => 73 => 2,
        bid => 82 => 1,
    ],

    get_clientids => [
        uid => 6 => [1],
        login => ['l-7'] => [2],
        cid => 777 => [2], 
        OrderID => 1777 => [2], 
        pid => 73 => [2],
        bid => 82 => [1],
        ret_cond_id => 112 => [1],
    ],

    get_key2clientid => [
        uid => 6 => {6 => 1},
        login => ['l-7'] => {'l-7' => 2},
        cid => 777 => {777 => 2}, 
        OrderID => 1777 => {1777 => 2}, 
        pid => 73 => {73 => 2},
        bid => 82 => {82 => 1},
    ],

    get_uid2clientid => [
        uid => 6 => {6=>1},
        uid => [6,7,9] => {6=>1, 7=>2},
        ClientID => 1 => {6=>1, 8=>1},
        ClientID => [5,2,7] => {7=>2},
    ],

    get_login2clientid => [
        login => ['l-5', 'L.6', 'l.6', 'l-7'] => {'L.6' => 1, 'l.6' => 1, 'l-7' => 2},
    ],

    get_uid => [
        ClientID => 1 => 6,
        login => 'L.6' => 6,
        cid => 666 => 6,
        bid => 81 => 6,
        vcard_id => 103 => 7,
    ],
    get_owner => [
        cid => 666 => 6,
        bid => 81 => 6,
        vcard_id => 103 => 7,
    ],
    get_uids => [
        ClientID => [1,2,3] => [6,7,8],
        login => ['l1', 'L.6', 'l-7'] => [6,7],
        cid => 666 => [6],
        OrderID => 1777 => [7],
        bid => [81, 82] => [6],
        vcard_id => [103, 108] => [7],
    ],
    get_cid2uid => [
        cid => 666 => {666=>6},
        cid => [666,777,888] => {666=>6, 777=>7},
    ],
    get_login2uid => [
        login => ['l-5', 'L.6', 'l.6', 'l-7'] => {'L.6' => 6, 'l.6' => 6, 'l-7' => 7},
    ],

    get_cid2clientid => [
        cid => 666 => {666=>1},
        cid => [666,777,888] => {666=>1, 777=>2},
    ],

    get_login => [
        uid => 7 => 'l-7',
    ],
    get_logins => [
        uid => [5,6,7] => ['l-6', 'l-7'],
        ClientID => [1,2] => ['l-6', 'l-7', 'l-8'],
    ],
    get_uid2login => [
        uid => [5,6,7] => {6 => 'l-6', 7 => 'l-7'},
    ],
    get_clientid2logins => [
        ClientID => [1,2] => {1=>['l-6', 'l-8'], 2=>['l-7']},
    ],
    get_chief => [
        ClientID => 1 => 6,
        ClientID => 2 => 7,
        ClientID => 3 => undef,
        uid => 6 => 6,
        uid => 7 => 7,
        uid => 8 => 6,
    ],
    get_retcondids => [
        ClientID => [1] => [112],
        ClientID => [1,2] => [112, 113, 114],
    ],

    );

if (@ARGV == 1 && $ARGV[0] eq 'print-tests') {
    # специальный режим работы для Perl::Critic::Policy::Direct::PrimitivesIds::ValidKeys
    # выводим json с информацией о существующих тестах
    my %ret;
    for my $func (sort keys %test_cases) {
        my $it = natatime 3, @{$test_cases{$func}};
        while(my ($key, $val, $res) = $it->()) {
            $ret{$func}{$key} = JSON::true;
        }
    }
    print to_json(\%ret);
    exit 0;
}

use_ok('PrimitivesIds');
init_test_dataset($dataset);
cmp_deeply([keys %test_cases], bag(@PrimitivesIds::EXPORT), 'unit-test existence for all exported functions');
    
for my $func (sort keys %test_cases) {
    my $it = natatime 3, @{$test_cases{$func}};
    while(my ($key, $val, $res) = $it->()) {
        no strict 'refs';
        my $text = "$func($key => ".(Dumper($val) =~ s/;|\$VAR1 = //gr =~ s/\s+/ /gr).")";
        if (ref $res eq 'SCALAR' && !defined $$res) {
            dies_ok { &{"PrimitivesIds::$func"}($key => $val); } "dies $text";
        } else {
            my $ret;
            lives_ok { $ret = &{"PrimitivesIds::$func"}($key => $val); } "lives $text";
            cmp_deeply($ret, ref $res eq 'ARRAY' ? bag(@$res) : $res, "result $text");
        }
    }
}

done_testing;
