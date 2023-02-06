package FastRes::Tester::SerpData;

use common::sense;

use FastRes::Creator::SerpData;

sub new {
    my ($class) = @_;

    return bless {}, ref($class) || $class;
}

sub verify_syntax {
    my ($self, $obj) = @_;
    return { errors => ["There are no serpdata field in object or it has wrong format (not hash)."] } unless $obj->{serpdata} && ref($obj->{serpdata}) eq 'HASH';
    
    return FastRes::Creator::SerpData::create_serpdata($obj);
}

1;
