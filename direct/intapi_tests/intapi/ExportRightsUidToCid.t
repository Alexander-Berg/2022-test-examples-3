#!/usr/bin/perl

use Direct::Modern;

use Yandex::HashUtils;

use Test::Intapi;
use Test::Deep;

use List::MoreUtils qw(zip);

my $url = base_url() . '/ExportRightsUidToCid';

=head2 _parse_tsv_response

    Разбирает ответ ручки в массив хешей

    $result = _parse_tsv_response($response->decoded_content);
    $result => [
        { $field_name1 => $value1, $field_name2 => $value2, ... },
        ...
    ]

=cut

sub _parse_tsv_response {
    my ($content) = @_;

    my @lines = split /\n/, $content;

    my $last_line = pop(@lines) // '';
    if ($last_line ne '#End') {
        die "invalid last line: $last_line";
    }

    my $header_line = shift(@lines) // '';
    unless ($header_line =~ s/^#//) {
        die "invalid header line: $header_line";
    }
    my @fields = split /\t/, $header_line;

    my @result;
    for my $line (@lines) {
        my @values = split /\t/, $line, scalar(@fields);
        my %row = zip @fields, @values;
        push @result, \%row;
    }

    return \@result;
}

=head2 _check_result

    Сравнивает результат, полученный из ручки, с эталонным
    На вход ожидает параметры:
        $data -- данные для теста вида: {
                     params => { cid => 263 },
                     expected => [ { uid => 666, cid => 263 } ],
                 }
        $response
        $test_name

=cut

sub _check_result {
    my ($data, $response, $test_name) = @_;

    my $params = $data->{params};
    my $expected = $data->{expected};

    ok($response->is_success, "$test_name: successfull response");
    my $result;
    lives_ok { $result = _parse_tsv_response($response->decoded_content) } "$test_name: parse response";
    cmp_bag($result, $expected, "$test_name: response got should match expected");
}

=head2 _preprocess_data

    Обработчик данных, вытаскивающий из $data параметры запроса

=cut

sub _preprocess_data {
    my ($data) = @_;

   return $data->{params};
}

my %default_test_params = (
    read_only => 1,
    url => $url,
    method => 'GET',
    preprocess => \&_preprocess_data,
    check_num => 3,
    check => \&_check_result,
);

my @tests = (
    {
        name => 'manager campaign',
        data => [
            {
                params => { cid => 263 },
                expected => [
                    { uid => 128411764, cid => 263 },
                ],
            },
        ],
        %default_test_params,
    },
    {
        name => 'agency campaign',
        data => [
            {
                params => { cid => 160084 },
                expected => [
                    { uid => 12793190, cid => 160084 },
                    { uid => 142382651, cid => 160084 },
                ],
            },
        ],
        %default_test_params,
    },
    {
        name => 'self client campaign',
        data => [
            {
                params => { cid => 103 },
                expected => [
                ],
            },
        ],
        %default_test_params,
    },
);

run_tests(\@tests);
