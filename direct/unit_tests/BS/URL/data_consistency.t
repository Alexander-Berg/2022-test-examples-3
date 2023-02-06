#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use BS::URL;

for my $host_type (sort keys %BS::URL::TYPE2HOST) {
    ok(defined $BS::URL::TYPE2HOST{ $host_type }->{host}, "host defined for host_type '$host_type'");
}

for my $name (sort keys %BS::URL::DATA) {
    my $info = $BS::URL::DATA{ $name };
    ok(defined $info->{url_path}, "$name: url_path defined");
    ok(defined $info->{host_type}, "$name: host_type defined");
    ok($info->{host_type} && exists $BS::URL::TYPE2HOST{ $info->{host_type} }, "$name: host_type '$info->{host_type}' described in \%TYPE2HOST");
    my $preprod_ok = !(exists $info->{has_preprod} && $info->{has_preprod})
                     || (exists $BS::URL::TYPE2HOST{ $info->{host_type} }
                         && exists $BS::URL::TYPE2HOST{ $info->{host_type} }->{pre}
                         && $BS::URL::TYPE2HOST{ $info->{host_type} }->{pre}
                        )
                     ;
    ok($preprod_ok, "$name: has_preprod and preprod host defined");
}

done_testing();
