#!/usr/bin/perl

# $Id$


=head1 NAME

    run_intapi_tests.pl

=head1 DESCRIPTION

    Запуск тестов на внутреннее API Директа

    Параметры: 

    -u, --url <url> 
    url, который тестируем
    Без схемы, т.е. например //8804.beta.direct.yandex.ru
    Можно указывать только номер беты: -u 8999

    -v, --verbose 
    выводить в STDERR подробную информацию о выполняемых http-запросах

    --write-allow
    запускать все тесты, не только read-only

    -t, --timeout <nnn>
    НЕ РЕАЛИЗОВАНО таймаут, с которым выполнять запросы
    
    -btb
    url для back to back тестирования(аналогично --url)


    Можно указать список файлов/каталогов, из которых добывать тесты (.t)
    Можно не указывать, подразумевается intapi_tests

    Беты
    ./intapi_tests/run_intapi_tests.pl -u '//8804.beta.direct.yandex.ru'
    ./intapi_tests/run_intapi_tests.pl -u '//8804.beta.direct.yandex.ru' intapi_tests/intapi/DirectConfiguration.t
    ./intapi_tests/run_intapi_tests.pl --write-allow -u '//8805.beta.direct.yandex.ru' intapi_tests/common intapi_tests/sandbox

    ТС
    ./intapi_tests/run_intapi_tests.pl --write-allow -u '//test-direct.yandex.ru' intapi_tests/common/alive.t

    Тест-Песочница
    ./intapi_tests/run_intapi_tests.pl --write-allow -u '//ppctest-sandbox-front.yandex.ru:17080' intapi_tests/sandbox

    Продакшен (только безопасные, read-only тесты)
    ./run_intapi_tests.pl -u '//ppcback01f.yandex.ru' common/alive.t
    ./run_intapi_tests.pl -u '//ppcback01f.yandex.ru:9000' intapi/UserRole.t

=cut

use strict;
use warnings;

use FindBin qw/$Bin/;

use Getopt::Long;
use Data::Dumper;

use utf8;
use open ':std' => ':utf8';

sub run
{
    
    my ($backtoback, $VERBOSE, $url, $timeout) = ('', '', '', '');
    my $WRITE_ALLOW = 0;
    GetOptions(
        "v|verbose" => \$VERBOSE,
        "u|url=s"   => \$url,
        "btb=s" => \$backtoback,
        "t|timeout=i" => \$timeout,
        "write-allow!" => \$WRITE_ALLOW,
        "h|help" => sub {
            system("podselect -section NAME -section DESCRIPTION $0 | pod2text-utf8 >&2"); 
            exit 0;
        },
    );
    foreach ($url, $backtoback) {
        $_ = "//${_}.beta1.direct.yandex.ru" if /^\d{4,5}$/
    }
    
    $ENV{TEST_INTAPI_BASE_URL} = $url or die "url missed\n";
    $ENV{TEST_INTAPI_VERBOSE} = $VERBOSE;
    $ENV{TEST_INTAPI_WRITE_ALLOW} = $WRITE_ALLOW;
    $ENV{TEST_INTAPI_BTB_URL} = $backtoback if $backtoback;

    my @targets = @ARGV;
    push @targets, $Bin if !@targets; 

    system "prove -I$Bin/../protected ".join " ", @targets;
    exit $? >> 8
}

run() unless caller();
