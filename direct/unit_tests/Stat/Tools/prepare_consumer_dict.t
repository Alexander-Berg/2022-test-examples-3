#!/usr/bin/perl

=pod

    $Id$

=cut

use strict;
use warnings;
use utf8;

use Test::Deep;
use Test::More;
use Test::Exception;

use Yandex::Test::UTF8Builder;

use Settings;

use_ok('Stat::Tools');

my @test_dataset = (
    {
        src => {desktop => [1],
                mobile  => [2],
                tablet  => [3],},
        result => { values => {
                        desktop => {bs => [1], orig_names => [qw/desktop/]},
                        mobile  => {bs => [2], orig_names => [qw/mobile/]},
                        tablet  => {bs => [3], orig_names => [qw/tablet/]}
                    },
                    need_reverse_translation => 0
                  },
        title => 'simplest dict format, default consumer',
    },
    {
        src => {desktop => [1],
                mobile  => [2],
                tablet  => [3],},
        consumer => 'api',
        result => { values => {
                        desktop => {bs => [1], orig_names => [qw/desktop/]},
                        mobile  => {bs => [2], orig_names => [qw/mobile/]},
                        tablet  => {bs => [3], orig_names => [qw/tablet/]}
                    },
                    need_reverse_translation => 0
                  },
         title => 'simplest dict format, api consumer',
    },
    {
        src => {undefined => {bs => -1, api => 'OS_TYPE_UNKNOWN', web_ui => {ord => 1}},
                other     => {bs => 0, api => 'OS_TYPE_UNKNOWN', web_ui => {ord => 4}},
                android   => {bs => 2, api => 'ANDROID', web_ui => {ord => 2}},
                ios       => {bs => 3, api => 'IOS', web_ui => {ord => 3}} },
        result => { values => {
                        undefined => {bs => [-1], orig_names => [qw/undefined/]},
                        other     => {bs => [0], orig_names => [qw/other/]},
                        android   => {bs => [2], orig_names => [qw/android/]},
                        ios       => {bs => [3], orig_names => [qw/ios/]}, 
                    },
                    need_reverse_translation => 0
                  },
        title => 'alt names for api, all ord-s for web_ui, default consumer',
    },
    {
        src => {undefined => {bs => -1, api => 'OS_TYPE_UNKNOWN', web_ui => {ord => 1}},
                other     => {bs => 0, api => 'OS_TYPE_UNKNOWN', web_ui => {ord => 4}},
                android   => {bs => 2, api => 'ANDROID', web_ui => {ord => 2}},
                ios       => {bs => 3, api => 'IOS', web_ui => {ord => 3}} },
        consumer => 'web_ui',
        result => { values => {
                        '1undefined' => {bs => [-1], name => 'undefined', orig_names => [qw/undefined/]},
                        '4other'     => {bs => [0], name => 'other', orig_names => [qw/other/]},
                        '2android'   => {bs => [2], name => 'android', orig_names => [qw/android/]},
                        '3ios'       => {bs => [3], name => 'ios', orig_names => [qw/ios/]},
                    },
                    need_reverse_translation => 1
                  },
        title => 'alt names for api, all ord-s for web_ui, web_ui consumer',
    },
    {
        src => {undefined => {bs => -1, api => 'OS_TYPE_UNKNOWN', web_ui => {ord => 1}},
                other     => {bs => 0, api => 'OS_TYPE_UNKNOWN', web_ui => {ord => 4}},
                android   => {bs => 2, api => 'ANDROID', web_ui => {ord => 2}},
                ios       => {bs => 3, api => 'IOS', web_ui => {ord => 3}} },
        consumer => 'api',
        result => { values => {
                        OS_TYPE_UNKNOWN => {bs => [-1,0], orig_names => [qw/other undefined/]},
                        ANDROID   => {bs => [2], orig_names => [qw/android/]},
                        IOS       => {bs => [3], orig_names => [qw/ios/]},
                    },
                    need_reverse_translation => 0
                  },
        title => 'alt names for api, all ord-s for web_ui, api consumer',
    },
    {
        src => {undefined => {bs => -1, web_ui => {name => 'none', ord => 1}},
                other     => {bs => 0, web_ui => {ord => 4}},
                android   => {bs => 2, web_ui => {ord => 2}},
                ios       => {bs => 3, web_ui => {ord => 3}} },
        consumer => 'web_ui',
        result => { values => {
                        '1none' => {bs => [-1], name => 'none', orig_names => [qw/undefined/]},
                        '4other'     => {bs => [0], name => 'other', orig_names => [qw/other/]},
                        '2android'   => {bs => [2], name => 'android', orig_names => [qw/android/]},
                        '3ios'       => {bs => [3], name => 'ios', orig_names => [qw/ios/]},
                    },
                    need_reverse_translation => 1
                  },
        title => 'all ord-s and some alt names for web_ui, web_ui consumer',
    },
    {
        src => {undefined => {bs => -1, web_ui => {name => 'none'}},
                other     => {bs => 0},
                android   => {bs => 2},
                ios       => {bs => 3} },
        consumer => 'web_ui',
        result => { values => {
                        none      => {bs => [-1], orig_names => [qw/undefined/]},
                        other     => {bs => [0], orig_names => [qw/other/]},
                        android   => {bs => [2], orig_names => [qw/android/]},
                        ios       => {bs => [3], orig_names => [qw/ios/]},
                    },
                    need_reverse_translation => 0
                  },
        title => 'no ord-s and some alt names for web_ui, web_ui consumer',
    },

    {
        src => {undefined => {bs => -1, web_ui => {name => 'none', ord => 1}},
                other     => {bs => 0, web_ui => {ord => 4}},
                android   => {bs => 2, web_ui => {ord => 2}},
                ios       => {bs => 3, web_ui => {ord => 3}} },
        consumer => 'web_ui',
        result => { values => {
                        1 => {bs => [-1], name => 'none', orig_names => [qw/undefined/]},
                        4 => {bs => [0], name => 'other', orig_names => [qw/other/]},
                        2 => {bs => [2], name => 'android', orig_names => [qw/android/]},
                        3 => {bs => [3], name => 'ios', orig_names => [qw/ios/]},
                    },
                    need_reverse_translation => 1
                  },
        use_text_keys => 0,
        title => 'all ord-s and some alt names for web_ui, web_ui consumer, use_text_keys=0',
    },
    {
        src => {undefined => {bs => -1, web_ui => {name => 'none'}},
                other     => {bs => 0},
                android   => {bs => 2},
                ios       => {bs => 3} },
        consumer => 'web_ui',
        result => { values => {
                        3 => {bs => [-1], name => 'none', orig_names => [qw/undefined/]},
                        4 => {bs => [0], name => 'other', orig_names => [qw/other/]},
                        1 => {bs => [2], name => 'android', orig_names => [qw/android/]},
                        2 => {bs => [3], name => 'ios', orig_names => [qw/ios/]},
                    },
                    need_reverse_translation => 1
                  },
        use_text_keys => 0,
        title => 'no ord-s and some alt names for web_ui, web_ui consumer, use_text_keys=0,',
    },

    # сценарии с критичными ошибками
    {
        src => {undefined => {bs => -1},
                other     => {bs => 0, web_ui => {ord => 1}},
                android   => {bs => 2},
                ios       => {bs => 3} },
        consumer => 'web_ui',
        result_status => 'die',
        title => 'some ord-s for web_ui, web_ui consumer',
    },
    {
        src => {undefined => {bs => -1, api => {ord => 1, name => 'OS_TYPE_UNKNOWN'}},
                other     => {bs => 0, api => {ord => 4, name => 'OS_TYPE_UNKNOWN'}},
                android   => {bs => 2, api => {ord => 2, name => 'ANDROID'}},
                ios       => {bs => 3, api => {ord => 3, name => 'IOS'}} },
        consumer => 'api',
        result_status => 'die',
        title => 'diff ord-s for same alt names for api, api consumer',
    },
    {
        src => {undefined => {bs => -1, web_ui => {ord => -1}},
                other     => {bs => 0, web_ui => {ord => 0}},
                android   => {bs => 2, web_ui => {ord => 1}},
                ios       => {bs => 3, web_ui => {ord => 2}} },
        consumer => 'web_ui',
        result_status => 'die',
        title => 'all ord-s, but some negative for web_ui, web_ui consumer',
    },

);

for my $t (@test_dataset) {
    if (($t->{result_status} // 'ok') eq 'die') {
        dies_ok(sub { Stat::Tools::_prepare_consumer_dict($t->{src}, $t->{consumer}, use_text_keys => $t->{use_text_keys} // 1)}, $t->{title});
    } else {
        cmp_deeply(Stat::Tools::_prepare_consumer_dict($t->{src}, $t->{consumer}, use_text_keys => $t->{use_text_keys} // 1), $t->{result}, $t->{title});
    }
}

done_testing;
