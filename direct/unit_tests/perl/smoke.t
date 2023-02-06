#!/usr/bin/perl

use Direct::Modern;
use Test::More;

=head1 DESCRIPTION

Смоук-тест - быстрая проверка на компилируемость всей кодовой базы Директа

Собирает все "use SOMETHING" из кода, записывает их во временный файл, и убеждается
что этот файл успешно обрабатывается perl -cw

Отдельно ищет все модифицированные *.pl и проверяет их компилируемость

=cut

use my_inc '..', for => 'protected';

use Path::Tiny;
use List::MoreUtils qw/uniq any/;

use Test::ListFiles;

use Settings;

my @files = grep { !m!/archive/! && /\.(?:pl|pm|psgi)$/ && !m!/data/lego/! } Test::ListFiles->list_repository($Settings::ROOT);

my $tmp = Path::Tiny->tempfile();
# my $tmp = path('tmp');

my @use = qw/
    PPCLoginBox
    Apache::DebugWSDL
    API::PreforkLoad
    Apache::DebugMergeStatic
/;

my $skip_re = join "|", qw/
lib::abs
hahn
lib
utf8
vars
open
my_inc
feature
warnings
[\d\.]+
Direct::AdGroupss
MRStreaming
ModuleErrors
SettingsALL
Yandex::JuggleMetadata
Test::
Exporter
Mouse
Mouse::Exporter
POSIX
geobase
YaCatalog
WSLD::JSON::
/;

for my $f (@files) {
    my @lines = path($f)->lines_utf8({ chomp => 1 });
    push @use, grep { !/^($skip_re)/ } map { /use\s+([\w\d\:\.]+)\W*/ && $1 } grep { /^\s*use\s+[\w\d\:]+/ } @lines;
}

push @use, map { /module\s+=>\s+'([\w\d\:\.]+)'/ ? ($1) : () } path("$Settings::ROOT/protected/Intapi.pm")->lines_utf8();

@use = uniq sort @use;

my $code = qq!
use my_inc '$Settings::ROOT', for => 'protected';
use my_inc '$Settings::ROOT', for => 'api/lib';
use my_inc '$Settings::ROOT', for => 'api/services/v5';
!;
$code .= join "\n", map { "use $_ qw();" } @use;
$tmp->spew($code);

my $res = `perl -cw '$tmp' 2>&1`;

my $rv = $? >> 8;

ok($rv == 0, $res);

ok($res =~ /\Q$tmp\E syntax OK/, $res);

my @changed = grep { /\.pl$/ } @{Yandex::Svn::svn_file_status($Settings::ROOT)->{modified}//[]};

for my $f (@changed) {
    $res = `perl -cw '$f' 2>&1`;
    my $rv = $? >> 8;
    ok($rv == 0, "$f: $res");
    ok($res =~ /\Q$f\E syntax OK/, "$f: $res");
}

done_testing();
