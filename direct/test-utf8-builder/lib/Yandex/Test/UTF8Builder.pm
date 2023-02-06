package Yandex::Test::UTF8Builder;

=pod
    $Id$
    обертка для Test::Builder(для работы с utf8)
    в юнит тесте достаточно написать use Yandex::Test::UTF8Builder
=cut

use utf8;

use strict;
use warnings;
use Test::Builder;

for my $fh (Test::Builder->new()->todo_output, Test::Builder->new()->failure_output, Test::Builder->new()->output) {
    binmode($fh, ":encoding(utf8)");
}


1;
