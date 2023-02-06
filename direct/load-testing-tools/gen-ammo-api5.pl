#!/usr/bin/perl
use strict;
use warnings;

# запускается на бете (ходит в базу за логинами) с SETTINGS_LOCAL_SUFFIX=<конфигурация среды, куда будем стрелять>
# конфигурация цели нужна, чтобы выкинуть логины, которых там нет.
# на входе результат yt read --format json //tmp/<...> # имя таблицы с результатами выполнения YQL-запроса в ppclog_api
# пример запроса https://yql.yandex-team.ru/Operations/59a59e4551198d5324562c13

use my_inc for=>'protected';

use Encode qw/encode_utf8/;
use JSON;
use Settings;
use PrimitivesIds;
use Getopt::Long;

use utf8;
use open qw/:std :encoding(UTF-8)/;

my @supported_methods = qw/get setAuto/;
my %O;
# TODO принимать на вход таблицу с cmd и брать метод оттуда
GetOptions(
    'method=s' => \$O{method},
) or die "can't parse options, stop\n";
die "no method given" unless $O{method};
die sprintf("unknown method '%s', must be one of %s", $O{method}, join(', ', @supported_methods)) unless grep {$O{method} eq $_} @supported_methods;

my $user_agent = 'lunapark';
my $token = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA';   # для "фальшивого" паспорта подойдёт любая последовательность, которая пройдет валидацию 

while (<>) {
my $r = from_json($_);
my $req = to_json( { method => $O{method}, params => from_json($r->{param}) } );
my $login = get_login(uid => $r->{cluid});
next unless $login;
my $request = sprintf "POST /json/v5/bids HTTP/1.0\nHost: test-direct.yandex.ru\nContent-Length: %d\nAuthorization: Bearer %s\nClient-Login: %s\nUser-Agent: %s\n\n%s", length(encode_utf8($req)), $token, $login, $user_agent, $req;
my $str = sprintf "%d\n%s\n", length(encode_utf8($request)), $request;
print $str;
}
