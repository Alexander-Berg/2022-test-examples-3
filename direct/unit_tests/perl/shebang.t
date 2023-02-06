#!/usr/bin/perl

=pod
    $Id$
    Проверка валидного shebang в perl скриптах
=cut

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

# ищем все перловые файлы
my @files = grep {-f && /\.pl$/} Test::ListFiles->list_repository(["$Settings::ROOT/protected", "$Settings::ROOT/registered"]);

Test::More::plan(tests => 2*scalar(@files));

for my $file (@files) {
    # test shebang
    ok(scalar(read_file($file)) =~ m{^(#!/usr/bin/perl|#!/usr/bin/env perl)\s}, "shebang $file");
    # test permissions: файл должен быть исполняемым.
    my $perm = (stat $file)[2] & 07777;
    ok($perm & 0555 == 0555, sprintf("permissions $file: %04o, expected 'x' for everybody", $perm));
}
