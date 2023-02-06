package ru.yandex.direct.grid.processing.util

const val METRIKA_COUNTERS_BY_DOMAIN_QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        metrikaCountersByDomain(input: %s) {
          allAccessibleCounters {
            name
            domain
            id
          }
          accessibleCountersByDomain {
            name
            id
            domain
          }
          isMetrikaAvailable
          inaccessibleCountersByDomain {
            id
            accessRequested
          }
        }
      }
    }"""

const val SHOW_CPM_UAC_PROMO_PARAMS_QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        showCpmUacPromoParams {
          colorLeft,
          colorRight,
          showCpmUacPromo,
          showCpmUacPromoBonus
        }
      }
    }"""

const val CPM_UAC_NEW_CAMP_QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        cpmUacNewCamp
      }
    }"""

const val REQUEST_METRIKA_COUNTERS_ACCESS_QUERY_TEMPLATE = """
    mutation {
      requestMetrikaCountersAccess(input: %s) {
        success
        isMetrikaAvailable
      }
    }"""

const val METRIKA_COUNTERS_INFO_QUERY_TEMPLATE = """
    {
      metrikaCountersInfo(input: %s) {
        counters {
          id
          status
        }
        isMetrikaAvailable
      }
    }"""

const val USER_PHONES_QUERY_TEMPLATE = """
    {
      userPhones {
        directPhone {
          phone
          phoneId
          isConfirmed
        }
        passportPhones {
          phone
          phoneId
          isConfirmed
        }
      }
    }"""

const val UPDATE_USER_VERIFIED_PHONE_QUERY_TEMPLATE = """
    mutation {
      updateUserVerifiedPhone(input: %s) {
        success
      }
    }"""

const val CONFIRM_AND_BIND_SUBMIT_QUERY_TEMPLATE = """
    {
      confirmAndBindPhoneSubmit(input: %s) {
        trackId
        validationResult {
          errors {
            code
            path
            params
          }
        }
      }
    }"""

const val CONFIRM_AND_BIND_COMMIT_QUERY_TEMPLATE = """
    {
      confirmAndBindPhoneCommit(input: %s) {
        phoneId
        validationResult {
          errors {
            code
            path
            params
          }
        }
      }
    }"""

const val GEO_SUGGEST_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        geoSuggest
      }
    }
"""

const val CAMPAIGNS_PROMO_EXTENSION_QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        campaigns(input: %s) {
          rowset {
            id
            ... on GdTextCampaign {
              promoExtension {
                id
                href
                description
                status
                startDate
                finishDate
                prefix
                type
                unit
                amount
                associatedCids
              }
            }
            ... on GdDynamicCampaign {
              promoExtension {
                id
                href
                description
                status
                startDate
                finishDate
                prefix
                type
                unit
                amount
                associatedCids
              }
            }
          }
        }
      }
    }
"""

const val PROMO_EXTENSIONS_QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        promoExtensions(input: %s) {
          rowset {
            id
            href
            description
            status
            startDate
            finishDate
            prefix
            type
            unit
            amount
            associatedCids
          }
        }
      }
    }
"""

const val SUGGEST_DATA_BY_URL_QUERY_TEMPLATE = """
    {
      suggestDataByUrl(input: %s) {
        counterIds
        counters {
          id
          isEditableByOperator
          isCalltrackingOnSiteCompatible
          isAccessible
          isAccessRequested
        }
        calltrackingOnSite {
          campaignIds
          settingsId
          counterId
          hasExternalCalltracking
          phones {
            hasClicks
            redirectPhone
            isCalltrackingOnSiteActive
            isCalltrackingOnSiteEnabled
          }
        }
        goals {
          ...GoalInfoFragment
        }
        campaignSettings {
          socialNetworkAccount {
            username
            type
          }
        }
      }
    }
    fragment GoalInfoFragment on GdGoal {
      id
      metrikaGoalType
      counterId
    }
"""

const val SUGGEST_METRIKA_DATA_BY_URL_QUERY_TEMPLATE = """
    {
      suggestMetrikaDataByUrl(input: %s) {
        counterIds
        counters {
          id
          isEditableByOperator
          isCalltrackingOnSiteCompatible
          isAccessible
          isAccessRequested
        }
        goals {
          ...GoalInfoFragment
        }
      }
    }
    fragment GoalInfoFragment on GdGoal {
      id
      metrikaGoalType
      counterId
    }
"""

const val CAMPAIGNS_WITH_STATS_AND_ACCESS_QUERY_TEMPLATE = """
    {
      client(searchBy: {userId: %s}) {
        campaigns(input: %s) {
          rowset {
             id
             type
             stats {
                 shows
                 clicks
                 cost
                 revenue
             }
             access {
                 canEdit
                 noActions
                 actions
                 pseudoActions
                 servicedState
             }
          }
          totalStats {
              shows
              clicks
              cost
              revenue
          }
        }
      }
    }
"""

const val GET_METRIKA_GOALS_BY_COUNTER_QUERY_TEMPLATE = """
    {
        getMetrikaGoalsByCounter(input: %s) {
          goals {
            id
            counterId
          }
          validationResult {
            errors {
              code
              path
            }
          }
        }
    }
"""

const val GET_GOALS_FOR_UPDATE_CAMPAIGNS_QUERY_TEMPLATE = """
    {
        getGoalsForUpdateCampaigns(input: %s) {
          rowset {
            id
            counterId
          }
          validationResult {
            errors {
              code
              path
            }
          }
        }
    }
"""

const val GET_RECOMMENDED_GOALS_COST_PER_ACTION_FOR_NEW_CAMPAIGN_QUERY_TEMPLATE = """
    {
      client(searchBy: {userId: %s}) {
        getRecommendedGoalsCostPerActionForNewCampaign(input: %s) {
          recommendedGoalsCostPerAction {
             id
             costPerAction
          }
        }
      }
    }
"""

const val GET_RECOMMENDED_CAMPAIGNS_GOALS_COST_PER_ACTION_QUERY_TEMPLATE = """
    {
      client(searchBy: {userId: %s}) {
        getRecommendedCampaignsGoalsCostPerAction(input: %s) {
          recommendedCampaignsGoalsCostPerAction {
             campaignId
             recommendedGoalsCostPerAction {
                 id
                 costPerAction
             }
          }
        }
      }
    }
"""

const val GET_CAMPAIGN_GOALS_QUERY_TEMPLATE = """
    {
      client(searchBy: {userId: %s}) {
        campaignGoals(input: %s) {
          totalCount
          rowset {
            id
            name
            counterId
          }
        }
      }
    }
"""

const val ADD_ADGROUP_TEMPLATE = """
    mutation {
        %s (input: %s) {
            validationResult {
                errors {
                    code
                    path
                    params
                }
            }
            addedAdGroupItems {
                adGroupId
            }
        }
    }
"""

const val UPDATE_ADGROUP_TEMPLATE = """
    mutation {
        %s (input: %s) {
            validationResult {
                errors {
                    code
                    path
                    params
                }
            }
            updatedAdGroupItems {
                adGroupId
            }
        }
    }
"""

const val UPDATE_WEEKLY_BUDGET_MUTATION_TEMPLATE = """
 mutation {
  updateCampaignsWeeklyBudget (input: %s) {
    validationResult {
      errors {
        code
        path
        params
      }
    }
    updatedCampaigns {
        id
    }
  }
}
"""
