#!/usr/bin/perl

=encoding utf8

=head2 DESCRIPTION

    Проверяем, что все версии в словаре %MobileContent::OS_VERSIONS записаны в "мажорной" нотации

=cut

use Direct::Modern;

use Test::More;

use MobileContent;

for my $os_type (keys(%MobileContent::OS_VERSIONS)) {
    for my $version (@{ $MobileContent::OS_VERSIONS{$os_type} }) {
        is($version, MobileContent::get_major_os_version($version), "$os_type version $version looks like 'major'");
    }
}

done_testing();
