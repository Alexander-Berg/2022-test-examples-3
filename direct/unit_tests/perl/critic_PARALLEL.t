#!/usr/bin/perl
use strict;
use warnings;

=pod

$Id$

critic_PARALLEL.t: запускает perlcritic с нашим набором правил на
всех файлах в папке protected/.

Когда какое-то правило в коде нарушается, тест падает. Сообщение об
ошибке при этом содержит имя файла и номер строки и даёт указания
к действию. Если с указанием к действию система определиться
не может, выдаётся (достаточно бесполезная) ссылка на страницу
в Perl Best Practices. Кроме того, система рекомендует,
какой perldoc посмотреть, чтобы узнать подробности.

Пример:

# Perl::Critic found these violations in "/var/www/.../protected/DoCmd.pm":
# /var/www/.../protected/DoCmd.pm: Variable declared in conditional statement at line 220, column 1.
# Declare variables outside of the condition
# [perldoc Perl::Critic::Policy::Variables::ProhibitConditionalDeclarations]

См. также:

Все стандартные правила perlcritic: http://search.cpan.org/~thaljef/Perl-Critic-1.118/
    + http://search.cpan.org/~thaljef/Perl-Critic-1.118/lib/Perl/Critic/PolicySummary.pod

Анализ соответствия кода Директа этим правилам: http://wiki/users/andy-ilyin/direct-perlcritic

=cut

use my_inc;
use lib::abs '.';

use PPI::Cache;
use PPI::Document;

use Path::Tiny;
use Digest::CRC qw( crc32 );
use Perl::Critic::Utils qw( all_perl_files );
use Test::More;
use Test::Perl::Critic;

use Settings;

my %SKIP_FILE = map { $_ => 1 } (
    # это тяжёлый автогенерируемый файл, его не может разобрать старая версия PPI, которая у нас используется;
    # https://rt.cpan.org/Public/Bug/Display.html?id=81616
    # кажется, проверять его на каждый коммит не обязательно, так что не проверяем
    "$Settings::ROOT/protected/geo_regions.pm",
);

# кеш для PPI
umask 0;
my $CACHE_ROOT = "/var/cache/ppc";
if (-d $CACHE_ROOT && -w $CACHE_ROOT) {
    my $ppi_cache_dir = "$CACHE_ROOT/ppi-cache-$>";
    path($ppi_cache_dir)->mkpath() unless -e $ppi_cache_dir;
    PPI::Document->set_cache( PPI::Cache->new(path => $ppi_cache_dir) );
}

my $CRITIC_PROFILE = <<'ENDPROFILE';
[BuiltinFunctions::ProhibitLvalueSubstr]
[BuiltinFunctions::ProhibitSleepViaSelect]
[BuiltinFunctions::ProhibitVoidGrep]
[BuiltinFunctions::RequireBlockGrep]
[BuiltinFunctions::RequireBlockMap]
[BuiltinFunctions::RequireGlobFunction]
[ControlStructures::ProhibitUnreachableCode]
[CodeLayout::RequireConsistentNewlines]
[ControlStructures::ProhibitLabelsWithSpecialBlockNames]
[ControlStructures::ProhibitUntilBlocks]
[InputOutput::ProhibitReadlineInForLoop]
[InputOutput::ProhibitTwoArgOpen]
[Miscellanea::ProhibitFormats]
[Miscellanea::ProhibitUnrestrictedNoCritic]
[Modules::RequireBarewordIncludes]
[Modules::RequireEndWithOne]
[Modules::RequireNoMatchVarsWithUseEnglish]
[Subroutines::ProhibitNestedSubs]
[ValuesAndExpressions::ProhibitSpecialLiteralHeredocTerminator]
[Variables::ProhibitConditionalDeclarations]
[Variables::ProhibitMatchVars]
[Variables::RequireLexicalLoopIterators]

[Freenode::AmpersandSubCalls]
[Freenode::DollarAB]
[Freenode::POSIXImports]

[TestingAndDebugging::RequireUseStrict]
equivalent_modules = Direct::Modern Mouse

[TestingAndDebugging::RequireUseWarnings]
equivalent_modules = Direct::Modern Mouse

[Modules::ProhibitEvilModules]
modules = XML::Simple E Clone

[BuiltinFunctions::ProhibitComplexMappings]
max_statements = 4

[BuiltinFunctions::ProhibitDeleteOnArrays]
[Direct::PrimitivesIds::ValidKeys]
[ControlStructures::ProhibitAssignmentInConditions]
ENDPROFILE

my ($par_id, $par_level) = (0, 1);
if (@ARGV == 1 && $ARGV[0] =~ /^(\d+)[:\/](\d+)$/) {
    ($par_id, $par_level) = ($1, $2);
} elsif (@ARGV) {
    die "Usage: $0 [par_id/par_level]";
}
$par_id %= $par_level;

my @files = all_perl_files(
    "$Settings::ROOT/api/bin",
    "$Settings::ROOT/api/lib",
    "$Settings::ROOT/api/services",
    "$Settings::ROOT/deploy",
    "$Settings::ROOT/protected",
);
@files = grep { $_ !~ m!/deploy/archive/! } @files;
@files = grep { !$SKIP_FILE{$_} } @files;
@files = grep { crc32($_) % $par_level == $par_id } @files;

Test::Perl::Critic->import(
    '-only'     => 1,
    '-profile'  => \$CRITIC_PROFILE,
    '-severity' => 1,
    '-verbose'  => "%f: %m at line %l, column %c.\n%e\n[perldoc %P]\n",
);

if (defined $ENV{UNIT_OPT_DB} && !$ENV{UNIT_OPT_DB}) {
    plan skip_all => "doesn't works properly on modules with Yandex::DBUnitTests and --no-db option";
} else {
    plan tests => scalar @files;
    critic_ok($_) foreach @files;
}
