#!/usr/bin/perl

=head "script must be running under switchman"

    Зачем? Все скрипты (особенно новые) должны запускать через switchman, это дает устойчивость
    к учениям, человеческим ошибкам, отказам железа.

    Что делать? Добавить в кронтаб параметры для запуска скрипта под switchman.
    При этом нужно будет не забыть изменить пакет на scripts-switchman.
    Как выглядят параметры (должны быть внутри <crontab>...</>), простой вариант:
    <switchman>
        group: scripts-other
    </switchman>

    Вариант посложнее, где XXX - объем потребляемой скриптом резидентной (RES) памяти в МБ.
    Имеет смысл указывать, если скрипт потребляет много (сотни/тысячи) мегабайт памяти, прикинуть
    значение можно запустив скрипт на бете с добавлением "sleep 180;" перед выходом и посмотрев
    потребление памяти по списку процессов (htop, ps, top). Для случаев продакшена можно попросить
    админов помочь понять, сколько скрипт потребляет памяти.
    Гнаться за точностью здесь не нужно, нужна "сферическая в вакууме" оценка сверху.
    <switchman>
        group: scripts-other
        <leases>
            mem: XXX
        </leases>
    </switchman>

    Посоветоваться всегда можно с ppalex@, pankovpv@, zhur@.

=cut

use strict;
use warnings;

use Path::Tiny;
use Test::Deep;
use Test::More;

use Test::ListFiles;

use ScriptsMetadata;
use Settings;

my @SWITCHMAN_PACKAGES = qw(
    scripts-switchman
    scripts-bs
    scripts-export
    scripts-limtest1
    scripts-limtest2
    conf-test-scripts
);
my @SWITCHMAN_GROUPS = qw(
    scripts-bs
    scripts-export
    scripts-other
    scripts-test
    scripts-resharding
);
# scripts-resharding: DIRECT-172412
my %PACKAGES_WITHOUT_SWITCHMAN = map {$_ => 1} '', qw(
    conf-dev
    conf-sandbox
    conf-sandbox-test
    conf-sandbox-test2
    conf-test2
    dev-scripts
    frontend
    internal-networks
    intapi
    monitoring
    scripts-sandbox
    scripts-ppcback
);

my %SCRIPTS_IN_SWITCHMAN_PACKAGES_NOT_UNDER_SWITCHMAN = map { $_ => 1 } qw(
    apiReportsBuilderCleanTempFiles.pl
);

my @files = grep {
    -f && (/\.p[ly]$/)
} (
    Test::ListFiles->list_repository("$Settings::ROOT/protected"),
    Test::ListFiles->list_repository("$Settings::ROOT/python/scripts"),
);

local $ScriptsMetadata::MEM_NAME_IN_META = $Settings::SWITCHMAN_MEM_LEASE_NAME_IN_META;
local $ScriptsMetadata::JUGGLER_CHECK_HOST_IS_MANDATORY = 1;

my @VALID_SWITCHMAN_LEASES = (
    $Settings::SWITCHMAN_MEM_LEASE_NAME_IN_META,
    'FQDN_reexport.workers',
    'FQDN_reexport.workers2',
);

my $path_prefix = "$Settings::ROOT/protected";
my %type2validator = (
    crontab => sub {
        my ($section, $conf, $file) = @_;
        my @crontab_errors;
        push @crontab_errors, ScriptsMetadata::validate_crontab($_[0],
                                                                package_prefix => 'yandex-direct',
                                                                switchman_packages => \@SWITCHMAN_PACKAGES,
                                                                switchman_groups => \@SWITCHMAN_GROUPS,
                                                                switchman_leases => \@VALID_SWITCHMAN_LEASES,
                                                                );
        if (! $PACKAGES_WITHOUT_SWITCHMAN{ $_[0]->{package} } ) {
            my $basename = path($file)->basename;
            if ( !$SCRIPTS_IN_SWITCHMAN_PACKAGES_NOT_UNDER_SWITCHMAN{$basename} && !$section->{switchman} ) {
                push @crontab_errors, 'script must be running under switchman';
            }

            if ( $SCRIPTS_IN_SWITCHMAN_PACKAGES_NOT_UNDER_SWITCHMAN{$basename} && $section->{switchman} ) {
                push @crontab_errors, 'script must not be running under switchman';
            }
        }
        return @crontab_errors;
    },
    monrun => \&ScriptsMetadata::validate_monrun,
    ubic => \&ScriptsMetadata::validate_ubic,
    juggler => sub {ScriptsMetadata::parse_and_validate_juggler($_[0],
                                                                files_path => "$Settings::ROOT/etc/juggler_configs",
                                                                this_file_path => $_[-1],
                                                                )},
    juggler_check => \&ScriptsMetadata::parse_and_validate_juggler_check,
);
for my $file (@files) {
    my $conf = ScriptsMetadata::get_conf($file);
    for my $type (keys %type2validator) {
        next unless $conf->{$type};
        my $sections = ref $conf->{$type} eq 'ARRAY' ? $conf->{$type} : [$conf->{$type}];
        for my $section (@$sections) {
            my @errors = $type2validator{$type}->($section, $conf, $file);
            my $test_name = join(" ", $file, $type);
            $test_name .= sprintf(": %s", join("; ", @errors)) if @errors;
            ok(!@errors, $test_name);
        }
    }
}

done_testing;
