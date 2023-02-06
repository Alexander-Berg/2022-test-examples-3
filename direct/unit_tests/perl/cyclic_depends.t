#!/usr/bin/perl

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

my %CYCLIC_SKIP = map {$_ => 1} qw/
        /;
my %OUTER_SKIP = map {$_ => 1} qw/
        Yandex::HighlightWords
        /;

my $LIBROOT = "$Settings::ROOT/protected";

# ищем все перловые файлы
my %MODULES;
for my $file (grep {-f && /\.p[m]$/} Test::ListFiles->list_repository($LIBROOT)) {
    (my $mod_name = $file) =~ s/^$LIBROOT\///;
    $mod_name =~ s/\//::/g;
    $mod_name =~ s/\.pm$//;
    $MODULES{$mod_name} = $file;
}
Test::More::plan(tests => 
                 scalar(keys %MODULES)
                 + scalar(grep {/^Yandex::/} keys %MODULES)
    );

# вычисляем все зависимости
my %DEPS;
for my $mod (sort keys %MODULES) {
    my $cont = scalar read_file $MODULES{$mod};
    $cont =~ s/\n=\w+.*?\n=cut//gs;
    $cont =~ s/\n__(END|DATA)__\s*\n.*//s;
    for my $dep ($cont =~ /(?:;|^|\n)\s*(?:use|require)\s+([a-z0-9_:]+)/gi) {
        # пропускаем системные модули
        next if !exists $MODULES{$dep};
        $DEPS{$mod}{$dep} = [];
    }
}

# ищем зависимости Yandex от не-Yandex
for my $mod (sort grep {/^Yandex::/} keys %MODULES) {
  SKIP: {
      skip "Old code", 1 if $OUTER_SKIP{$mod};
      my @invalid_deps = grep {!/^Yandex::/} keys %{$DEPS{$mod} || {}};
      if (@invalid_deps) {
          fail("$mod depends of outer modules:: ".join(', ', @invalid_deps));
      } else {
          pass("no outer depends for $mod");
      }
    }
}

my $changed = 1;
while($changed) {
    $changed = 0;
    for my $mod (sort keys %DEPS) {
        for my $dep (sort keys %{$DEPS{$mod}}) {
            next if !exists $DEPS{$dep};
            for my $dep_dep (sort keys %{$DEPS{$dep}}) {
                next if exists $DEPS{$mod}{$dep_dep};
                $DEPS{$mod}{$dep_dep} = [@{$DEPS{$mod}{$dep}}, $dep, @{$DEPS{$dep}{$dep_dep}}];
                $changed = 1;
            }
        }
    }
}

# ищем циклы
for my $mod (sort keys %MODULES) {
  SKIP: {
      skip "Old code", 1 if $CYCLIC_SKIP{$mod};
      if (exists $DEPS{$mod}{$mod}) {
          fail("cycles for $mod: ".join('->', $mod, @{$DEPS{$mod}{$mod}}));
      } else {
          pass("no cycles for $mod");
      }
    }
}

