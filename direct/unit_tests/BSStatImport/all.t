#!/usr/bin/perl

use warnings;
use strict;

use CGI;
use Test::Deep;
use Test::Exception;
use Test::MockObject::Extends;
use Test::More;

use Yandex::HashUtils;

sub c { return join("\t", @_); }

my @test_data = ({
    name => 'correct named_type answer',
    type => 'named_type',
    content => [
        0,
        c('#field_uint', 'field_datetime'),
        c(12345, '20121111101010'),
        '#End',
    ],
    check_data_rows_count => 1,
    check_invalid_rows_count => 0,
    check_is_deferred_serialization => 0,
    check_named_response => [{
        field_uint => 12345,
        field_datetime => '2012-11-11 10:10:10',
    }],
    check_invalid_data => [],
}, {
    name => 'correct named_type answer (with extra columns)',
    type => 'named_type',
    content => [
        0,
        c('#field_uint', 'field_datetime', 'extra_column'),
        c(12345, '20121111101010', 42),
        '#End',
    ],
    check_data_rows_count => 1,
    check_invalid_rows_count => 0,
    check_is_deferred_serialization => 0,
    check_named_response => [{
        field_uint => 12345,
        field_datetime => '2012-11-11 10:10:10',
        extra_column => '42',
    }],
    check_invalid_data => [],
}, {
    name => 'incorrect named_type answer (text value for uint field)',
    type => 'named_type',
    content => [
        0,
        c('#field_uint', 'field_datetime'),
        c('aaa', '20121111101010'),
        '#End',
    ],
    check_get_error => qr/Error invalid uint in field field_uint/,
}, {
    name => 'named_type answer with separate_invalid_rows',
    type => 'named_type',
    separate_invalid_rows => 1,
    content => [
        0,
        c('#field_uint', 'field_datetime'),
        c(12345, '20121111101010'),
        c('aaa', '20121111101010'),
        c(678, '20150102030405'),
        '#End',
    ],
    check_data_rows_count => 2,
    check_invalid_rows_count => 1,
    check_is_deferred_serialization => 0,
    check_invalid_data => [
        {error => 'Error invalid uint in field field_uint', row => {field_uint => 'aaa', field_datetime => '20121111101010'}},
    ],
    check_named_response => [
        { field_uint => 12345, field_datetime => '2012-11-11 10:10:10' },
        { field_uint => 678, field_datetime => '2015-01-02 03:04:05' },
    ],
}, {
    name => 'incorrect named_type answer (bad line with field names)',
    type => 'named_type',
    content => [
        0,
        c('field_uint', 'field_datetime'), #missed #
        c('aaa', '20121111101010'),
        '#End',
    ],
    check_get_error => qr/Invalid line with field names/,
},  {
    name => 'incorrect named_type answer (bad line with field names - spaces instead of tab)',
    type => 'named_type',
    content => [
        0,
        '#field_uint    field_datetime',
        c('aaa', '20121111101010'),
        '#End',
    ],
    check_get_error => qr/Invalid line with field names/,
}, {
    name => 'incorrect named_type answer (no line with field names)',
    type => 'named_type',
    content => [
        0,
        '#End',
    ],
    check_get_error => qr/No line with field names/,
}, {
    name => 'incorrect named_type answer (empty response)',
    type => 'named_type',
    content => [],
    check_get_error => qr/Empty response/,
}, {
    name => 'incorrect named_type answer (absent value for datetime field)',
    type => 'named_type',
    content => [
        0,
        c('#field_uint', 'field_datetime'),
        c(12345, ''),
        '#End',
    ],
    check_get_error => qr/Error invalid datetime/,
}, {
    name => q!incorrect named_type answer (field doesn't exists)!,
    type => 'named_type',
    content => [
        0,
        c('#field_uint'),
        c(12345),
        '#End',
    ],
    check_get_error => qr/field_datetime are required but was not found/,
}, {
    name => 'correct named_type answer (with keyval_list_sum_by_key and deferred serialization)',
    type => 'named_type_2',
    content => [
        0,
        '#GoalNumsByGoalIDs',
        '101:1,102:2',
        '101:1,102:2,103:3,101:4,101:5,102:2',
        '101:2,102:3,103:0,200:4,200:3,200:2,200:1',
        '300:10,301:11',
        '400:2,400:1',
        '#End',
    ],
    check_data_rows_count => 5,
    check_invalid_rows_count => 0,
    check_is_deferred_serialization => 1,
    check_named_response => [
        { GoalNumsByGoalIDs => bag([101, 1], [102, 2]) },
        { GoalNumsByGoalIDs => bag([101, 10], [102, 4], [103, 3]) },
        { GoalNumsByGoalIDs => bag([101, 2], [102, 3], [103, 0], [200, 10]) },
        { GoalNumsByGoalIDs => bag([300, 10], [301, 11]) },
        { GoalNumsByGoalIDs => [ [400, 3] ] },
    ],
    check_invalid_data => [],
}, {
    name => 'correct named_type answer (with keyval_list_sum_by_key and deferred serialization and separate_invalid_rows)',
    type => 'named_type_2',
    separate_invalid_rows => 1,
    content => [
        0,
        '#GoalNumsByGoalIDs',
        '101:1,102:2',
        '101:1,102:2,103:3,101:4,101:5,102:2',
        '500:a',
        '101:2,102:3,103:0,200:4,200:3,200:2,200:1',
        '300:10,301:11',
        '400:2,400:1',
        '8:8,',
        '#End',
    ],
    check_data_rows_count => 5,
    check_invalid_rows_count => 2,
    check_is_deferred_serialization => 1,
    check_named_response => [
        { GoalNumsByGoalIDs => bag([101, 1], [102, 2]) },
        { GoalNumsByGoalIDs => bag([101, 10], [102, 4], [103, 3]) },
        { GoalNumsByGoalIDs => bag([101, 2], [102, 3], [103, 0], [200, 10]) },
        { GoalNumsByGoalIDs => bag([300, 10], [301, 11]) },
        { GoalNumsByGoalIDs => [ [400, 3] ] },
    ],
    check_invalid_data => [
        {error => 'Error unmatched "^(?:|(?:\d+:\d+,)*\d+:\d+)$" value 500:a in field GoalNumsByGoalIDs', row => {GoalNumsByGoalIDs => '500:a'}},
        {error => 'Error unmatched "^(?:|(?:\d+:\d+,)*\d+:\d+)$" value 8:8, in field GoalNumsByGoalIDs', row => {GoalNumsByGoalIDs => '8:8,'}},
    ],
}, {
    name => 'correct real_incremental answer (with value_modifiers, named_type, info_line)',
    type => 'real_incremental',
    content => [
        0,
        c('#Clicks', 'Shows', 'MaxHostTime',   'OrderID', 'TargetType', 'CostCur',  'UpdateTime',     'Cost'),
        c(  0,        8,      '20131030222603', 1000976,   2,           '12300000', '20131030000000', '4100000'),
        c(  14,       117,    '20131030222603', 1000976,   0,           '45600000', '20131030000000', '1520000'),
        ('1' x 101), #info line
        '#End',
    ],
    check_data_rows_count => 2,
    check_invalid_rows_count => 0,
    check_named_response => [{
        Clicks => 0,
        Shows => 8,
        MaxHostTime => '2013-10-30 22:26:03',
        OrderID => 1000976,
        TargetType => 2,
        CostCur => num(12.3),
        UpdateTime => '2013-10-30',
        Cost => num(4.1),
    }, {
        Clicks => 14,
        Shows => 117,
        MaxHostTime => '2013-10-30 22:26:03',
        OrderID => 1000976,
        TargetType => 0,
        CostCur => num(45.6),
        UpdateTime => '2013-10-30',
        Cost => num(1.52),
    }],
    check_invalid_data => [],
    check_is_deferred_serialization => 0,
}, {
    name => 'correct real_incremental answer (with value_modifiers, named_type, info_line, zero_negative)',
    type => 'real_incremental',
    content => [
        0,
        c('#Clicks', 'Shows', 'MaxHostTime',   'OrderID', 'TargetType', 'CostCur',  'UpdateTime',     'Cost'),
        c(  -666,     -666,   '20131031232704', 1000976,   2,           '-321000',  '20131031000000', '-10700'),
        c(  -3,       117,    '20131030222603', 1000976,   0,           '45600000', '20131030000000', '1520000'),
        ('2' x 101), #info line
        '#End',
    ],
    check_data_rows_count => 2,
    check_invalid_rows_count => 0,
    check_named_response => [{
        Clicks => 0,
        Shows => 0,
        MaxHostTime => '2013-10-31 23:27:04',
        OrderID => 1000976,
        TargetType => 2,
        CostCur => num(0),
        UpdateTime => '2013-10-31',
        Cost => num(0),
    }, {
        Clicks => 0,
        Shows => 117,
        MaxHostTime => '2013-10-30 22:26:03',
        OrderID => 1000976,
        TargetType => 0,
        CostCur => num(45.6),
        UpdateTime => '2013-10-30',
        Cost => num(1.52),
    }],
    check_invalid_data => [],
    check_is_deferred_serialization => 0,
}, {
    name => 'incorrect real_incremental answer (no last line)',
    type => 'real_incremental',
    content => [
        0,
        c('#Clicks', 'Shows', 'MaxHostTime',   'OrderID', 'TargetType', 'CostCur',  'UpdateTime',     'Cost'),
        c(  0,        8,      '20131030222603', 1000976,   2,           '12300000', '20131030000000', '78900000'),
        ('3' x 101), #info line
    ],
    check_get_error => qr/Incorrect last but one line/,
}, {
    name => 'incorrect real_incremental answer (bad info line)',
    type => 'real_incremental',
    content => [
        0,
        c('#Clicks', 'Shows', 'MaxHostTime',   'OrderID', 'TargetType', 'CostCur',  'UpdateTime',     'Cost'),
        c(  0,        8,      '20131030222603', 1000976,   2,           '12300000', '20131030000000', '78900000'),
        ('4' x 99), #info line
        '#End',
    ],
    check_get_error => qr/Incorrect info line/,
});

my $mock_LWP_UserAgent_get = sub {
    my $self = shift;
    my ($url, $paramstr) = (shift =~ m/(.*?)\?(.*)/);

    my $request = CGI->new($paramstr)->Vars;
    $request->{test_case} //= 0;
    my $data = $test_data[$request->{test_case}] // {};

    my $response = Test::MockObject::Extends->new('HTTP::Response');
    $response->mock('is_success', sub { return defined($data->{content}) });
    $response->mock('status_line', sub { return defined($data->{content}) ? '200 OK' : q/418 I'm a teapot/ });
    $response->mock('decoded_content', sub { return join("\n", @{ $data->{content} }) });
    return $response;
};

{
    # mock'и для методов скачивания данных
    no warnings 'redefine';
    *LWP::UserAgent::get = $mock_LWP_UserAgent_get;
}

BEGIN {
    # Подменяем адрес, чтобы тест не пошел за реальной статистикой
    $Settings::BS_EXPORT_PROXY = $Settings::BS_EXPORT_PROXY_READONLY = 'http://127.0.0.127:7999/';

    use_ok( 'BSStatImport' );
};

local $BSStatImport::BS_STAT_TYPES = {
    named_type => {
        url => 'http://127.0.0.200/test_stat/named_type.cgi',
        required_fields => [qw/field_uint field_datetime/],
        fields_format => {field_uint => 'uint', field_datetime => 'datetime'},
        value_modifiers => {field_datetime => 'datetime'},
    },
    named_type_2 => {
        url => 'http://127.0.0.200/test_stat/named_type_2.cgi',
        value_modifiers => {
            GoalNumsByGoalIDs => 'keyval_list_sum_by_key',
        },
        fields_format => {
            GoalNumsByGoalIDs => {type => 're_match', re => '^(?:|(?:\d+:\d+,)*\d+:\d+)$'},
        },
        deferred_serialization_lines_border_count => 2,
    },
    real_incremental => {
        url => 'http://127.0.0.200/test_stat/real_incremental.cgi',
        params => {format => 1},
        incremental => 1,
        info_line => qr/^\S{100,}$/,
        required_fields => [qw/OrderID        UpdateTime      TargetType      Shows   Clicks  Cost    MaxHostTime     CostCur/],
        fields_format => {
            OrderID => 'uint',
            UpdateTime => 'datetime',
            TargetType => {type => 'uint', max => 9},
            Shows => {type => 'uint', min => undef},
            Clicks => {type => 'uint', min => undef},
            Cost => {type => 'uint', min => undef},
            MaxHostTime => 'datetime',
            CostCur => {type => 'uint', min => undef},
        },
        value_modifiers => {
            UpdateTime => 'date',
            Shows => 'zero_negative',
            Clicks => 'zero_negative',
            Cost => ['zero_negative', 'sum'],
            MaxHostTime => 'datetime',
            CostCur => ['zero_negative', 'sum'],
        },
    },
};

can_ok('BSStatImport', qw(new get get_rows_count get_data_rows_count get_url get_info get_response_bs_status_code
                          as_named_arrayref get_named_chunks_iterator get_named_rows_iterator
                          get_invalid_data get_invalid_rows_count
                          ));

throws_ok { BSStatImport->new( param => 'foo', params => {foo => 'bar'} ) } qr/Can't create BSStatImport object without type of query./,
    "Can't create BSStatImport object without type of query.";

throws_ok { BSStatImport->new( type => 'foo', params => {foo => 'bar'} ) } qr/Can't create BSStatImport object. Wrong type of statistics/,
    "Wrong type of statistics.";

my $error;
lives_ok { $error = BSStatImport->new(type => 'named_type')->_check_named_row({field_uint => 12345}) } "_check_named_row (field doesn't exists) - success";
like($error, qr/field_datetime does not exists/, "_check_named_row (field doesn't exists) - checking error text");
$error = undef;

my $tc = 0;
for my $test (@test_data) {
    my ($stat_obj, $error_msg);

    $stat_obj = BSStatImport->new(
        type => $test->{type},
        params => hash_merge($test->{params}, {test_case => $tc}),
        ($test->{separate_invalid_rows} ? (separate_invalid_rows => 1) : ()),
    );

    lives_ok { $error_msg = $stat_obj->get() } "get() for $test->{name} - success";

    if ($test->{check_get_error}) {
        like($error_msg, $test->{check_get_error}, "get() for $test->{name} - check error text");
    } else {
        is($error_msg, 0, "get() for $test->{name} - returned status code is 0");

        my $rows_count;
        lives_ok(sub { $rows_count = $stat_obj->get_rows_count() },
                 "get_rows_count() for $test->{name} - success",
                 );
        is($rows_count,
           scalar(@{ $test->{content} }),
           "get_rows_count() for $test->{name} - compare result with source data",
           );

        if (defined $test->{check_data_rows_count}) {
            my $data_rows_count;
            lives_ok(sub { $data_rows_count = $stat_obj->get_data_rows_count() },
                     "get_data_rows_count() for $test->{name} - success",
                     );
            is($data_rows_count,
               $test->{check_data_rows_count},
               "get_data_rows_count() for $test->{name} - compare result with test data",
               );
        }

        if (defined $test->{check_invalid_rows_count}) {
            my $invalid_rows_count;
            lives_ok(sub { $invalid_rows_count = $stat_obj->get_invalid_rows_count() },
                     "get_invalid_rows_count() for $test->{name} - success",
                     );
            is($invalid_rows_count,
               $test->{check_invalid_rows_count},
               "get_invalid_rows_count() for $test->{name} - compare result with test data",
               );
        }

        if (defined $test->{check_is_deferred_serialization}) {
            my $is_deferred;
            lives_ok(sub { $is_deferred = $stat_obj->_is_deferred_serialization() },
                     "_is_deferred_serialization() for $test->{name} - success",
                     );
            if ($test->{check_is_deferred_serialization}) {
                ok($is_deferred, "_is_deferred_serialization() for $test->{name} - true");
                ok(!exists $stat_obj->{named_response}, "_is_deferred_serialization() for $test->{name} - object hasn't named_response");
                ok(exists $stat_obj->{response}, "_is_deferred_serialization() for $test->{name} - object has response");
            } else {
                ok(!$is_deferred, "_is_deferred_serialization() for $test->{name} - false");
            }
        }

        if ($test->{check_invalid_data}) {
            my $invalid_data;
            lives_ok(sub { $invalid_data = $stat_obj->get_invalid_data() },
                     "get_invalid_data() for $test->{name} - success",
                     );
            cmp_deeply($invalid_data,
                       $test->{check_invalid_data},
                       "get_invalid_data() for $test->{name} - check data",
                       );
        }

        if ($test->{check_named_response}) {
            my $stat_data;
            lives_ok(sub { $stat_data = $stat_obj->as_named_arrayref() },
                     "as_named_arrayref() for $test->{name} - success",
                     );
            cmp_deeply($stat_data,
                       $test->{check_named_response},
                       "as_named_arrayref() for $test->{name} - check data",
                       );

            # Также сверяем с данными, полученными через итераторы
            my ($iterator, @stat_data);
            lives_ok(sub { $iterator = $stat_obj->get_named_rows_iterator()},
                     "get_named_rows_iterator() for $test->{name} - success",
                     );
            lives_ok(sub { while (my $row = $iterator->()) { push @stat_data, $row } },
                     "call named_rows_iterator for $test->{name} - success",
                     );
            cmp_deeply(\@stat_data,
                       $test->{check_named_response},
                       "get_named_rows_iterator() for $test->{name} - check data",
                       );
            my $data_rows_count = $stat_obj->get_data_rows_count();
            for (my $chunk_size = 1; $chunk_size <= $data_rows_count + 1; $chunk_size++) {
                $iterator = undef;
                @stat_data = ();
                my @chunks;

                my $quotient = int($data_rows_count / $chunk_size);
                my $remainder = $data_rows_count % $chunk_size;
                my $expected_chunks_count = ($remainder || !$quotient) ? ($quotient + 1) : $quotient;
                my $last_chunk_size = $quotient ? ($remainder || $chunk_size) : $data_rows_count;

                lives_ok(sub { $iterator = $stat_obj->get_named_chunks_iterator($chunk_size)},
                         "get_named_chunks_iterator() for $test->{name} (chunk_size=$chunk_size) - success",
                         );
                lives_ok(sub { while (my $chunk = $iterator->()) { push @chunks, $chunk } },
                     "call named_chunks_iterator for $test->{name} (chunk_size=$chunk_size) - success",
                     );
                is (@chunks,
                    $expected_chunks_count,
                    "check chunks count for $test->{name} (chunk_size=$chunk_size)"
                    );

                for (my $i = 0; $i <= $#chunks; $i++) {
                    my $chunk = $chunks[$i];
                    push @stat_data, @$chunk;
                    my $test_name = "check data chunk size for $test->{name} (chunk_size=$chunk_size) - chunk $i";
                    if ($i == $#chunks) {
                        is(@$chunk, $last_chunk_size, $test_name);
                    } else {
                        is (@$chunk, $chunk_size, $test_name);
                    }
                }
                cmp_deeply(\@stat_data,
                           $test->{check_named_response},
                           "get_named_chunks_iterator() for $test->{name} - check data",
                           );
            }
        }
    }

    $tc++;
};

done_testing();
