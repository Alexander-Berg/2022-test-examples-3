#!/usr/bin/env perl

=pod

=encoding utf-8

=head1 NAME

dependencies_not_dirty - проверка, что в завимостях нет dirty-пакетов

=cut

use Direct::Modern;

use Path::Tiny;
use Test::More tests => 1;

my $control_file = path(path($0)->dirname)->child("../../packages/yandex-direct/debian/control");
my $control = $control_file->slurp;

# текст с зависимостями для сборки
my ($build_dep_text) = ($control =~ m/Build-Depends: (.*?)Standards-Version:/sm);

# текст со списком зависимостей
my @dep_texts = ($control =~ /^Depends: (.*?)^\w+:/smg);
my @deps;

foreach my $dep_text ($build_dep_text, @dep_texts) {
    # массив отельных строчек вида 'libtemplate-perl (= 2.22-1)'
    push @deps, grep {$_} split /\s*[,|]\s*/, join "\n", grep {!/^#/} split "\n", $dep_text;
}

# массив строчек с версиями вида '>= 2.22-1'
my @versions = map {s/.*\((.*)\).*/$1/; $_} grep {/\(.*\)/} @deps;

my @dirty = grep {/dirty/} @versions;
if (@dirty) {
    ok(0, ('dirty versions found, look for these in control file: ' . join ', ', @dirty));
} else {
    ok(1, 'no dirty dependencies found');
}
