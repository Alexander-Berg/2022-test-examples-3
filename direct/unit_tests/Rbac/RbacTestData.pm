package RbacTestData;

use Direct::Modern;

use Settings;

use Rbac ':const';

our $DATASET = {
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1, },
            { ClientID => 2, shard => 2, },
            { ClientID => 3, shard => 2, }, # ag
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 6, ClientID => 1, },
            { uid => 7, ClientID => 2, },
            { uid => 8, ClientID => 1, },

            { uid => 10, ClientID => 3, },
            { uid => 11, ClientID => 3, },
            { uid => 12, ClientID => 3, },
            { uid => 13, ClientID => 3, },
        ],
    },
    shard_login => {
        original_db => PPCDICT,
        rows => [
            { login => 'l-6', uid => 6, },
            { login => 'l-7', uid => 7, },
            { login => 'l-8', uid => 8, },

            { login => 'l-10', uid => 10, },
            { login => 'l-11', uid => 11, },
            { login => 'l-12', uid => 12, },
            { login => 'l-13', uid => 13, },
        ],
    },

    clients => {
        original_db => PPC(shard => 'all'),
        rows => {1 => [ { ClientID => 1, chief_uid => 6, role => 'client', subrole => undef, agency_client_id => 3, agency_uid => 13, perms => 'super_subclient', 'primary_manager_set_by_idm' => 1, primary_manager_uid => 77 } ],
                 2 => [ 
                     { ClientID => 2, chief_uid => 7, role => 'client', subrole => undef, agency_client_id => undef, agency_uid => undef},
                     { ClientID => 3, chief_uid => 10, role => 'agency', subrole => undef, agency_client_id => undef, agency_uid => undef, },
                     ],
                },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {1 => [ 
                     { uid => 6, rep_type => 'chief', login => 'l-6', ClientID => 1 }, 
                     { uid => 8, rep_type => 'main', login => 'l-8', ClientID => 1 }, 
                     ],
                 2 => [ 
                     { uid => 7, rep_type => 'chief', login => 'l-7', ClientID => 2 }, 
                     { uid => 10, rep_type => 'chief', login => 'l-10', ClientID => 3 },
                     { uid => 11, rep_type => 'main', login => 'l-11', ClientID => 3 },
                     { uid => 12, rep_type => 'limited', login => 'l-12', ClientID => 3 },
                     { uid => 13, rep_type => 'limited', login => 'l-13', ClientID => 3 },
                 ]
                },
    },
    agency_lim_rep_clients => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{ ClientID => 1, agency_uid => 12 }],
        }
    },
    reverse_clients_relations => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{ reverse_relation_id => 1, client_id_from => 1, client_id_to => 2, type => "mcc" }],
        }
    },

};


1;
