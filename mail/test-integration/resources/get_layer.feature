Feature: LayerAction.getLayer test cases

  Background:
      Given users
          | login    | type  |
          | owner    | BASIC |
          | attendee | YT    |
          | private  | BASIC |
          | public   | BASIC |
          | invited  | BASIC |
      Given layer 'New layer' (owned by 'owner')
      When update invitations to layer 'New layer' (owned by 'owner') with permissions
          | login            | perm        |
          | attendee         | EDIT        |
          | invited          | CREATE      |
      And share layer 'New layer' (owned by 'owner') by private token with 'private'
      And share layer 'New layer' (owned by 'owner') by layer id with 'public'


  Scenario: Get own layer
      Then participants returned by get-layer requested by 'owner' for layer 'New layer' (owned by 'owner') are
          | login    | perm   |
          | attendee | EDIT   |
          | private  | VIEW   |
          | invited  | CREATE |
      And get-layer requested by 'owner' for layer 'New layer' (owned by 'owner') responses that the requester is the owner


  Scenario Outline: Get shared to <requester> layer
      Then participants returned by get-layer requested by '<requester>' for layer 'New layer' (owned by 'owner') are empty
      And get-layer requested by '<requester>' for layer 'New layer' (owned by 'owner') responses that the requester is participant

      Examples:
          | requester |
          | attendee  |
          | private   |
          | public    |
