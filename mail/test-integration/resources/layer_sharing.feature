Feature: Sharing layers with other users

  Scenario: Create new layer
  New layer should have only one participant with perm=ADMIN and no invitations.
    Given users
      | login | type  |
      | owner | BASIC |
    And layer 'New layer' (owned by 'owner')

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login | perm  |
      | owner | ADMIN |
    And invitations to layer 'New layer' (owned by 'owner') should be empty


  Scenario: Share layer via private link
  New participant will have VIEW permissions
    Given users
      | login   | type  |
      | owner   | BASIC |
      | private | BASIC |
    And layer 'New layer' (owned by 'owner')

    When share layer 'New layer' (owned by 'owner') by private token with 'private'

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login   | perm  |
      | owner   | ADMIN |
      | private | VIEW  |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login   | perm |
      | private | VIEW |


  Scenario: Share layer via public link
  New participant will have ACCESS permissions and no invitation
    Given users
      | login  | type  |
      | owner  | BASIC |
      | public | BASIC |
    And layer 'New layer' (owned by 'owner')

    When share layer 'New layer' (owned by 'owner') by layer id with 'public'

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login  | perm   |
      | owner  | ADMIN  |
      | public | ACCESS |
    And invitations to layer 'New layer' (owned by 'owner') should be empty


  Scenario Outline: Share layer with permission level <perm> and accept invitation
  It is a base case for sharing via invitation (not via link). Owner invites another user to a layer and then invitee accepts
  the invitation
    Given users
      | login   | type  |
      | owner   | BASIC |
      | invitee | BASIC |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm   |
      | invitee | <perm> |
    And 'invitee' reply 'YES' to 'New layer' (owned by 'owner') layer invitation

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login   | perm   |
      | owner   | ADMIN  |
      | invitee | <perm> |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login   | perm   |
      | invitee | <perm> |

    Examples:
      | perm   |
      | LIST   |
      | VIEW   |
      | CREATE |
      | EDIT   |


  Scenario: Share layer with another user and do not answer to invitation
  Just invite another user.
    Given users
      | login   | type  |
      | owner   | BASIC |
      | invitee | BASIC |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm |
      | invitee | VIEW |

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login | perm  |
      | owner | ADMIN |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login   | perm |
      | invitee | VIEW |


  Scenario: Share layer with another user and reject invitation
  Owner invites and invitee answers 'NO'
    Given users
      | login   | type  |
      | owner   | BASIC |
      | invitee | BASIC |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm |
      | invitee | VIEW |
    And 'invitee' reply 'NO' to 'New layer' (owned by 'owner') layer invitation

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login | perm  |
      | owner | ADMIN |
    And invitations to layer 'New layer' (owned by 'owner') should be empty


  Scenario: Share layer with another user and then revoke invitation
  Owner invites, invitee accept invitation and owner revoke invitation
    Given users
      | login   | type  |
      | owner   | BASIC |
      | invitee | BASIC |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm |
      | invitee | VIEW |
    And 'invitee' reply 'YES' to 'New layer' (owned by 'owner') layer invitation
    And owner 'owner' revoke all invitations to layer 'New layer'

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login | perm  |
      | owner | ADMIN |
    And invitations to layer 'New layer' (owned by 'owner') should be empty


  Scenario: Sharing of layer do not drop self-invited users
  Self-invited users do not have layer_invitations and owner of layer do not see them in the list of invited users
    Given users
      | login   | type  |
      | owner   | BASIC |
      | public  | BASIC |
      | invitee | BASIC |
    And layer 'New layer' (owned by 'owner')

    When share layer 'New layer' (owned by 'owner') by layer id with 'public'
    And update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm |
      | invitee | VIEW |
    And 'invitee' reply 'YES' to 'New layer' (owned by 'owner') layer invitation

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login   | perm   |
      | owner   | ADMIN  |
      | public  | ACCESS |
      | invitee | VIEW   |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login   | perm |
      | invitee | VIEW |


  Scenario: Share layer with a self-shared user
  A user first get access to a layer by public link and then owner invite him to the layer
    Given users
      | login   | type  |
      | owner   | BASIC |
      | invitee | BASIC |
    And layer 'New layer' (owned by 'owner')

    When share layer 'New layer' (owned by 'owner') by layer id with 'invitee'
    And update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm |
      | invitee | EDIT |

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login   | perm  |
      | owner   | ADMIN |
      | invitee | EDIT  |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login   | perm |
      | invitee | EDIT |


  Scenario: Update sharing
  Invite two user to layer. Then change permissions for the first one, revoke invitation for another and invite a third one.
    Given users
      | login   | type  |
      | owner   | BASIC |
      | invitee | BASIC |
      | another | BASIC |
      | third   | BASIC |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm   |
      | invitee | CREATE |
      | another | EDIT   |
    And 'invitee' reply 'YES' to 'New layer' (owned by 'owner') layer invitation
    And 'another' reply 'YES' to 'New layer' (owned by 'owner') layer invitation
    And update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login   | perm |
      | invitee | EDIT |
      | third   | LIST |
    And 'third' reply 'YES' to 'New layer' (owned by 'owner') layer invitation

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login   | perm  |
      | owner   | ADMIN |
      | invitee | EDIT  |
      | third   | LIST  |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login   | perm |
      | invitee | EDIT |
      | third   | LIST |


  Scenario: Ignore invitation for layer owner
    Given users
      | login | type  |
      | owner | BASIC |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login | perm   |
      | owner | CREATE |

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login | perm  |
      | owner | ADMIN |
    And invitations to layer 'New layer' (owned by 'owner') should be empty


  Scenario: Auto-accept invitations for corp
  If we share layer in corp calendar then invitation accepted automatically and email message just inform invitee.
    Given users
      | login  | type  |
      | owner  | BASIC |
      | yateam | YT    |
    And layer 'New layer' (owned by 'owner')

    When update invitations to layer 'New layer' (owned by 'owner') with permissions
      | login  | perm |
      | yateam | VIEW |

    Then users of layer 'New layer' (owned by 'owner') should have the following permissions
      | login  | perm  |
      | owner  | ADMIN |
      | yateam | VIEW  |
    And invitations to layer 'New layer' (owned by 'owner') should offer the following permissions
      | login  | perm |
      | yateam | VIEW |
    And email message for 'yateam@yandex-team.ru' should not contains an invitation
