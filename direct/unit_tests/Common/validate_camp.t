#!/usr/bin/perl

=pod
=encoding utf8

    $Id:$

=head1 NAME

    validate_camp.t

=head1 DESCRIPTION

    https://testpalm.yandex-team.ru/testcase/directapi-1432

=head1 METHODS

=cut

use Direct::Modern;

use Test::More;

use Storable qw/dclone/;
use POSIX qw/strftime/;

use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/copy_table/;

use Common;
use Settings;

use utf8;
use open ':std' => ':utf8';

copy_table(PPCDICT, 'shard_inc_cid');
copy_table(PPCDICT, 'ppc_properties');

subtest 'fio validation ' => sub {
    my @valid_fio = (
        qq~Русское имя~,
        qq~English name~,
        qq~ пробел в начале~,
        qq~müthişreklamÇĞğÖŞÜ~,
        qq~Горілка~,
        qq~арақ~,
        qq~н~,
        qq~.~,
        qq~123~,
        qq~name\\n~,
        qq~name"~,
        ('a'x255),
        ''
    );

    my @invalid_fio = (
        # https://st.yandex-team.ru/DIRECT-46135
        #[ ' ' => 'Имя клиента пусто \(одни пробелы\)' ],
        #[ 'サムライ' => 'ФИО содержит недопустимые символы' ],
        #[ qq~</Name>~ => 'Имя клиента содержит спецсимволы' ],
        #[ ('a'x256) => 'Превышена допустимая длина имени клиента' ]
    );

    my $campaign = get_campaign();
    foreach my $fio (@valid_fio) {
        $campaign->{fio} = $fio;
        validates_ok("fio $fio passes validation", $campaign);
    }

    foreach my $fio (@invalid_fio) {
        my ($fio, $error, $msg) = @$fio;
        $campaign->{fio} = $fio;
        test_for_error("fio validation fails for $fio", $error, $campaign);
    }
};

subtest 'start_date validation ' => sub {
    my $f = "start_time";
    check_date_field_ok("start_time");

    my $campaign = get_campaign();
    my $date = $campaign->{$f} = strftime("%d-%m-%Y", localtime());
    test_for_error("today date reverse $date", "Дата старта кампании указана неверно", $campaign);

    $date = $campaign->{$f} = 'someString';
    test_for_error("today date reverse $date", "Дата старта кампании указана неверно", $campaign);
};

subtest 'finish_date validation ' => sub {
    my $f = "finish_time";
    check_date_field_ok($f);

    my $campaign = get_campaign();

    my $date = $campaign->{$f} = strftime("%d-%m-%Y", localtime());
    test_for_error("today date reverse $date", "Дата окончания кампании указана неверно", $campaign);

    $date = $campaign->{$f} = 'someString';
    test_for_error("today date reverse ", "Дата окончания кампании указана неверно", $campaign);
};

sub check_date_field_ok {
    my $f = shift;
    my $campaign = get_campaign();

    my $date = $campaign->{$f} = strftime("%Y-%m-%d", localtime());
    validates_ok("today date $date", $campaign);

    $date = $campaign->{$f} = strftime("%Y-%m-%d", localtime(time() + 24*3600*7));
    validates_ok("next week date $date", $campaign);

}

sub validates_ok {
    my $msg = shift;
    my @errors = Common::validate_camp(@_, 1234);
    if(@errors) {
        warn "fails with " . join("\n", @errors);
    }
    ok(!@errors, $msg);
}

sub get_campaign {
    state $campaign = {
        cid              => 123,
        name             => 'Тестовая кампания',
        mediaType        => 'text',
        fio              => 'Федор Федорович Федоров',
        email            => 'nobody@nothere.ru',
        start_time       => '2000-01-01',
        ClientID         => 12345,
        meaningful_goals => [],
    };
    return dclone($campaign);
}

sub test_for_error {
    my ($testing, $error_msg, @params) = @_;
    my @errors = Common::validate_camp(@params, 1234);
    foreach (@errors) {
        return pass($testing) if /$error_msg/;
    }
    say "failed with error messages:\n" . join("\n", @errors) . "\n\twhile expected $error_msg";
    fail($testing);
}

done_testing;
