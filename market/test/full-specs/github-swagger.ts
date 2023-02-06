import {Spec} from 'swagger-schema-official';

// Схема от mbi не полностью типизируется под swagger-schema-official, есть некоторые ошибки,
// поэтому хачим через any.
const spec: any = {
  "swagger": "2.0",
  "info": {
    "description": "Legacy servantlets and Spring MVC REST API",
    "version": null,
    "title": "Market Partner Interface Backend",
    "contact": {
      "name": "Sergey Fedoseenkov",
      "url": "https://wiki.yandex-team.ru/MBI/",
      "email": "sergey-fed@yandex-team.ru"
    }
  },
  "host": "",
  "basePath": "/mbi/api",
  "tags": [
    {
      "name": "agency-reward-controller",
      "description": "Agency Reward Controller"
    },
    {
      "name": "assortment-commit-controller",
      "description": "Assortment Commit Controller"
    },
    {
      "name": "assortment-validation-controller",
      "description": "Assortment Validation Controller"
    },
    {
      "name": "bank-info-controller",
      "description": "Bank Info Controller"
    },
    {
      "name": "cabinet-controller",
      "description": "Cabinet Controller"
    },
    {
      "name": "certification-document-controller",
      "description": "Certification Document Controller"
    },
    {
      "name": "checker-controller",
      "description": "Checker Controller"
    },
    {
      "name": "client-summary-controller",
      "description": "Client Summary Controller"
    },
    {
      "name": "cpc-placement-controller",
      "description": "Cpc Placement Controller"
    },
    {
      "name": "cpc-state-controller",
      "description": "Cpc State Controller"
    },
    {
      "name": "credit-min-price-limit-controller",
      "description": "Credit Min Price Limit Controller"
    },
    {
      "name": "credit-template-controller",
      "description": "Credit Template Controller"
    },
    {
      "name": "crossborder-campaign-controller",
      "description": "Crossborder Campaign Controller"
    },
    {
      "name": "crossborder-partner-application-controller",
      "description": "Crossborder Partner Application Controller"
    },
    {
      "name": "crossborder-partner-contract-controller",
      "description": "Crossborder Partner Contract Controller"
    },
    {
      "name": "current-stocks-report-controller",
      "description": "Current Stocks Report Controller"
    },
    {
      "name": "daily-stocks-report-controller",
      "description": "Daily Stocks Report Controller"
    },
    {
      "name": "data-camp-controller",
      "description": "Data Camp Controller"
    },
    {
      "name": "data-camp-feed-controller",
      "description": "Data Camp Feed Controller"
    },
    {
      "name": "data-camp-parameters-controller",
      "description": "Data Camp Parameters Controller"
    },
    {
      "name": "data-camp-report-controller",
      "description": "Data Camp Report Controller"
    },
    {
      "name": "data-transfer-permission-storage-controller",
      "description": "Data Transfer Permission Storage Controller"
    },
    {
      "name": "datasource-controller",
      "description": "Datasource Controller"
    },
    {
      "name": "delivery-calculator-controller",
      "description": "Delivery Calculator Controller"
    },
    {
      "name": "delivery-partner-application-controller",
      "description": "Delivery Partner Application Controller"
    },
    {
      "name": "delivery-partner-controller",
      "description": "Delivery Partner Controller"
    },
    {
      "name": "delivery-payment-controller",
      "description": "Delivery Payment Controller"
    },
    {
      "name": "detailed-cpa-orders-controller",
      "description": "Detailed Cpa Orders Controller"
    },
    {
      "name": "dynamic-price-control-controller",
      "description": "Dynamic Price Control Controller"
    },
    {
      "name": "environment-controller",
      "description": "Environment Controller"
    },
    {
      "name": "feature-controller",
      "description": "Feature Controller"
    },
    {
      "name": "feed-controller",
      "description": "Feed Controller"
    },
    {
      "name": "feed-validation-controller",
      "description": "Feed Validation Controller"
    },
    {
      "name": "fmcg-campaign-controller",
      "description": "Fmcg Campaign Controller"
    },
    {
      "name": "fmcg-info-controller",
      "description": "Fmcg Info Controller"
    },
    {
      "name": "fmcg-partner-application-controller",
      "description": "Fmcg Partner Application Controller"
    },
    {
      "name": "fulfillment-report-controller",
      "description": "Fulfillment Report Controller"
    },
    {
      "name": "fulfillment-service-controller",
      "description": "Fulfillment Service Controller"
    },
    {
      "name": "graceful-shutdown-ping-controller",
      "description": "Graceful Shutdown Ping Controller"
    },
    {
      "name": "guarantee-letter-controller",
      "description": "Guarantee Letter Controller"
    },
    {
      "name": "hidden-offer-controller",
      "description": "Hidden Offer Controller"
    },
    {
      "name": "intake-cost-controller",
      "description": "Intake Cost Controller"
    },
    {
      "name": "mapping-controller",
      "description": "Mapping Controller"
    },
    {
      "name": "market-delivery-controller",
      "description": "Market Delivery Controller"
    },
    {
      "name": "metrika-counter-controller",
      "description": "Metrika Counter Controller"
    },
    {
      "name": "metrika-counters-controller",
      "description": "Metrika Counters Controller"
    },
    {
      "name": "moderation-controller",
      "description": "Moderation Controller"
    },
    {
      "name": "new-feed-controller",
      "description": "New Feed Controller"
    },
    {
      "name": "orders-billing-report-controller",
      "description": "Orders Billing Report Controller"
    },
    {
      "name": "organization-info-controller",
      "description": "Organization Info Controller"
    },
    {
      "name": "pagematch-controller",
      "description": "Pagematch Controller"
    },
    {
      "name": "partner-application-controller",
      "description": "Partner Application Controller"
    },
    {
      "name": "partner-content-controller",
      "description": "Partner Content Controller"
    },
    {
      "name": "phone-visibility-schedule-controller",
      "description": "Phone Visibility Schedule Controller"
    },
    {
      "name": "premoderation-state-controller",
      "description": "Premoderation State Controller"
    },
    {
      "name": "prepay-request-controller",
      "description": "Prepay Request Controller"
    },
    {
      "name": "program-controller",
      "description": "Program Controller"
    },
    {
      "name": "program-status-controller",
      "description": "Program Status Controller"
    },
    {
      "name": "promo-stat-controller",
      "description": "Promo Stat Controller"
    },
    {
      "name": "push-ready-button-controller",
      "description": "Push Ready Button Controller"
    },
    {
      "name": "region-controller",
      "description": "Region Controller"
    },
    {
      "name": "region-delivery-group-status-controller",
      "description": "Region Delivery Group Status Controller"
    },
    {
      "name": "region-group-delivery-services-controller",
      "description": "Region Group Delivery Services Controller"
    },
    {
      "name": "sales-notes-controller",
      "description": "Sales Notes Controller"
    },
    {
      "name": "sales-report-controller",
      "description": "Sales Report Controller"
    },
    {
      "name": "shop-banner-controller",
      "description": "Shop Banner Controller"
    },
    {
      "name": "shop-delivery-services-controller",
      "description": "Shop Delivery Services Controller"
    },
    {
      "name": "shop-logo-controller",
      "description": "Shop Logo Controller"
    },
    {
      "name": "shop-placement-schedule-controller",
      "description": "Shop Placement Schedule Controller"
    },
    {
      "name": "shop-registration-controller",
      "description": "Shop Registration Controller"
    },
    {
      "name": "shop-vat-controller",
      "description": "Shop Vat Controller"
    },
    {
      "name": "shop-warehouse-controller",
      "description": "Shop Warehouse Controller"
    },
    {
      "name": "sku-movement-report-controller",
      "description": "Sku Movement Report Controller"
    },
    {
      "name": "statistics-report-controller",
      "description": "Statistics Report Controller"
    },
    {
      "name": "stocks-by-supply-report-controller",
      "description": "Stocks By Supply Report Controller"
    },
    {
      "name": "subsidies-controller",
      "description": "Subsidies Controller"
    },
    {
      "name": "supplier-application-controller",
      "description": "Supplier Application Controller"
    },
    {
      "name": "supplier-assortment-controller",
      "description": "Supplier Assortment Controller"
    },
    {
      "name": "supplier-contract-controller",
      "description": "Supplier Contract Controller"
    },
    {
      "name": "supplier-controller",
      "description": "Supplier Controller"
    },
    {
      "name": "supplier-feed-controller",
      "description": "Supplier Feed Controller"
    },
    {
      "name": "supplier-registration-controller",
      "description": "Supplier Registration Controller"
    },
    {
      "name": "supplier-suggest-controller",
      "description": "Supplier Suggest Controller"
    },
    {
      "name": "supplier-summary-controller",
      "description": "Supplier Summary Controller"
    },
    {
      "name": "supplier-validation-controller",
      "description": "Supplier Validation Controller"
    },
    {
      "name": "survey-controller",
      "description": "Survey Controller"
    },
    {
      "name": "timezone-controller",
      "description": "Timezone Controller"
    },
    {
      "name": "token-controller",
      "description": "Token Controller"
    },
    {
      "name": "uni-report-controller",
      "description": "Uni Report Controller"
    },
    {
      "name": "virtual-shop-controller",
      "description": "Virtual Shop Controller"
    },
    {
      "name": "wizard-controller",
      "description": "Wizard Controller"
    }
  ],
  "paths": {
    "/agency/reward": {
      "get": {
        "tags": [
          "agency-reward-controller"
        ],
        "summary": "Получение коэффициентов премий агенств",
        "description": "Получение коэффициентов премий агенств по агенству и кварталу. Если квартал не задан, будет использоваться текущий. Агенство определяется по uid",
        "operationId": "getAgencyRewardSummaryUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "quarter",
            "in": "query",
            "description": "Номер квартала",
            "required": false,
            "type": "integer",
            "default": -1,
            "format": "int32",
            "allowEmptyValue": false
          },
          {
            "name": "year",
            "in": "query",
            "description": "Год",
            "required": false,
            "type": "integer",
            "default": -1,
            "format": "int32",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение коэффициентов",
            "schema": {
              "$ref": "#/definitions/AgencyRewardSummaryDTO"
            }
          },
          "400": {
            "description": "Нет данных по премиям для данныого агенства и квартала",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "У агенства нет доступа к премиям"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/agency/reward/report": {
      "get": {
        "tags": [
          "agency-reward-controller"
        ],
        "summary": "Получение отчета о премиях агенства",
        "description": "Получение отчета о премиях агенства. Детализация будет по указанному кварталу. Если квартал не задан, то будет использоваться текущий. Агенство определяется по uid",
        "operationId": "getXlsReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "quarter",
            "in": "query",
            "description": "Номер квартала",
            "required": false,
            "type": "integer",
            "default": -1,
            "format": "int32",
            "allowEmptyValue": false
          },
          {
            "name": "year",
            "in": "query",
            "description": "Год",
            "required": false,
            "type": "integer",
            "default": -1,
            "format": "int32",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение отчета. Xlsx-файл",
            "schema": {
              "$ref": "#/definitions/AgencyRewardSummaryDTO"
            }
          },
          "400": {
            "description": "Нет данных по премиям для данныого агенства и квартала",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "У агенства нет доступа к премиям"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/banks/{bic}": {
      "get": {
        "tags": [
          "bank-info-controller"
        ],
        "summary": "Получение информации о действующем банке по БИК",
        "operationId": "getActiveBankByBicUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "bic",
            "in": "path",
            "description": "bic",
            "required": true,
            "type": "string"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение информации о действующем банке по БИК",
            "schema": {
              "$ref": "#/definitions/BankInfoDTO"
            }
          },
          "400": {
            "description": "В качестве параметра передан невалидный БИК",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "По заданному БИК не найден ни один действующий банк",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      }
    },
    "/cabinet/{cabinet}/config": {
      "get": {
        "tags": [
          "cabinet-controller"
        ],
        "summary": "getConfig",
        "operationId": "getConfigUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "cabinet",
            "in": "path",
            "description": "cabinet",
            "required": true,
            "type": "string"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/Cabinet"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/cabinet/{cabinet}/page": {
      "get": {
        "tags": [
          "cabinet-controller"
        ],
        "summary": "getConfig",
        "operationId": "getConfigUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "cabinet",
            "in": "path",
            "description": "cabinet",
            "required": true,
            "type": "string"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "page",
            "in": "query",
            "description": "page",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/Cabinet"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/cabinet/{cabinet}/pages": {
      "get": {
        "tags": [
          "cabinet-controller"
        ],
        "summary": "getConfig",
        "operationId": "getConfigUsingGET_2",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "cabinet",
            "in": "path",
            "description": "cabinet",
            "required": true,
            "type": "string"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/Cabinet"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/calcDeliveryCosts": {
      "post": {
        "tags": [
          "delivery-calculator-controller"
        ],
        "summary": "calcDeliveryCosts",
        "operationId": "calcDeliveryCostsUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "costRequest",
            "description": "costRequest",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DeliveryCostsRequestOld"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DeliveryCostsResponseOld"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/creditTemplate": {
      "post": {
        "tags": [
          "credit-template-controller"
        ],
        "summary": "Обновление кредитного шаблона магазина",
        "operationId": "postCreditTemplateUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "template",
            "description": "template",
            "required": true,
            "schema": {
              "$ref": "#/definitions/CreditTemplateRequestDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное обновление кредитного шаблона магазина",
            "schema": {
              "$ref": "#/definitions/CreditTemplateResponseDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "400": {
            "description": "Ошибка в заполнении полей кредитного шаблона",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Попытка обновить кредитный шаблон другого партнера",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "404": {
            "description": "Кампания не найдена",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/creditTemplate/{templateId}": {
      "delete": {
        "tags": [
          "credit-template-controller"
        ],
        "summary": "Удаление кредитного шаблона магазина",
        "operationId": "deleteCreditTemplatesUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "templateId",
            "in": "path",
            "description": "templateId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное удаление кредитного шаблона магазина"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Кредитный шаблон с запрошенным идентификатором не найден",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/creditTemplates": {
      "get": {
        "tags": [
          "credit-template-controller"
        ],
        "summary": "Получение всех кредитных шаблонов магазина",
        "operationId": "getCreditTemplatesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение всех кредитных шаблонов магазина",
            "schema": {
              "$ref": "#/definitions/CreditTemplateListDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Кампания не найдена",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/logo/delete": {
      "delete": {
        "tags": [
          "shop-logo-controller"
        ],
        "summary": "Удаление логотипа",
        "operationId": "deleteUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/logo/info": {
      "get": {
        "tags": [
          "shop-logo-controller"
        ],
        "summary": "Получение информации о загруженных логотипах",
        "operationId": "getInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopLogoInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/logo/upload": {
      "post": {
        "tags": [
          "shop-logo-controller"
        ],
        "summary": "Загрузка нового логотипа",
        "operationId": "uploadUsingPOST_1",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "file",
            "in": "formData",
            "description": "file",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopLogoInfoDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaign/{campaignId}/logo/validate": {
      "post": {
        "tags": [
          "shop-logo-controller"
        ],
        "summary": "Валидация логотипа",
        "operationId": "validateUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "file",
            "in": "formData",
            "description": "file",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopLogoInfoDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/programs": {
      "get": {
        "tags": [
          "program-controller"
        ],
        "summary": "Получение статусов по всем программам для указанного магазина.",
        "operationId": "getProgramsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/ProgramStatus"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/programs/{program}": {
      "get": {
        "tags": [
          "program-controller"
        ],
        "summary": "Получение статуса по конкретной программе для указанного магазина.",
        "operationId": "getProgramUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "is_debug",
            "in": "query",
            "description": "is_debug",
            "required": false,
            "type": "boolean",
            "default": false
          },
          {
            "name": "program",
            "in": "path",
            "description": "Тип программы",
            "required": false,
            "type": "string",
            "enum": [
              "CPC",
              "CHINA_GOODS_APP_PLACEMENT",
              "MARKETPLACE"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ProgramStatus"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/programs/{program}/disable": {
      "post": {
        "tags": [
          "program-controller"
        ],
        "summary": "Выключение программы для магазина.",
        "operationId": "disableUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "program",
            "in": "path",
            "description": "Тип программы",
            "required": false,
            "type": "string",
            "enum": [
              "CPC",
              "CHINA_GOODS_APP_PLACEMENT",
              "MARKETPLACE"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/programs/{program}/enable": {
      "post": {
        "tags": [
          "program-controller"
        ],
        "summary": "Включение программы для магазина.",
        "operationId": "enableUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "program",
            "in": "path",
            "description": "Тип программы",
            "required": false,
            "type": "string",
            "enum": [
              "CPC",
              "CHINA_GOODS_APP_PLACEMENT",
              "MARKETPLACE"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/programs/{program}/fields": {
      "get": {
        "tags": [
          "program-controller"
        ],
        "summary": "Получение данных о заполненности полей программы.",
        "operationId": "getProgramFieldsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "program",
            "in": "path",
            "description": "Тип программы",
            "required": false,
            "type": "string",
            "enum": [
              "CPC",
              "CHINA_GOODS_APP_PLACEMENT",
              "MARKETPLACE"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/ProgramField"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/steps": {
      "get": {
        "tags": [
          "wizard-controller"
        ],
        "summary": "allSteps",
        "operationId": "allStepsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/WizardStepStatus"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/steps/{step}": {
      "get": {
        "tags": [
          "wizard-controller"
        ],
        "summary": "step",
        "operationId": "stepUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "step",
            "in": "path",
            "description": "step",
            "required": true,
            "type": "string",
            "enum": [
              "LEGAL",
              "FEED",
              "DELIVERY",
              "SETTINGS",
              "CROSSBORDER_LEGAL"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/WizardStepStatus"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/commits": {
      "post": {
        "tags": [
          "assortment-commit-controller"
        ],
        "summary": "Начать коммит ассортимента",
        "operationId": "commitFeedUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "selectedValidation",
            "description": "selectedValidation",
            "required": true,
            "schema": {
              "$ref": "#/definitions/SupplierValidationSelectionDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierCommitIdDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/commits/latest": {
      "get": {
        "tags": [
          "assortment-commit-controller"
        ],
        "summary": "Информация о последнем успешно опубликованном ассортименте",
        "operationId": "getLatestAssortmentInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/commits/latest/download": {
      "get": {
        "tags": [
          "assortment-commit-controller"
        ],
        "summary": "Получить файл с последним успешно опубликованным ассортиментом",
        "operationId": "getCommitedLatestDownloadUsingGET",
        "produces": [
          "application/vnd.ms-excel"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/commits/{commitId}": {
      "get": {
        "tags": [
          "assortment-commit-controller"
        ],
        "summary": "Получить результат коммита ассортимента",
        "operationId": "getCommitResultUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "commitId",
            "in": "path",
            "description": "commitId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierCommitInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/commits/{commitId}/download": {
      "get": {
        "tags": [
          "assortment-commit-controller"
        ],
        "summary": "Получить файл обогащённый ошибками при публикации ассортимента",
        "operationId": "getCommitDownloadUsingGET",
        "produces": [
          "application/octet-stream"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "commitId",
            "in": "path",
            "description": "commitId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/validations": {
      "post": {
        "tags": [
          "assortment-validation-controller"
        ],
        "summary": "Начать валидацию ассортимента/маппинга",
        "operationId": "createValidationFromUploadUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "upload",
            "in": "formData",
            "description": "upload",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/validations/{validationId}": {
      "get": {
        "tags": [
          "assortment-validation-controller"
        ],
        "summary": "Получить текущий статус валидации ассортимента/маппинга",
        "operationId": "getValidationUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validationId",
            "in": "path",
            "description": "validationId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/assortment/validations/{validationId}/download": {
      "get": {
        "tags": [
          "assortment-validation-controller"
        ],
        "summary": "Получить результат валидации ассортимента/маппинга",
        "operationId": "getValidationDownloadUsingGET",
        "produces": [
          "application/octet-stream"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validationId",
            "in": "path",
            "description": "validationId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/feed/dynamic-price-control": {
      "get": {
        "tags": [
          "dynamic-price-control-controller"
        ],
        "summary": "Получить текущие настройки динамического управления ценами. Возвращает 404, если динамическое ценообразование выключено.",
        "operationId": "getDynamicPriceControlConfigUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DynamicPriceControlConfigDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "dynamic-price-control-controller"
        ],
        "summary": "Обновить настройки динамического управления ценами",
        "operationId": "updateDynamicPriceControlConfigUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "configView",
            "description": "configView",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DynamicPriceControlConfigDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "dynamic-price-control-controller"
        ],
        "summary": "Выключить динамическое ценообразование",
        "operationId": "disbaleDynamicPriceControlUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/market-skus/queries": {
      "post": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Запросить все привязки (к shop-sku) для переданного набора market-sku",
        "operationId": "queryUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "queryBuilder",
            "description": "queryBuilder",
            "required": true,
            "schema": {
              "$ref": "#/definitions/Builder"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ProcessedMarketSkuQuery"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/market-skus/{marketSku}/shop-skus": {
      "post": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Привязать market-sku к shop-sku",
        "operationId": "createMappingUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "marketSku",
            "in": "path",
            "description": "marketSku",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "shopSkuBuilder",
            "description": "shopSkuBuilder",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ShopSku"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopSku"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus": {
      "get": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Получить информацию об офере и его привязкам по Shop SKU",
        "operationId": "getShopSkusUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "category_id",
            "in": "query",
            "description": "category_id",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int32"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "limit",
            "in": "query",
            "description": "limit",
            "required": false,
            "type": "integer",
            "default": 100,
            "format": "int32"
          },
          {
            "name": "offer_processing_status",
            "in": "query",
            "description": "offer_processing_status",
            "required": false,
            "type": "array",
            "items": {
              "type": "string"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "page_token",
            "in": "query",
            "description": "page_token",
            "required": false,
            "type": "string"
          },
          {
            "name": "q",
            "in": "query",
            "description": "Текстовая строка поискового запроса",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopSkuPageDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Добавить/обновить shop-sku не создавая и не обновляя привязку",
        "operationId": "updateShopOfferUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "shopSkuBuilder",
            "description": "shopSkuBuilder",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ShopSku"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopSku"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku": {
      "get": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Получить информацию об офере и его привязкам по Shop SKU",
        "operationId": "getShopSkuUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "shop_sku",
            "in": "query",
            "description": "shop_sku",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/MappedShopSku"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/availability": {
      "put": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Указать признак доступности для ShopSKU",
        "operationId": "updateAvailabilityUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "availabilityRequest",
            "description": "availabilityRequest",
            "required": true,
            "schema": {
              "$ref": "#/definitions/AvailabilityUpdateDTO"
            }
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "shop_sku",
            "in": "query",
            "description": "shop_sku",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AvailabilityUpdateDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/market-sku": {
      "put": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Указать маппинг для существубщего ShopSKU, без редактирования каких-либо других полей",
        "operationId": "linkShopSkuUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "marketSku",
            "description": "marketSku",
            "required": true,
            "schema": {
              "$ref": "#/definitions/MarketSku"
            }
          },
          {
            "name": "shop_sku",
            "in": "query",
            "description": "shop_sku",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/MarketSku"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/market-skus": {
      "put": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Не используйте этот метод, в место этого используйте /campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/market-sku (без s на конце)",
        "operationId": "linkShopSkuWrongUrlUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "marketSku",
            "description": "marketSku",
            "required": true,
            "schema": {
              "$ref": "#/definitions/MarketSku"
            }
          },
          {
            "name": "shop_sku",
            "in": "query",
            "description": "shop_sku",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/MarketSku"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/offer-processing-status": {
      "put": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Изменить статус процессинга офера для ShopSKU",
        "operationId": "updateOfferProcessingStatusUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "shop_sku",
            "in": "query",
            "description": "shop_sku",
            "required": true,
            "type": "string"
          },
          {
            "in": "body",
            "name": "statusRequest",
            "description": "statusRequest",
            "required": true,
            "schema": {
              "$ref": "#/definitions/OfferProcessingStatus"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferProcessingStatus"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/categories": {
      "get": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Получить категории оферов по указанным фильтрам",
        "operationId": "getShopSkuCategoriesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "accept_good_content",
            "in": "query",
            "description": "accept_good_content",
            "required": false,
            "type": "boolean"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "category_id",
            "in": "query",
            "description": "category_id",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int32"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "offer_processing_status",
            "in": "query",
            "description": "offer_processing_status",
            "required": false,
            "type": "array",
            "items": {
              "type": "string"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "q",
            "in": "query",
            "description": "q",
            "required": false,
            "type": "string"
          },
          {
            "name": "tree",
            "in": "query",
            "description": "tree",
            "required": false,
            "type": "boolean",
            "default": true
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/CategoryDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/categories/{categoryId}/content-template": {
      "get": {
        "tags": [
          "partner-content-controller"
        ],
        "summary": "Скачать шаблон из сервиса контента",
        "operationId": "getCategoryContentTemplateUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "accept_good_content",
            "in": "query",
            "description": "accept_good_content",
            "required": false,
            "type": "boolean"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "categoryId",
            "in": "path",
            "description": "categoryId",
            "required": true,
            "type": "integer",
            "format": "int32"
          },
          {
            "name": "offer_processing_status",
            "in": "query",
            "description": "offer_processing_status",
            "required": false,
            "type": "array",
            "items": {
              "type": "string"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "q",
            "in": "query",
            "description": "q",
            "required": false,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "string",
              "format": "byte"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/offer-processing-statuses": {
      "get": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Получить статистику оферов по processing статусу",
        "operationId": "getOfferProcessingStatusStatsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "category_id",
            "in": "query",
            "description": "category_id",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int32"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "offer_processing_status",
            "in": "query",
            "description": "offer_processing_status",
            "required": false,
            "type": "array",
            "items": {
              "type": "string"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "q",
            "in": "query",
            "description": "Текстовая строка поискового запроса",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferProcessingStatusStats"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/campaigns/{campaignId}/offer-mapping/shop-skus/xls": {
      "get": {
        "tags": [
          "mapping-controller"
        ],
        "summary": "Получить информацию об офере и его привязкам по Shop SKU",
        "operationId": "getShopSkusAsXlsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "category_id",
            "in": "query",
            "description": "category_id",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int32"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "offer_processing_status",
            "in": "query",
            "description": "offer_processing_status",
            "required": false,
            "type": "array",
            "items": {
              "type": "string"
            },
            "collectionFormat": "multi"
          },
          {
            "name": "q",
            "in": "query",
            "description": "Текстовая строка поискового запроса",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/checker/result": {
      "get": {
        "tags": [
          "checker-controller"
        ],
        "summary": "calculateChecker",
        "operationId": "calculateCheckerUsingGET",
        "produces": [
          "text/plain"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "authorityName",
            "in": "query",
            "description": "authorityName",
            "required": true,
            "type": "string"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "checkerName",
            "in": "query",
            "description": "checkerName",
            "required": true,
            "type": "string"
          },
          {
            "name": "domain",
            "in": "query",
            "description": "domain",
            "required": true,
            "type": "string"
          },
          {
            "name": "param",
            "in": "query",
            "description": "param",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "string"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/client/summary": {
      "get": {
        "tags": [
          "client-summary-controller"
        ],
        "summary": "Получение сводной информации по клиенту пользоватея и его кампаниям",
        "operationId": "getClientSummaryUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "all_campaigns",
            "in": "query",
            "description": "Возвращать данные по всем кампаниям клиента пользователя, или только по тем, для которых у пользователя есть роли.",
            "required": false,
            "type": "boolean",
            "default": false,
            "allowEmptyValue": false,
            "x-example": false
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ClientSummary"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/cpcState": {
      "get": {
        "tags": [
          "cpc-state-controller"
        ],
        "summary": "Получение состояния CPC программы для магазина",
        "operationId": "getCpcStateUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CpcState"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/credits/minPriceLimit": {
      "get": {
        "tags": [
          "credit-min-price-limit-controller"
        ],
        "summary": "Получить лимит",
        "operationId": "getMinPriceLimitUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "integer",
              "format": "int32"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/application": {
      "get": {
        "tags": [
          "crossborder-partner-application-controller"
        ],
        "summary": "getApplication",
        "operationId": "getApplicationUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CrossborderPartnerApplicationDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/application/document": {
      "get": {
        "tags": [
          "crossborder-partner-application-controller"
        ],
        "summary": "getApplicationPdf",
        "operationId": "getApplicationPdfUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/application/edits": {
      "post": {
        "tags": [
          "crossborder-partner-application-controller"
        ],
        "summary": "editRegistrationRequest",
        "operationId": "editRegistrationRequestUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "edit",
            "description": "edit",
            "required": true,
            "schema": {
              "$ref": "#/definitions/CrossborderPartnerApplicationFormDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CrossborderPartnerApplicationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/application/submit": {
      "post": {
        "tags": [
          "crossborder-partner-application-controller"
        ],
        "summary": "submitApplication",
        "operationId": "submitApplicationUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CrossborderPartnerApplicationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/campaigns": {
      "get": {
        "tags": [
          "crossborder-campaign-controller"
        ],
        "summary": "Получить список кроссбордер-кампаний клиента. Данные выдаются постранично.",
        "operationId": "getPagedCampaignsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "page",
            "in": "query",
            "description": "Номер страницы, которую следует выгружать",
            "required": false,
            "type": "integer",
            "format": "int32",
            "allowEmptyValue": false
          },
          {
            "name": "perpageNumber",
            "in": "query",
            "description": "Количество элементов отображаемых на странице. Значение не может быть больше 200",
            "required": false,
            "type": "integer",
            "default": 100,
            "format": "int32",
            "allowEmptyValue": false
          },
          {
            "name": "query",
            "in": "query",
            "description": "Поисковый запрос (по имени магазина или идентификатору кампании)",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PagedCrossborderCampaignsDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/contracts/external": {
      "get": {
        "tags": [
          "crossborder-partner-contract-controller"
        ],
        "summary": "Получение внешних номеров договоров для партнеров красного маркета",
        "operationId": "getPartnerContractsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CrossborderPartnerExternalContractsDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/crossborder/payee": {
      "post": {
        "tags": [
          "crossborder-partner-application-controller"
        ],
        "summary": "Метод для интеграции партнера с платежной системой на Красном маркете. Создает или возвращает получателя платежей",
        "operationId": "getOrCreatePayeeUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "crossborderPayeeDTO",
            "description": "crossborderPayeeDTO",
            "required": true,
            "schema": {
              "$ref": "#/definitions/CrossborderPayeeDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CrossborderPayeeDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/application": {
      "get": {
        "tags": [
          "delivery-partner-application-controller"
        ],
        "summary": "getApplication",
        "operationId": "getApplicationUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DeliveryPartnerApplicationDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/application/document": {
      "get": {
        "tags": [
          "delivery-partner-application-controller"
        ],
        "summary": "getApplicationPdf",
        "operationId": "getApplicationPdfUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/application/edits": {
      "post": {
        "tags": [
          "delivery-partner-application-controller"
        ],
        "summary": "editRegistrationRequest",
        "operationId": "editRegistrationRequestUsingPOST_1",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "applicationFormDTO",
            "description": "applicationFormDTO",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DeliveryPartnerApplicationFormDTO"
            }
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DeliveryPartnerApplicationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/application/submit": {
      "post": {
        "tags": [
          "delivery-partner-application-controller"
        ],
        "summary": "submitApplication",
        "operationId": "submitApplicationUsingPOST_1",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DeliveryPartnerApplicationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/partners": {
      "get": {
        "tags": [
          "delivery-partner-controller"
        ],
        "summary": "Получить список всех магазинов клиента, подключенных к доставке",
        "operationId": "getPartnersUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "query",
            "in": "query",
            "description": "Поисковый запрос (по имени или идентификатору магазина)",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DeliveryPartnersDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/tariff/paymentTypes": {
      "get": {
        "tags": [
          "delivery-payment-controller"
        ],
        "summary": "Получение региональных типов оплат магазина",
        "operationId": "findPaymentTypesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionGroupId",
            "in": "query",
            "description": "ID группы регионов",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/RegionGroupPaymentDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "delivery-payment-controller"
        ],
        "summary": "Изменение региональных типов оплат магазина",
        "operationId": "changePaymentTypesUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "paymentTypes",
            "description": "paymentTypes",
            "required": true,
            "schema": {
              "type": "array",
              "items": {
                "type": "string",
                "enum": [
                  "COURIER_CASH",
                  "COURIER_CARD",
                  "PREPAYMENT_OTHER",
                  "PREPAYMENT_CARD"
                ]
              }
            }
          },
          {
            "name": "regionGroupId",
            "in": "query",
            "description": "ID группы регионов",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/RegionGroupPaymentDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/delivery/tariff/status": {
      "get": {
        "tags": [
          "region-delivery-group-status-controller"
        ],
        "summary": "Получение статуса проверки региональной группы",
        "operationId": "getStatusUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionGroupId",
            "in": "query",
            "description": "Идентификатор группы регионов",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/RegionGroupStatusDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/detailedCpaOrders": {
      "get": {
        "tags": [
          "detailed-cpa-orders-controller"
        ],
        "summary": "getDetailedCpaOrders",
        "operationId": "getDetailedCpaOrdersUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "days",
            "in": "query",
            "description": "days",
            "required": true,
            "type": "integer",
            "format": "int32"
          },
          {
            "name": "scale",
            "in": "query",
            "description": "scale",
            "required": false,
            "type": "string",
            "default": "HOUR",
            "enum": [
              "HOUR",
              "FOUR_HOURS",
              "DAY"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DetailedCpaOrderResponse"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/disableFeature": {
      "post": {
        "tags": [
          "feature-controller"
        ],
        "summary": "Выключение программы",
        "operationId": "disableFeatureUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "feature-id",
            "in": "query",
            "description": "Идентификатор программы",
            "required": false,
            "type": "integer",
            "format": "int32",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopFeatureInfoDto"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/dtps/data": {
      "get": {
        "tags": [
          "data-transfer-permission-storage-controller"
        ],
        "summary": "getData",
        "operationId": "getDataUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DataModel"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/enableFeature": {
      "post": {
        "tags": [
          "feature-controller"
        ],
        "summary": "Включение программы, если доступна",
        "operationId": "enableFeatureUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "feature-id",
            "in": "query",
            "description": "Идентификатор программы",
            "required": false,
            "type": "integer",
            "format": "int32",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopFeatureInfoDto"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/environment": {
      "get": {
        "tags": [
          "environment-controller"
        ],
        "summary": "Получить значения для environment переменной",
        "operationId": "getEnvironmentUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "var",
            "in": "query",
            "description": "var",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "type": "string"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/featureInfo": {
      "get": {
        "tags": [
          "feature-controller"
        ],
        "summary": "Получение текущего состояния программы",
        "operationId": "getFeatureInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "feature-id",
            "in": "query",
            "description": "Идентификатор программы",
            "required": false,
            "type": "integer",
            "format": "int32",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopFeatureInfoDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed": {
      "post": {
        "tags": [
          "feed-controller"
        ],
        "summary": "createFeed",
        "operationId": "createFeedUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validation_id",
            "in": "query",
            "description": "validation_id",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CreateFeedResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      },
      "delete": {
        "tags": [
          "feed-controller"
        ],
        "summary": "removeFeed",
        "operationId": "removeFeedUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "feed_id",
            "in": "query",
            "description": "feed_id",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2": {
      "post": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "createFeed",
        "operationId": "createFeedUsingPOST_1",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "completeFeed",
            "in": "query",
            "description": "completeFeed",
            "required": false,
            "type": "boolean",
            "default": true
          },
          {
            "name": "validation_id",
            "in": "query",
            "description": "validation_id",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CreateFeedResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2/upload": {
      "post": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "upload",
        "operationId": "uploadUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/UploadResponseDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2/validate": {
      "post": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "validateFeed",
        "operationId": "validateFeedUsingPOST_1",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "dto",
            "description": "dto",
            "required": true,
            "schema": {
              "$ref": "#/definitions/FeedValidationDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FeedValidationInfo"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2/validationParsed": {
      "get": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "getFeedValidation",
        "operationId": "getFeedValidationUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "errors_limit",
            "in": "query",
            "description": "errors_limit",
            "required": false,
            "type": "integer",
            "default": 5,
            "format": "int32"
          },
          {
            "name": "show_log",
            "in": "query",
            "description": "show_log",
            "required": false,
            "type": "boolean"
          },
          {
            "name": "validation_id",
            "in": "query",
            "description": "validation_id",
            "required": false,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FeedValidationParsedDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2/{feedId}": {
      "put": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "editFeed",
        "operationId": "editFeedUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "completeFeed",
            "in": "query",
            "description": "completeFeed",
            "required": false,
            "type": "boolean",
            "default": true
          },
          {
            "in": "body",
            "name": "editFeedDTO",
            "description": "editFeedDTO",
            "required": true,
            "schema": {
              "$ref": "#/definitions/EditFeedDTO"
            }
          },
          {
            "name": "feedId",
            "in": "path",
            "description": "feedId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2/{validationId}/csv": {
      "get": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "csvReport",
        "operationId": "csvReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validationId",
            "in": "path",
            "description": "validationId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/feed/v2/{validationId}/xls": {
      "get": {
        "tags": [
          "new-feed-controller"
        ],
        "summary": "xlsReport",
        "operationId": "xlsReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validationId",
            "in": "path",
            "description": "validationId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fmcg/application": {
      "get": {
        "tags": [
          "fmcg-partner-application-controller"
        ],
        "summary": "getApplication",
        "operationId": "getApplicationUsingGET_2",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FmcgPartnerApplicationDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fmcg/application/edits": {
      "post": {
        "tags": [
          "fmcg-partner-application-controller"
        ],
        "summary": "editRegistrationRequest",
        "operationId": "editRegistrationRequestUsingPOST_2",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "applicationFormDTO",
            "description": "applicationFormDTO",
            "required": true,
            "schema": {
              "$ref": "#/definitions/FmcgPartnerApplicationFormDTO"
            }
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FmcgPartnerApplicationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fmcg/campaigns": {
      "get": {
        "tags": [
          "fmcg-campaign-controller"
        ],
        "summary": "Получить список FMCG-кампаний клиента",
        "operationId": "getCampaignsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "query",
            "in": "query",
            "description": "Поисковый запрос (по имени магазина или идентификатору кампании)",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/FmcgCampaignDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fmcg/shopping-cart": {
      "get": {
        "tags": [
          "fmcg-info-controller"
        ],
        "summary": "getShoppingCartSettings",
        "operationId": "getShoppingCartSettingsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FmcgShoppingCartDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fmcg/shopping-cart/edits": {
      "post": {
        "tags": [
          "fmcg-info-controller"
        ],
        "summary": "editShoppingCartSettings",
        "operationId": "editShoppingCartSettingsUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "shoppingCartDTO",
            "description": "shoppingCartDTO",
            "required": true,
            "schema": {
              "$ref": "#/definitions/FmcgShoppingCartDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/orders/virtual-shop/xls": {
      "get": {
        "tags": [
          "fulfillment-report-controller"
        ],
        "summary": "getVirtualShopOrdersXlsReport",
        "operationId": "getVirtualShopOrdersXlsReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "ffShopId",
            "in": "query",
            "description": "ffShopId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/orders/xls": {
      "get": {
        "tags": [
          "fulfillment-report-controller"
        ],
        "summary": "Получение отчета по заказам с поофферной детализацией для синего или красного магазина.",
        "operationId": "getFfOrdersXlsReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "Нижняя граница для данных в отчете.",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "Верхняя граница для данных в отчете.",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/sales/supplier": {
      "get": {
        "tags": [
          "sales-report-controller"
        ],
        "summary": "getSalesSupplierReport",
        "operationId": "getSalesSupplierReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/sales/virtual": {
      "get": {
        "tags": [
          "sales-report-controller"
        ],
        "summary": "getSalesVirtualShopReport",
        "operationId": "getSalesVirtualShopReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "supplierId",
            "in": "query",
            "description": "supplierId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/sku-movements": {
      "get": {
        "tags": [
          "sku-movement-report-controller"
        ],
        "summary": "Получить отчет по движениям",
        "operationId": "generateSkuMovementReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "Нижняя граница для данных в отчете",
            "required": false,
            "type": "string",
            "format": "date",
            "allowEmptyValue": false
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "Верхняя граница для данных в отчете",
            "required": false,
            "type": "string",
            "format": "date",
            "allowEmptyValue": false
          },
          {
            "name": "shopSku",
            "in": "query",
            "description": "Индетификатор офера поставщика",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "Ндентификатор склада",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/stocks/current/supplier": {
      "get": {
        "tags": [
          "current-stocks-report-controller"
        ],
        "summary": "generateStocksDailyReport",
        "operationId": "generateStocksDailyReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "warehouseId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/stocks/daily/supplier": {
      "get": {
        "tags": [
          "daily-stocks-report-controller"
        ],
        "summary": "generateStocksDailyReport",
        "operationId": "generateStocksDailyReportUsingGET_1",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "warehouseId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/stocks/daily/supplier/aggregated": {
      "get": {
        "tags": [
          "daily-stocks-report-controller"
        ],
        "summary": "generateGroupedStocksDailyReport",
        "operationId": "generateGroupedStocksDailyReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "warehouseId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/stocks/daily/virtual": {
      "get": {
        "tags": [
          "daily-stocks-report-controller"
        ],
        "summary": "generateVirtualStocksDailyReport",
        "operationId": "generateVirtualStocksDailyReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "supplierId",
            "in": "query",
            "description": "supplierId",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "warehouseId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/stocks/daily/virtual/aggregated": {
      "get": {
        "tags": [
          "daily-stocks-report-controller"
        ],
        "summary": "generateGroupedVirtualStocksDailyReportBySupplier",
        "operationId": "generateGroupedVirtualStocksDailyReportBySupplierUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "warehouseId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/stocks/daily/virtual/grouped": {
      "get": {
        "tags": [
          "daily-stocks-report-controller"
        ],
        "summary": "generateGroupedVirtualStocksDailyReport",
        "operationId": "generateGroupedVirtualStocksDailyReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "query",
            "description": "warehouseId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/reports/storage-billing/stocks-by-supply": {
      "get": {
        "tags": [
          "stocks-by-supply-report-controller"
        ],
        "summary": "Получить отчёт остатков по поставкам",
        "operationId": "getStocksBySupplyReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/services": {
      "get": {
        "tags": [
          "fulfillment-service-controller"
        ],
        "summary": "getAll",
        "operationId": "getAllUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/FulfillmentServiceDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/fulfillment/services/available": {
      "get": {
        "tags": [
          "fulfillment-service-controller"
        ],
        "summary": "getAvailable",
        "operationId": "getAvailableUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/FulfillmentServiceDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/getShopBanners": {
      "get": {
        "tags": [
          "shop-banner-controller"
        ],
        "summary": "getShopBanners",
        "operationId": "getShopBannersUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/BannerListWrapper"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/getShopVAT": {
      "get": {
        "tags": [
          "shop-vat-controller"
        ],
        "summary": "Метод получения информации о налогообложении магазина",
        "operationId": "getShopVatUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopVat"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/guaranteeLetter": {
      "get": {
        "tags": [
          "guarantee-letter-controller"
        ],
        "summary": "Получить информацию про гарантийное письмо магазина",
        "operationId": "getGuaranteeLetterInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное выполнение запроса",
            "schema": {
              "$ref": "#/definitions/GuaranteeLetterInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Гарантийное письмо магазина не найдено",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "guarantee-letter-controller"
        ],
        "summary": "Загрузить гарантийное письмо магазина",
        "description": "Принимаются файлы с расширениями .jpeg, .jpg, .tiff, .png, .pdf",
        "operationId": "uploadGuaranteeLetterUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "file",
            "in": "formData",
            "description": "file",
            "required": false,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешная загрузка гарантийного письма"
          },
          "201": {
            "description": "Created"
          },
          "400": {
            "description": "Неверный формат имени файла или файл не представлен",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Кампания не найдена",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "guarantee-letter-controller"
        ],
        "summary": "Удалить гарантийное письмо магазина",
        "operationId": "deleteGuaranteeLetterUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное удаление гарантийного письма"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Гарантийное письмо магазина не найдено",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить список скрытых офферов, удовлетворяющих переданным критериям.",
        "operationId": "getHiddenOfferDtoListUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "categoryId",
            "in": "query",
            "description": "Список id маркетных категорий",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            },
            "collectionFormat": "multi",
            "allowEmptyValue": false
          },
          {
            "name": "count",
            "in": "query",
            "description": "Количесвто возвращаемых в ответе офферов",
            "required": false,
            "type": "integer",
            "format": "int32",
            "allowEmptyValue": false
          },
          {
            "name": "from",
            "in": "query",
            "description": "Оффсет для пейджинации",
            "required": false,
            "type": "integer",
            "format": "int32",
            "allowEmptyValue": false
          },
          {
            "name": "reason",
            "in": "query",
            "description": "Причина скрытия",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "text",
            "in": "query",
            "description": "Строка для поиска по наименованию оффера или id оффера в фиде магазина",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/HiddenOffersDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/categories": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить список маркетных категорий, удовлетворяющих критериям поиска, и которые есть у магазина",
        "operationId": "getHidingDetailsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "reason",
            "in": "query",
            "description": "Причина скрытия",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "text",
            "in": "query",
            "description": "Cтрока для поиска в названии категории на вхождение",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/CategoryDto"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/hidden-offers/categories-v2": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить список маркетных категорий, удовлетворяющих критериям поиска, и которые есть у магазина",
        "operationId": "getHidingCategoriesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "reason",
            "in": "query",
            "description": "Причина скрытия",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "text",
            "in": "query",
            "description": "Cтрока для поиска в названии категории на вхождение",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/CategoryDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/csv": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить отчет по скрытым офферам в формате csv",
        "operationId": "csvUsingGET",
        "produces": [
          "text/csv"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "categoryId",
            "in": "query",
            "description": "Идентификаторы маркетных категорий",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            },
            "collectionFormat": "multi",
            "allowEmptyValue": false
          },
          {
            "name": "reason",
            "in": "query",
            "description": "Причина скрытия",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "text",
            "in": "query",
            "description": "Наименование оффера или id оффера в фиде магазина",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/details/abo": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить детали от АБО по сокрытию оффера.",
        "operationId": "getAboHidingDetailsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "query",
            "description": "Id оффера",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AboHidingDetailsDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/details/indexer/{offerId}": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить детали по сокрытию оффера от индексатора.",
        "operationId": "getIndexerHidingDetailsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "path",
            "description": "Id оффера",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/IndexerHidingDetailsDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/details/partner_api": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить детали по сокрытию оффера через ПАПИ.",
        "operationId": "getPartnerApiHidingDetailsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "cmId",
            "in": "query",
            "description": "Classifier magic id",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "hidingTimestamp",
            "in": "query",
            "description": "Время скрытия",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          },
          {
            "name": "shopSku",
            "in": "query",
            "description": "Магазинный sku",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PapiHidingDetailsDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/reasons": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить список причин сокрытия, которые есть у магазина",
        "operationId": "getHidingDetailsUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "type": "string"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/hidden-offers/xls": {
      "get": {
        "tags": [
          "hidden-offer-controller"
        ],
        "summary": "Получить отчет по скрытым офферам в формате Excel",
        "operationId": "xlsUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "categoryId",
            "in": "query",
            "description": "Идентификаторы маркетных категорий",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            },
            "collectionFormat": "multi",
            "allowEmptyValue": false
          },
          {
            "name": "reason",
            "in": "query",
            "description": "Причина скрытия",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "text",
            "in": "query",
            "description": "Наименование оффера или id оффера в фиде магазина",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/intakeCost": {
      "get": {
        "tags": [
          "intake-cost-controller"
        ],
        "summary": "intakeCost",
        "operationId": "intakeCostUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "deliveryServiceId",
            "in": "query",
            "description": "deliveryServiceId",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "depth",
            "in": "query",
            "description": "depth",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "height",
            "in": "query",
            "description": "height",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "shipmentId",
            "in": "query",
            "description": "shipmentId",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "weight",
            "in": "query",
            "description": "weight",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "test-require",
            "in": "query",
            "description": "type changing",
            "required": false,
            "type": "integer",
            "format": "iny64"
        },
          {
            "name": "width",
            "in": "query",
            "description": "width",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/IntakeCost"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/marketDeliveryServices": {
      "get": {
        "tags": [
          "market-delivery-controller"
        ],
        "summary": "getMarketDeliveryServices",
        "operationId": "getMarketDeliveryServicesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "deliveryServices",
            "in": "query",
            "description": "deliveryServices",
            "required": false,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/DeliveryServiceInfoShort"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/metrika/counter/checkout": {
      "get": {
        "tags": [
          "metrika-counter-controller"
        ],
        "summary": "getCounter",
        "operationId": "getCounterUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/Goal"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "metrika-counter-controller"
        ],
        "summary": "saveCounter",
        "operationId": "saveCounterUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "goal",
            "description": "goal",
            "required": true,
            "schema": {
              "$ref": "#/definitions/Goal"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "metrika-counter-controller"
        ],
        "summary": "deleteCounter",
        "operationId": "deleteCounterUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/metrika/counters/checkout": {
      "get": {
        "tags": [
          "metrika-counters-controller"
        ],
        "summary": "getCounters",
        "operationId": "getCountersUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/Goal"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "metrika-counters-controller"
        ],
        "summary": "saveCounters",
        "operationId": "saveCountersUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "goal",
            "description": "goal",
            "required": true,
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/Goal"
              }
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "metrika-counters-controller"
        ],
        "summary": "deleteCounter",
        "operationId": "deleteCounterUsingDELETE_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "id",
            "in": "query",
            "description": "id",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/moderationRequestState": {
      "get": {
        "tags": [
          "moderation-controller"
        ],
        "summary": "getModerationRequestState",
        "operationId": "getModerationRequestStateUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ModerationRequestState"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/organizationInfo": {
      "get": {
        "tags": [
          "organization-info-controller"
        ],
        "summary": "Получение информации об организаци",
        "operationId": "getOrganizationInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OrganizationInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "organization-info-controller"
        ],
        "summary": "Редактирование информации об организаци",
        "operationId": "editOrganizationInfoUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "Юридическая информация",
            "description": "organizationInfoDTO",
            "required": false,
            "schema": {
              "$ref": "#/definitions/OrganizationInfo"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OrganizationInfo"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/organizationInfo/spark": {
      "get": {
        "tags": [
          "organization-info-controller"
        ],
        "summary": "Получение юридической информации из СПАРК, через ABO",
        "operationId": "getSparkInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "registrationNumber",
            "in": "query",
            "description": "Регистрационный номер",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "type",
            "in": "query",
            "description": "Тип организации",
            "required": false,
            "type": "string",
            "allowEmptyValue": false,
            "enum": [
              "NONE",
              "OOO",
              "ZAO",
              "IP",
              "CHP",
              "OTHER",
              "OAO"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SparkInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/organizationInfo/types": {
      "get": {
        "tags": [
          "organization-info-controller"
        ],
        "summary": "Получение возможных типов организаций",
        "operationId": "getTypesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "region",
            "in": "query",
            "description": "Если указан параметр region, доступные типы организаций выбираются только в рамках указанного региона. Если параметр region не указан, ручка отдает все известные типы организаций",
            "required": false,
            "type": "integer",
            "format": "int64",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OrganizationInfoTypesDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/pagematch": {
      "get": {
        "tags": [
          "pagematch-controller"
        ],
        "summary": "pagematch",
        "operationId": "pagematchUsingGET",
        "produces": [
          "text/plain"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/partner/application/{requestId}/documents": {
      "post": {
        "tags": [
          "partner-application-controller"
        ],
        "summary": "uploadDocument",
        "operationId": "uploadDocumentUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "document",
            "in": "formData",
            "description": "document",
            "required": true,
            "type": "file"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "type",
            "in": "query",
            "description": "type",
            "required": true,
            "type": "string",
            "enum": [
              "OTHER",
              "SIGNED_APP_FORM",
              "SIGNATORY_DOC",
              "SIGNED_APP_PROGRAMS_UPDATE",
              "CERTIFICATE_OF_INCORPORATION",
              "COMPANY_LICENCE",
              "TAX_REGISTRATION",
              "ID"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PartnerDocumentDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/partner/application/{requestId}/documents/{docId}/url": {
      "get": {
        "tags": [
          "partner-application-controller"
        ],
        "summary": "getDocumentUrlWithSecurityCheck",
        "operationId": "getDocumentUrlWithSecurityCheckUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "docId",
            "in": "path",
            "description": "docId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PartnerDocumentUrlDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/ping": {
      "get": {
        "tags": [
          "graceful-shutdown-ping-controller"
        ],
        "summary": "ping",
        "operationId": "pingUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "string"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/premoderation/state": {
      "get": {
        "tags": [
          "premoderation-state-controller"
        ],
        "summary": "Получить краткую информацию о состоянии модераций программ магазина ",
        "operationId": "getPremoderationStateUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PremoderationState"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/prepay-request": {
      "get": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "getPrepayInfoByShop",
        "operationId": "getPrepayInfoByShopUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PrepaymentShopInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "createNewPrepayRequest",
        "operationId": "createNewPrepayRequestUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "form",
            "description": "form",
            "required": true,
            "schema": {
              "$ref": "#/definitions/PrepayRequestForm"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PrepayRequestDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/prepay-request/{requestId}": {
      "put": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "updatePrepayRequest",
        "operationId": "updatePrepayRequestUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "form",
            "description": "form",
            "required": true,
            "schema": {
              "$ref": "#/definitions/PrepayRequestForm"
            }
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PrepayRequestDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/prepay-request/{requestId}/application-form": {
      "get": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "getApplicationForm",
        "operationId": "getApplicationFormUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "type",
            "in": "query",
            "description": "type",
            "required": false,
            "type": "string",
            "enum": [
              "OTHER",
              "SIGNED_APP_FORM",
              "SIGNATORY_DOC",
              "SIGNED_APP_PROGRAMS_UPDATE",
              "CERTIFICATE_OF_INCORPORATION",
              "COMPANY_LICENCE",
              "TAX_REGISTRATION",
              "ID"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/prepay-request/{requestId}/document": {
      "post": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "uploadDocument",
        "operationId": "uploadDocumentUsingPOST_1",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "file",
            "in": "query",
            "required": false,
            "type": "file"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "type",
            "in": "query",
            "required": false,
            "type": "string",
            "enum": [
              "OTHER",
              "SIGNED_APP_FORM",
              "SIGNATORY_DOC",
              "SIGNED_APP_PROGRAMS_UPDATE",
              "CERTIFICATE_OF_INCORPORATION",
              "COMPANY_LICENCE",
              "TAX_REGISTRATION",
              "ID"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PrepayRequestDocumentDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/prepay-request/{requestId}/document/{documentId}": {
      "get": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "getDocument",
        "operationId": "getDocumentUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "documentId",
            "in": "path",
            "description": "documentId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/PrepayRequestDocumentDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "deleteDocument",
        "operationId": "deleteDocumentUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "documentId",
            "in": "path",
            "description": "documentId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/prepay-request/{requestId}/status": {
      "put": {
        "tags": [
          "prepay-request-controller"
        ],
        "summary": "updateRequestStatus",
        "operationId": "updateRequestStatusUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "requestId",
            "in": "path",
            "description": "requestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "status",
            "in": "query",
            "description": "status",
            "required": true,
            "type": "string",
            "enum": [
              "INIT",
              "IN_PROGRESS",
              "COMPLETED",
              "FROZEN",
              "CLOSED",
              "DECLINED",
              "INTERNAL_CLOSED",
              "NEW",
              "NEED_INFO",
              "CANCELLED",
              "NEW_PROGRAMS_VERIFICATION_REQUIRED",
              "NEW_PROGRAMS_VERIFICATION_FAILED"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/program/status": {
      "get": {
        "tags": [
          "program-status-controller"
        ],
        "summary": "getProgramStatuses",
        "operationId": "getProgramStatusesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ProgramStatusResponseDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/promoCpcClicksReport": {
      "get": {
        "tags": [
          "promo-stat-controller"
        ],
        "summary": "promoCpcClicksReport",
        "operationId": "promoCpcClicksReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "from-date",
            "in": "query",
            "description": "from-date",
            "required": false,
            "type": "string",
            "format": "date-time"
          },
          {
            "name": "group-type",
            "in": "query",
            "description": "group-type",
            "required": false,
            "type": "string",
            "default": "DAY",
            "enum": [
              "DAY",
              "WEEK",
              "MONTH"
            ]
          },
          {
            "name": "order-asc",
            "in": "query",
            "description": "order-asc",
            "required": false,
            "type": "boolean",
            "default": true
          },
          {
            "name": "to-date",
            "in": "query",
            "description": "to-date",
            "required": false,
            "type": "string",
            "format": "date-time"
          },
          {
            "name": "version",
            "in": "query",
            "description": "version",
            "required": false,
            "type": "integer",
            "default": 0,
            "format": "int32"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/Report"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/pushReadyForTesting": {
      "get": {
        "tags": [
          "push-ready-button-controller"
        ],
        "summary": "Обработать нажатие кпонки отправиться на модерацию",
        "operationId": "startModerationUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "type": "object"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/region-group/all-delivery-services": {
      "get": {
        "tags": [
          "region-group-delivery-services-controller"
        ],
        "summary": "Получить список СД",
        "description": "Получить список существующих служб доставки. Роли: AGENCY, PARTNER_READER, SHOP_EVERYONE",
        "operationId": "getAllDeliveryServicesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение списка служб доставки",
            "schema": {
              "$ref": "#/definitions/DeliveryServiceResponseDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/region-group/{regionGroupId}/delivery-service": {
      "get": {
        "tags": [
          "region-group-delivery-services-controller"
        ],
        "summary": "Получить список настроенных СД",
        "description": "Получить список служб доставки, который настроены в региональной группе. Роли: AGENCY, PARTNER_READER, SHOP_EVERYONE",
        "operationId": "getSelectedDeliveryServicesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionGroupId",
            "in": "path",
            "description": "regionGroupId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение списка служб доставки",
            "schema": {
              "$ref": "#/definitions/SelectedDeliveryServiceResponseDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/region-group/{regionGroupId}/delivery-service/{deliveryServiceId}": {
      "post": {
        "tags": [
          "region-group-delivery-services-controller"
        ],
        "summary": "Сохранить настройки СД",
        "description": "Сохранить настройки службы доставки в региональной группе. Роли: MODIFY_REGION_GROUP",
        "operationId": "selectDeliveryServiceUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "data",
            "description": "data",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DeliveryServiceRequestDTO"
            }
          },
          {
            "name": "deliveryServiceId",
            "in": "path",
            "description": "deliveryServiceId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionGroupId",
            "in": "path",
            "description": "regionGroupId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное сохранение"
          },
          "201": {
            "description": "Created"
          },
          "400": {
            "description": "Ошибка сохранения",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "region-group-delivery-services-controller"
        ],
        "summary": "Удаление настройки СД",
        "description": "Удаление настройки службы доставки в региональной группе. Роли: MODIFY_REGION_GROUP",
        "operationId": "deleteSelectedDeliveryServiceUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "deliveryServiceId",
            "in": "path",
            "description": "deliveryServiceId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionGroupId",
            "in": "path",
            "description": "regionGroupId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное удаление"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/region/parents": {
      "get": {
        "tags": [
          "region-controller"
        ],
        "summary": "Получить список всех регионов (текущие + родительские) по указанному списку регионов",
        "operationId": "findParentsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionIds",
            "in": "query",
            "description": "Список идентификаторов регионов",
            "required": true,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            },
            "collectionFormat": "multi",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/RegionDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/region/{regionId}/timezone": {
      "get": {
        "tags": [
          "region-controller"
        ],
        "summary": "getRegionTimezone",
        "operationId": "getRegionTimezoneUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "regionId",
            "in": "path",
            "description": "regionId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/TimezoneDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/register-shop": {
      "post": {
        "tags": [
          "shop-registration-controller"
        ],
        "summary": "register",
        "operationId": "registerUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "dto",
            "description": "dto",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ShopRegistrationDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopRegistrationResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/register-shop/is-valid-domain": {
      "get": {
        "tags": [
          "shop-registration-controller"
        ],
        "summary": "isValidDomain",
        "operationId": "isValidDomainUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "domain",
            "in": "query",
            "description": "domain",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "boolean"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/reports/billing/orders/supplier": {
      "get": {
        "tags": [
          "orders-billing-report-controller"
        ],
        "summary": "generateStocksDailyReport",
        "operationId": "generateStocksDailyReportUsingGET_2",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "billingServiceType",
            "in": "query",
            "description": "billingServiceType",
            "required": false,
            "type": "string",
            "enum": [
              "FEE",
              "FEE_CANCELLATION",
              "FEE_CORRECTION",
              "FF_PROCESSING",
              "FF_PROCESSING_CORRECTION",
              "FF_STORAGE",
              "FF_CUSTOMER_RETURN",
              "FF_UNDELIVERED_RETURN",
              "FF_WITHDRAW",
              "FF_STORAGE_BILLING",
              "AGENCY_COMMISSION",
              "CROSSBORDER_FEE",
              "CROSSBORDER_FEE_CORRECTION",
              "CROSSBORDER_PAID_DELIVERY",
              "CROSSBORDER_PAID_DELIVERY_CORRECTION",
              "CROSSBORDER_EXTRA_CHARGE",
              "CROSSBORDER_EXTRA_CHARGE_CORRECTION",
              "CROSSBORDER_DELIVERY",
              "CROSSBORDER_BEST_PRICE_RECOMMENDATION_SERVICE",
              "CROSSBORDER_BEST_PRICE_RECOMMENDATION_SERVICE_CORRECTION"
            ]
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "string",
            "format": "date"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "string",
            "format": "date"
          },
          {
            "name": "orderId",
            "in": "query",
            "description": "orderId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/reports/billing/orders/virtual": {
      "get": {
        "tags": [
          "orders-billing-report-controller"
        ],
        "summary": "generateFfBillingReport",
        "operationId": "generateFfBillingReportUsingGET",
        "produces": [
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "billingServiceType",
            "in": "query",
            "description": "billingServiceType",
            "required": false,
            "type": "string",
            "enum": [
              "FEE",
              "FEE_CANCELLATION",
              "FEE_CORRECTION",
              "FF_PROCESSING",
              "FF_PROCESSING_CORRECTION",
              "FF_STORAGE",
              "FF_CUSTOMER_RETURN",
              "FF_UNDELIVERED_RETURN",
              "FF_WITHDRAW",
              "FF_STORAGE_BILLING",
              "AGENCY_COMMISSION",
              "CROSSBORDER_FEE",
              "CROSSBORDER_FEE_CORRECTION",
              "CROSSBORDER_PAID_DELIVERY",
              "CROSSBORDER_PAID_DELIVERY_CORRECTION",
              "CROSSBORDER_EXTRA_CHARGE",
              "CROSSBORDER_EXTRA_CHARGE_CORRECTION",
              "CROSSBORDER_DELIVERY",
              "CROSSBORDER_BEST_PRICE_RECOMMENDATION_SERVICE",
              "CROSSBORDER_BEST_PRICE_RECOMMENDATION_SERVICE_CORRECTION"
            ]
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "dateFrom",
            "in": "query",
            "description": "dateFrom",
            "required": true,
            "type": "string",
            "format": "date"
          },
          {
            "name": "dateTo",
            "in": "query",
            "description": "dateTo",
            "required": true,
            "type": "string",
            "format": "date"
          },
          {
            "name": "orderId",
            "in": "query",
            "description": "orderId",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "supplierId",
            "in": "query",
            "description": "supplierId",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/schedule/phone": {
      "get": {
        "tags": [
          "phone-visibility-schedule-controller"
        ],
        "summary": "Получить недельное расписание показа телефона магазина",
        "description": "Получить недельное расписание показа телефона магазина. Если у магазина явно не задано расписание, вернется дефолтное. Расписание сконвертировано в таймзону, которая указана в ответе. Это будет либо пользовательская таймзона (в которой он задавал расписание), либо московская",
        "operationId": "getPhoneScheduleUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение расписания",
            "schema": {
              "$ref": "#/definitions/ScheduleResponseDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "phone-visibility-schedule-controller"
        ],
        "summary": "Изменить недельное расписания показа телефона магазина",
        "description": "Расписание передается в таймзоне, которую выбрал пользователь",
        "operationId": "updatePhoneScheduleUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "dto",
            "description": "dto",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ScheduleUpdateDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное обновление расписания"
          },
          "201": {
            "description": "Created"
          },
          "400": {
            "description": "Ошибка установки расписания",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/schedule/placement": {
      "get": {
        "tags": [
          "shop-placement-schedule-controller"
        ],
        "summary": "Получить недельное расписание размещения магазина",
        "description": "Получить расписание размещения магазина. Если магазин отключен, расписание будет пустым. Если расписание не задано, вернется дефолтное расписание. Расписание сконвертировано в таймзону, которая указана в ответе. Это будет либо пользовательская таймзона (в которой он задавал расписание), либо московская",
        "operationId": "getPlacementScheduleUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное получение расписания",
            "schema": {
              "$ref": "#/definitions/ScheduleResponseDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "shop-placement-schedule-controller"
        ],
        "summary": "Изменить недельное расписание размещения магазина",
        "description": "Расписание передается в таймзоне, которую выбрал пользователь.",
        "operationId": "updatePlacementScheduleUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "dto",
            "description": "dto",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ScheduleUpdateDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное обновление расписания"
          },
          "201": {
            "description": "Created"
          },
          "400": {
            "description": "Ошибка установки расписания",
            "schema": {
              "$ref": "#/definitions/PartnerErrorInfo"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/schedule/placement/reset": {
      "post": {
        "tags": [
          "shop-placement-schedule-controller"
        ],
        "summary": "Сбросить расписание размещения магазина",
        "description": "У магазина будет удалено пользовательское расписание. Для размещения будет использоваться дефолтное. Закрывается катофф PARTNER",
        "operationId": "resetPlacementScheduleUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешный сброс расписания"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/schedule/placement/switch-off": {
      "post": {
        "tags": [
          "shop-placement-schedule-controller"
        ],
        "summary": "Отключить размещение магазина",
        "description": "Удаляется пользователькое расписание размещения магазина. Открывается катофф PARTNER",
        "operationId": "switchOffPlacementUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "Успешное отключение размещения"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/setShopVAT": {
      "post": {
        "tags": [
          "shop-vat-controller"
        ],
        "summary": "Метод позволяет сохранить информацию о налогообложении магазина",
        "operationId": "setShopVatUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "shopVatRequest",
            "description": "shopVatRequest",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ShopVatForm"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/ShopVat"
              }
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shop/internalName": {
      "post": {
        "tags": [
          "datasource-controller"
        ],
        "summary": "changeInternalName",
        "operationId": "changeInternalNameUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "datasource",
            "description": "datasource",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DatasourceNameDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DatasourceNameDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shopDeliveryCosts": {
      "post": {
        "tags": [
          "delivery-calculator-controller"
        ],
        "summary": "shopDeliveryCosts",
        "operationId": "shopDeliveryCostsUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "costRequest",
            "description": "costRequest",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DeliveryCostsRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DeliveryCostsResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shopDeliveryServices": {
      "get": {
        "tags": [
          "shop-delivery-services-controller"
        ],
        "summary": "getShopDeliveryServices",
        "operationId": "getShopDeliveryServicesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/DeliveryServiceResponseDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shopMarketDeliveryConditions": {
      "get": {
        "tags": [
          "market-delivery-controller"
        ],
        "summary": "getMarketDeliveryConditions",
        "operationId": "getMarketDeliveryConditionsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "shopId",
            "in": "query",
            "description": "shopId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopMarketDeliveryConditionsDto"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shopMarketDeliveryServices": {
      "get": {
        "tags": [
          "market-delivery-controller"
        ],
        "summary": "getMarketDeliveryServices",
        "operationId": "getMarketDeliveryServicesUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "deliveryServices",
            "in": "query",
            "description": "deliveryServices",
            "required": false,
            "type": "string"
          },
          {
            "name": "shopId",
            "in": "query",
            "description": "shopId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/ShopMarketDeliveryService"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shopWarehouses": {
      "get": {
        "tags": [
          "shop-warehouse-controller"
        ],
        "summary": "Получить склады магазина",
        "operationId": "getShopWarehousesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/ShopWarehouse"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "shop-warehouse-controller"
        ],
        "summary": "Создать склад магазина",
        "operationId": "putShopWarehouseUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "shopWarehouse",
            "description": "Склад магазина для заборов",
            "required": false,
            "schema": {
              "$ref": "#/definitions/ShopWarehouse"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopWarehouse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shopWarehouses/{warehouseId}": {
      "get": {
        "tags": [
          "shop-warehouse-controller"
        ],
        "summary": "Получить склад магазина по id",
        "operationId": "getShopWarehouseUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "warehouseId",
            "in": "path",
            "description": "Идентификатор склада магазина",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopWarehouse"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shops/sales-notes/{partnerId}": {
      "get": {
        "tags": [
          "sales-notes-controller"
        ],
        "summary": "getShopSalesNotes",
        "operationId": "getShopSalesNotesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "partnerId",
            "in": "path",
            "description": "partnerId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SalesNotesDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shops/sales-notes/{partnerId}/set-order-min-cost": {
      "put": {
        "tags": [
          "sales-notes-controller"
        ],
        "summary": "setOrderMinCost",
        "operationId": "setOrderMinCostUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "partnerId",
            "in": "path",
            "description": "partnerId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "value",
            "in": "query",
            "description": "value",
            "required": true,
            "type": "integer",
            "format": "int32"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shops/sales-notes/{partnerId}/toggle-order-min-cost": {
      "put": {
        "tags": [
          "sales-notes-controller"
        ],
        "summary": "toggleOrderMinCost",
        "operationId": "toggleOrderMinCostUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "partnerId",
            "in": "path",
            "description": "partnerId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "status",
            "in": "query",
            "description": "status",
            "required": true,
            "type": "boolean"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/shutdown": {
      "get": {
        "tags": [
          "graceful-shutdown-ping-controller"
        ],
        "summary": "shutdown",
        "operationId": "shutdownUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/statistics-report/months": {
      "get": {
        "tags": [
          "statistics-report-controller"
        ],
        "summary": "Получить периоды, для которых создан отчет",
        "operationId": "getMonthsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/YearMonth"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/statistics-report/url": {
      "get": {
        "tags": [
          "statistics-report-controller"
        ],
        "summary": "Получить URL отчета для запрашиваемого периода",
        "operationId": "getReportUrlUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "month",
            "in": "query",
            "description": "month",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "string"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/subsidies/contract": {
      "get": {
        "tags": [
          "subsidies-controller"
        ],
        "summary": "getSubsidiesContract",
        "operationId": "getSubsidiesContractUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SubsidiesContract"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers": {
      "post": {
        "tags": [
          "supplier-registration-controller"
        ],
        "summary": "Регистрация поставщика",
        "operationId": "registerSupplierUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "form",
            "description": "form",
            "required": true,
            "schema": {
              "$ref": "#/definitions/SupplierRegistrationDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierStateDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/applications": {
      "get": {
        "tags": [
          "supplier-registration-controller"
        ],
        "summary": "Получение всех заявок клиента",
        "operationId": "getClientApplicationsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ClientApplications"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/fulfillment": {
      "get": {
        "tags": [
          "supplier-controller"
        ],
        "summary": "Получить список поставщиков и магазинов с подключенной фичей FULFILLMENT, название которых содержит searchString, отсортированных по имени.",
        "operationId": "findFulfillmentSuppliersUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "searchString",
            "in": "query",
            "description": "searchString",
            "required": false,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/SupplierInfoDto"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/full-info": {
      "get": {
        "tags": [
          "supplier-controller"
        ],
        "summary": "getSuppliersFullInfo",
        "operationId": "getSuppliersFullInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "query",
            "in": "query",
            "description": "query",
            "required": false,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SuppliersDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/application": {
      "get": {
        "tags": [
          "supplier-application-controller"
        ],
        "summary": "Получение статуса заявки на подключение к синему маркету",
        "operationId": "getBlankRegistrationRequestUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierApplicationDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/application/edits": {
      "post": {
        "tags": [
          "supplier-application-controller"
        ],
        "summary": "Изменение полей заявки на подключение к синему маркету",
        "operationId": "editRegistrationRequestUsingPOST_3",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "edit",
            "description": "edit",
            "required": true,
            "schema": {
              "$ref": "#/definitions/SupplierApplicationDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/application/status": {
      "put": {
        "tags": [
          "supplier-application-controller"
        ],
        "summary": "Изменение статуса заявки на подключение к синему маркету",
        "operationId": "updateRegistrationRequestStatusUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "statusUpdate",
            "description": "statusUpdate",
            "required": true,
            "schema": {
              "$ref": "#/definitions/SupplierApplicationStatusDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/application/unsigned": {
      "get": {
        "tags": [
          "supplier-application-controller"
        ],
        "summary": "Получение неподписанной заявки в виде PDF",
        "operationId": "getUnsignedApplicationUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/assortment/commits": {
      "post": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте POST /campaigns/{campaignId}/assortment/commits",
        "operationId": "commitFeedUsingPOST_1",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "selectedValidation",
            "description": "selectedValidation",
            "required": true,
            "schema": {
              "$ref": "#/definitions/SupplierValidationSelectionDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierCommitIdDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/assortment/commits/latest/download": {
      "get": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте GET /campaigns/{campaignId}/assortment/commits/latest/download",
        "operationId": "getCommitedLatestDownloadUsingGET_1",
        "produces": [
          "application/vnd.ms-excel"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/assortment/commits/{commitId}": {
      "get": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте GET /campaigns/{campaignId}/assortment/commits/{commitId}",
        "operationId": "getCommitResultUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "commitId",
            "in": "path",
            "description": "commitId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierCommitInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/assortment/commits/{commitId}/download": {
      "get": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте GET /campaigns/{campaignId}/assortment/commits/{commitId}/download",
        "operationId": "getCommitDownloadUsingGET_1",
        "produces": [
          "application/octet-stream"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "commitId",
            "in": "path",
            "description": "commitId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/assortment/download": {
      "get": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "getCommitedAssortment",
        "operationId": "getCommitedAssortmentUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/assortment/validations": {
      "post": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте POST /campaigns/{campaignId}/assortment/validations",
        "operationId": "createValidationFromUploadUsingPOST_1",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "upload",
            "in": "formData",
            "description": "upload",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/contracts": {
      "get": {
        "tags": [
          "supplier-contract-controller"
        ],
        "summary": "Получение номеров договоров, заключенных с поставщиком",
        "operationId": "getSupplierContractIdsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "Идентификатор кампании поставщика",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierContract"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/documents": {
      "get": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "Получить сертификат по регистрационному номеру.",
        "operationId": "getCertificateUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "registrationNumber",
            "in": "query",
            "description": "Регистрационный номер сертификата",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CertificationDocument"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/documents/attachment": {
      "post": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "createUpload",
        "operationId": "createUploadUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "upload",
            "in": "formData",
            "description": "upload",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CertificationDocumentAttachmentUpload"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/documents/offers": {
      "get": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "Получить информацию об оферах привязанных к сертификату поставщика.",
        "operationId": "searchSupplierSkusUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "limit",
            "in": "query",
            "description": "limit",
            "required": false,
            "type": "integer",
            "default": 20,
            "format": "int32"
          },
          {
            "name": "page_token",
            "in": "query",
            "description": "page_token",
            "required": false,
            "type": "string"
          },
          {
            "name": "registrationNumber",
            "in": "query",
            "description": "Регистрационный номер сертификата",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CertificationDocumentOffersPageDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "Привязать офер к сертификату поставщика.",
        "operationId": "addCertificationDocumentSkuUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "registrationNumber",
            "in": "query",
            "description": "Регистрационный номер сертификата",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "shopSku",
            "in": "query",
            "description": "Индетификатор офера поставщика",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "Отвязать офер от сертификата поставщика.",
        "operationId": "removeCertificationDocumentSkuUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "registrationNumber",
            "in": "query",
            "description": "Регистрационный номер сертификата",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "shopSku",
            "in": "query",
            "description": "Индетификатор офера поставщика",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/documents/search": {
      "get": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "Получить список документов поставщиков, shop sku или регистрационный номер документа которых содержит query.",
        "operationId": "searchUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "limit",
            "in": "query",
            "description": "limit",
            "required": false,
            "type": "integer",
            "default": 20,
            "format": "int32"
          },
          {
            "name": "page_token",
            "in": "query",
            "description": "page_token",
            "required": false,
            "type": "string"
          },
          {
            "name": "q",
            "in": "query",
            "description": "Текстовая строка поискового запроса",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CertificationDocumentPageDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/documents/update": {
      "post": {
        "tags": [
          "certification-document-controller"
        ],
        "summary": "Создать/изменить сертификаты.",
        "operationId": "createCertificatesUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "requestBuilder",
            "description": "requestBuilder",
            "required": true,
            "schema": {
              "$ref": "#/definitions/CertificationDocumentsChangeRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AddCertificationDocumentsResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed": {
      "get": {
        "tags": [
          "supplier-feed-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте GET /latest для получения информации о фиде(прайс-листе) и GET /campaigns/{campaignId}/assortment/commits/latest для получения информации о ассортименте",
        "operationId": "getFeedInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierFeedInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/feed/download": {
      "get": {
        "tags": [
          "supplier-assortment-controller"
        ],
        "summary": "getSupplierFeed",
        "operationId": "getSupplierFeedUsingGET",
        "produces": [
          "application/octet-stream"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/latest": {
      "get": {
        "tags": [
          "supplier-feed-controller"
        ],
        "summary": "Информация о текущем фиде(прайс-листе) поставщика",
        "operationId": "getLatestFeedInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FeedInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/suggests": {
      "post": {
        "tags": [
          "supplier-suggest-controller"
        ],
        "summary": "enrichFeed",
        "operationId": "enrichFeedUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "file",
            "in": "formData",
            "description": "file",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FeedSuggestDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/suggests/{suggestId}": {
      "get": {
        "tags": [
          "supplier-suggest-controller"
        ],
        "summary": "getFeedEnrichmentResult",
        "operationId": "getFeedEnrichmentResultUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "suggestId",
            "in": "path",
            "description": "suggestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FeedSuggestInfoDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/suggests/{suggestId}/download": {
      "get": {
        "tags": [
          "supplier-suggest-controller"
        ],
        "summary": "getEnrichedFeed",
        "operationId": "getEnrichedFeedUsingGET",
        "produces": [
          "application/octet-stream"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "suggestId",
            "in": "path",
            "description": "suggestId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/updates": {
      "post": {
        "tags": [
          "supplier-feed-controller"
        ],
        "summary": "Заменить активный фид постащика на провалидированный фид",
        "operationId": "updateFeedUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "completeFeed",
            "in": "query",
            "description": "completeFeed",
            "required": false,
            "type": "boolean",
            "default": true
          },
          {
            "in": "body",
            "name": "selectedValidation",
            "description": "selectedValidation",
            "required": true,
            "schema": {
              "$ref": "#/definitions/SupplierValidationSelectionDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierFeedUpdateResultDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/validations": {
      "post": {
        "tags": [
          "supplier-feed-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте POST /validations/uploads",
        "operationId": "createValidationFromUploadOldUsingPOST",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/feed/validations/resources": {
      "post": {
        "tags": [
          "supplier-feed-controller"
        ],
        "summary": "Начать валидацию файла, на который ссылается переданный url",
        "operationId": "createValidationUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "form",
            "description": "form",
            "required": true,
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/feed/validations/uploads": {
      "post": {
        "tags": [
          "supplier-feed-controller"
        ],
        "summary": "Начать валидацию файла",
        "operationId": "createValidationFromUploadUsingPOST_2",
        "consumes": [
          "multipart/form-data"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "upload",
            "in": "formData",
            "description": "upload",
            "required": true,
            "type": "file"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение сводки по поставщику",
        "operationId": "getSupplierSummaryInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SupplierSummaryStateDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary/assortment": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение сводки по маппингам предложений поставщика",
        "operationId": "getAssortmentSummaryInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentSummaryDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary/feed": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение сводки по загруженным ценам поставщика",
        "operationId": "getFeedSummaryInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/FeedSummaryDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary/orders": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение сводки по заказам поставщика",
        "operationId": "getOrderSummaryInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OrderSummaryDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary/orders-v2": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение сводки по заказам поставщика новая версия",
        "operationId": "getOrderSummaryInfoV2UsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OrderSummaryDTOV2"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary/rating": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение рейтинга поставщика",
        "operationId": "getRatingSummaryInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/RatingSummaryDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/summary/stock-storage": {
      "get": {
        "tags": [
          "supplier-summary-controller"
        ],
        "summary": "Получение сводки по состоянию склада поставщика",
        "operationId": "getStockStorageSummaryInfoUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/StockStorageSummaryDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/suppliers/{campaignId}/validations/{validationId}": {
      "get": {
        "tags": [
          "supplier-validation-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте GET /campaigns/{campaignId}/assortment/validations/{validationId}",
        "operationId": "getValidationUsingGET_1",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validationId",
            "in": "path",
            "description": "validationId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AssortmentFeedValidationDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/suppliers/{campaignId}/validations/{validationId}/download": {
      "get": {
        "tags": [
          "supplier-validation-controller"
        ],
        "summary": "Не используйте эту ручку, вместо этого используйте GET /campaigns/{campaignId}/assortment/validations/{validationId}/download",
        "operationId": "getValidationDownloadUsingGET_1",
        "produces": [
          "application/octet-stream"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaignId",
            "in": "path",
            "description": "campaignId",
            "required": true,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validationId",
            "in": "path",
            "description": "validationId",
            "required": true,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/surveys": {
      "get": {
        "tags": [
          "survey-controller"
        ],
        "summary": "getDatasourceSurveys",
        "operationId": "getDatasourceSurveysUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/ShopSurveysDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/surveys/{surveyId}/passed": {
      "post": {
        "tags": [
          "survey-controller"
        ],
        "summary": "markSurveyAsPassed",
        "operationId": "markSurveyAsPassedUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "surveyId",
            "in": "path",
            "description": "surveyId",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/timezones": {
      "get": {
        "tags": [
          "timezone-controller"
        ],
        "summary": "Получение списка таймзон",
        "operationId": "getAllTimezonesUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "$ref": "#/definitions/TimezoneDTO"
              }
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/token": {
      "get": {
        "tags": [
          "token-controller"
        ],
        "summary": "Получение токена",
        "operationId": "getMarketTokenUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "application_id",
            "in": "query",
            "description": "Идентификатор сервиса, к которому осуществляется доступ посредством токена",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "euid",
            "in": "query",
            "description": "euid",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/MarketToken"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "post": {
        "tags": [
          "token-controller"
        ],
        "summary": "Добавление токена",
        "operationId": "saveMarketTokenUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "access_token",
            "in": "query",
            "description": "Ключ токена",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "application_id",
            "in": "query",
            "description": "Идентификатор сервиса, к которому осуществляется доступ посредством токена",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "euid",
            "in": "query",
            "description": "euid",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "expire_date",
            "in": "query",
            "description": "Таймстамп протухания токен",
            "required": false,
            "type": "string",
            "format": "date-time",
            "allowEmptyValue": false
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/MarketToken"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "token-controller"
        ],
        "summary": "Удаление токена",
        "operationId": "deleteTokenUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "application_id",
            "in": "query",
            "description": "Идентификатор сервиса, к которому осуществляется доступ посредством токена",
            "required": false,
            "type": "string",
            "allowEmptyValue": false
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "euid",
            "in": "query",
            "description": "euid",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/updateCpcPlacement": {
      "post": {
        "tags": [
          "cpc-placement-controller"
        ],
        "summary": "Включение / выключение программы CPC",
        "operationId": "updateCpcPlacementUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "enabled",
            "in": "query",
            "description": "enabled",
            "required": true,
            "type": "boolean"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/GenericResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/updateShopDsSettings": {
      "put": {
        "tags": [
          "market-delivery-controller"
        ],
        "summary": "update",
        "operationId": "updateUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "settings",
            "description": "settings",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ShopMarketDeliveryServiceSettings"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/feeds/history": {
      "get": {
        "tags": [
          "data-camp-feed-controller"
        ],
        "summary": "Информация по последним фидам, удачно загруженному партнёром в оферное хранилище",
        "operationId": "getHistoryUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "feedIds",
            "in": "query",
            "description": "feedIds",
            "required": false,
            "type": "array",
            "items": {
              "type": "integer",
              "format": "int64"
            },
            "collectionFormat": "multi"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/DataCampFeedHistoryResponse"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/offers": {
      "post": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Создание офера",
        "operationId": "createOfferUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "request",
            "description": "request",
            "required": true,
            "schema": {
              "$ref": "#/definitions/CreateUpdateOfferRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Удаление набора оферов",
        "operationId": "deleteOffersUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "in": "body",
            "name": "request",
            "description": "request",
            "required": true,
            "schema": {
              "$ref": "#/definitions/DeleteOffersRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/offers/search": {
      "post": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Поиск и постраничная выдача содержимого оферного хранилища",
        "operationId": "searchUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "pageSize",
            "in": "query",
            "description": "pageSize",
            "required": false,
            "type": "integer",
            "format": "int32"
          },
          {
            "name": "pagingSpread",
            "in": "query",
            "description": "pagingSpread",
            "required": false,
            "type": "integer",
            "default": 2,
            "format": "int32"
          },
          {
            "in": "body",
            "name": "request",
            "description": "request",
            "required": false,
            "schema": {
              "$ref": "#/definitions/SearchOffersRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/SearchOffersResponse"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/offers/{offerId}": {
      "get": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Поиск и выдача офера по указанному идентификатору",
        "operationId": "getOfferUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "path",
            "description": "offerId",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "put": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Обновление существующего офера",
        "operationId": "updateOfferUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "path",
            "description": "offerId",
            "required": true,
            "type": "string"
          },
          {
            "in": "body",
            "name": "request",
            "description": "request",
            "required": true,
            "schema": {
              "$ref": "#/definitions/CreateUpdateOfferRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      },
      "delete": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Удаление офера",
        "operationId": "deleteOfferUsingDELETE",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "path",
            "description": "offerId",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK"
          },
          "204": {
            "description": "No Content"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/offers/{offerId}/disabled": {
      "put": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Изменение статуса скрытия офера",
        "operationId": "changeOfferStatusUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "path",
            "description": "offerId",
            "required": true,
            "type": "string"
          },
          {
            "in": "body",
            "name": "request",
            "description": "request",
            "required": true,
            "schema": {
              "$ref": "#/definitions/ChangeOfferStatusRequest"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/offers/{offerId}/price": {
      "put": {
        "tags": [
          "data-camp-controller"
        ],
        "summary": "Изменение цены офера",
        "operationId": "changeOfferPriceUsingPUT",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "offerId",
            "in": "path",
            "description": "offerId",
            "required": true,
            "type": "string"
          },
          {
            "in": "body",
            "name": "request",
            "description": "request",
            "required": true,
            "schema": {
              "$ref": "#/definitions/PriceDTO"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/OfferDTO"
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v1/campaigns/{campaignId}/report": {
      "get": {
        "tags": [
          "data-camp-report-controller"
        ],
        "summary": "Отчёт по оферам парнёра загруженых в оферное хранилище",
        "operationId": "csvFeedReportUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/StreamingResponseBody"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v1/categories/{categoryId}/fields": {
      "get": {
        "tags": [
          "data-camp-parameters-controller"
        ],
        "summary": "Ручка выдачи параметров категории по её идентификатору из данных МБО",
        "operationId": "getParamsUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "categoryId",
            "in": "path",
            "description": "categoryId",
            "required": true,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/CategoryDTO"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    },
    "/v2/validateFeed": {
      "post": {
        "tags": [
          "feed-validation-controller"
        ],
        "summary": "validateFeed",
        "operationId": "validateFeedUsingPOST",
        "consumes": [
          "application/json"
        ],
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "login",
            "in": "query",
            "description": "login",
            "required": false,
            "type": "string"
          },
          {
            "name": "password",
            "in": "query",
            "description": "password",
            "required": false,
            "type": "string"
          },
          {
            "name": "url",
            "in": "query",
            "description": "url",
            "required": false,
            "type": "string"
          },
          {
            "name": "validate_id",
            "in": "query",
            "description": "validate_id",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "validation_type",
            "in": "query",
            "description": "validation_type",
            "required": true,
            "type": "string",
            "enum": [
              "TOLERANT",
              "FULL",
              "NULL",
              "SUPPLIER_MAPPING",
              "SUPPLIER_PRICES",
              "SUPPLIER_MAPPING_WITH_PRICES",
              "CHINA_GOODS_APP_FEED",
              "FMCG_APP_FEED"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "array",
              "items": {
                "type": "object"
              }
            }
          },
          "201": {
            "description": "Created"
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": true
      }
    },
    "/virtual-shop/status": {
      "get": {
        "tags": [
          "virtual-shop-controller"
        ],
        "summary": "isVirtual",
        "operationId": "isVirtualUsingGET",
        "produces": [
          "*/*"
        ],
        "parameters": [
          {
            "name": "_user_id",
            "in": "query",
            "description": "Идентификатор пользователя",
            "required": false,
            "type": "integer",
            "format": "int64"
          },
          {
            "name": "campaign_id",
            "in": "query",
            "description": "Идентификатор кампании",
            "required": false,
            "type": "integer",
            "format": "int64"
          }
        ],
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "boolean"
            }
          },
          "401": {
            "description": "Unauthorized"
          },
          "403": {
            "description": "Forbidden"
          },
          "404": {
            "description": "Not Found"
          }
        },
        "deprecated": false
      }
    }
  },
  "definitions": {
    "AboHidingDetailsDto": {
      "type": "object",
      "properties": {
        "hidingDetails": {
          "xml": {
            "name": "hidingDetails",
            "attribute": true,
            "wrapped": false
          },
          "description": "Детали скрытия",
          "$ref": "#/definitions/HidingDetails"
        },
        "hidingReason": {
          "type": "string",
          "xml": {
            "name": "hidingReason",
            "attribute": true,
            "wrapped": false
          },
          "description": "Причина скрытия"
        },
        "hidingSubReason": {
          "type": "string",
          "xml": {
            "name": "hidingSubReason",
            "attribute": true,
            "wrapped": false
          },
          "description": "Дополнительная информация по причине скрытия"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id оффера"
        },
        "regionName": {
          "type": "string",
          "xml": {
            "name": "regionName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Имя региона"
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          },
          "description": "Url оффера"
        }
      },
      "title": "AboHidingDetailsDto",
      "xml": {
        "name": "AboHidingDetailsDto",
        "attribute": false,
        "wrapped": false
      },
      "description": "Детали сокрытия оффера через abo"
    },
    "AddCertificationDocumentResponse": {
      "type": "object",
      "properties": {
        "certificationDocument": {
          "xml": {
            "name": "certificationDocument",
            "attribute": false,
            "wrapped": false
          },
          "description": "Сертификат поставщика",
          "$ref": "#/definitions/CertificationDocument"
        },
        "errors": {
          "type": "array",
          "xml": {
            "name": "errors",
            "attribute": false,
            "wrapped": true
          },
          "description": "Список ошибок",
          "items": {
            "$ref": "#/definitions/UserMessageDTO"
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус",
          "enum": [
            "OK",
            "ERROR"
          ]
        }
      },
      "title": "AddCertificationDocumentResponse",
      "xml": {
        "name": "addCertificationDocumentResponse",
        "attribute": false,
        "wrapped": false
      },
      "description": "Ответ поставщика по измененному документу"
    },
    "AddCertificationDocumentsResponse": {
      "type": "object",
      "properties": {
        "addCertificationDocumentResponses": {
          "type": "array",
          "xml": {
            "name": "addCertificationDocumentResponses",
            "attribute": false,
            "wrapped": true
          },
          "description": "Ответы по добавлению поставщика",
          "items": {
            "$ref": "#/definitions/AddCertificationDocumentResponse"
          }
        },
        "error": {
          "xml": {
            "name": "error",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ошибка",
          "$ref": "#/definitions/UserMessageDTO"
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус",
          "enum": [
            "OK",
            "ERROR"
          ]
        }
      },
      "title": "AddCertificationDocumentsResponse",
      "xml": {
        "name": "addCertificationDocumentsResponse",
        "attribute": false,
        "wrapped": false
      },
      "description": "Ответ поставщика по измененным документам"
    },
    "Address": {
      "type": "object",
      "properties": {
        "addrAdditional": {
          "type": "string",
          "description": "Прочая адресная информация. Информация, которая не может быть описана другими полями (например - 3-ий этаж, павильнон 2)"
        },
        "block": {
          "type": "string",
          "description": "Корпус"
        },
        "building": {
          "type": "string",
          "description": "Строение"
        },
        "city": {
          "type": "string",
          "description": "Город"
        },
        "estate": {
          "type": "string",
          "description": "Владение"
        },
        "flat": {
          "type": "string",
          "description": "Квартира"
        },
        "km": {
          "type": "integer",
          "format": "int32",
          "description": "Километр"
        },
        "number": {
          "type": "string",
          "description": "Номер дома"
        },
        "postCode": {
          "type": "string",
          "description": "Почтовый индекс"
        },
        "street": {
          "type": "string",
          "description": "Улица"
        }
      },
      "title": "Address",
      "description": "Адрес точки"
    },
    "AgencyRewardSummaryDTO": {
      "type": "object",
      "properties": {
        "activityRatio": {
          "type": "number",
          "format": "double",
          "description": "Коэффициент активности",
          "required": true
        },
        "agencyId": {
          "type": "integer",
          "format": "int64",
          "description": "Id агенства",
          "required": true
        },
        "maxTotalRatio": {
          "type": "number",
          "format": "double",
          "description": "Максимальное значение итогового коэффициента",
          "required": true
        },
        "minActivityRatio": {
          "type": "number",
          "format": "double",
          "description": "Минимальный коэффициент активности",
          "required": true
        },
        "minQualityRatio": {
          "type": "number",
          "format": "double",
          "description": "Минимальный коэффициент качества",
          "required": true
        },
        "qualityRatio": {
          "type": "number",
          "format": "double",
          "description": "Коэффициент качества",
          "required": true
        },
        "quarter": {
          "description": "Квартал",
          "$ref": "#/definitions/RewardQuarterDTO",
          "required": true
        },
        "totalRatio": {
          "type": "number",
          "format": "double",
          "description": "Итоговый коэффициент"
        },
        "updatedAt": {
          "type": "string",
          "format": "date-time",
          "description": "Дата, когда производился расчет",
          "required": true
        }
      },
      "title": "AgencyRewardSummaryDTO"
    },
    "AmountCurrencyDTO": {
      "type": "object",
      "properties": {
        "amount": {
          "type": "number",
          "xml": {
            "name": "amount",
            "attribute": true,
            "wrapped": false
          }
        },
        "currency": {
          "type": "string",
          "xml": {
            "name": "currency",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "AmountCurrencyDTO"
    },
    "ApplicationValidationsDTO": {
      "type": "object",
      "properties": {
        "canDownload": {
          "type": "boolean",
          "xml": {
            "name": "canDownload",
            "attribute": true,
            "wrapped": false
          }
        },
        "canSubmit": {
          "type": "boolean",
          "xml": {
            "name": "canSubmit",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "ApplicationValidationsDTO",
      "xml": {
        "name": "validations",
        "attribute": false,
        "wrapped": false
      }
    },
    "AssortmentFeedDTO": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "description": "ID фида в БД mbi"
        },
        "indexerFeedIds": {
          "type": "array",
          "description": "Список идентификаторов фидов, под которыми оферы лежат в индексаторе. Могут отличаться из feed.id. Может быть несколько в случае мультискладов",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "pushFeedInfo": {
          "description": "Информация о последнем удачном пуш-фиде, если это пуш-партнер",
          "$ref": "#/definitions/PushFeedInfo"
        },
        "resource": {
          "description": "Информация о файле, доступ к которому осуществляется по URL",
          "$ref": "#/definitions/FeedResourceDTO"
        },
        "upload": {
          "description": "Информация о файле, загруженном на сервер",
          "$ref": "#/definitions/FileUploadDTO"
        }
      },
      "title": "AssortmentFeedDTO",
      "description": "Информация об источнике данных"
    },
    "AssortmentFeedValidationDTO": {
      "type": "object",
      "properties": {
        "campaignId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "campaignId",
            "attribute": true,
            "wrapped": false
          }
        },
        "feed": {
          "description": "Информация о валидируемом источнике данных",
          "$ref": "#/definitions/AssortmentFeedDTO"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "result": {
          "description": "Результат валидации: статистика и ошибки",
          "$ref": "#/definitions/ResultDTO"
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "description": "Текущий статус процесса валидации",
          "enum": [
            "PROCESSING",
            "OK",
            "WARNING",
            "ERROR"
          ]
        }
      },
      "title": "AssortmentFeedValidationDTO",
      "xml": {
        "name": "feedValidationDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о процессе валидации"
    },
    "AssortmentInfoDTO": {
      "type": "object",
      "properties": {
        "assortment": {
          "description": "Информация о текущем ассортименте(маппинге) поставщика",
          "$ref": "#/definitions/AssortmentFeedDTO"
        }
      },
      "title": "AssortmentInfoDTO",
      "xml": {
        "name": "assortmentInfoDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о текущем ассортименте(маппинге) поставщика"
    },
    "AssortmentSummaryDTO": {
      "type": "object",
      "properties": {
        "offers": {
          "$ref": "#/definitions/OffersSummaryDTO"
        },
        "upload": {
          "$ref": "#/definitions/UploadFeedDTO"
        }
      },
      "title": "AssortmentSummaryDTO"
    },
    "AvailabilityUpdateDTO": {
      "type": "object",
      "properties": {
        "availability": {
          "type": "string",
          "enum": [
            "ACTIVE",
            "INACTIVE",
            "DELISTED"
          ]
        }
      },
      "title": "AvailabilityUpdateDTO"
    },
    "BankInfoDTO": {
      "type": "object",
      "properties": {
        "bic": {
          "type": "string",
          "xml": {
            "name": "bic",
            "attribute": true,
            "wrapped": false
          },
          "description": "Банковский идентификационный код (БИК)"
        },
        "isArchived": {
          "type": "boolean",
          "xml": {
            "name": "isArchived",
            "attribute": true,
            "wrapped": false
          },
          "description": "Является ли банк неактивным (заархивированным)"
        },
        "place": {
          "type": "string",
          "xml": {
            "name": "place",
            "attribute": true,
            "wrapped": false
          },
          "description": "Город (или другой населенный пункт) банка"
        }
      },
      "title": "BankInfoDTO"
    },
    "BannerListWrapper": {
      "type": "object",
      "title": "BannerListWrapper"
    },
    "BaseSecurable": {
      "type": "object",
      "properties": {
        "defaults": {
          "$ref": "#/definitions/BaseSecurable"
        },
        "override": {
          "type": "boolean"
        },
        "roles": {
          "$ref": "#/definitions/SecurityRule"
        },
        "states": {
          "$ref": "#/definitions/SecurityRule"
        }
      },
      "title": "BaseSecurable"
    },
    "Builder": {
      "type": "object",
      "properties": {
        "marketSkus": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        }
      },
      "title": "Builder"
    },
    "Cabinet": {
      "type": "object",
      "properties": {
        "defaults": {
          "$ref": "#/definitions/BaseSecurable"
        },
        "features": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Feature"
          }
        },
        "override": {
          "type": "boolean"
        },
        "pages": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Page"
          }
        },
        "roles": {
          "$ref": "#/definitions/SecurityRule"
        },
        "states": {
          "$ref": "#/definitions/SecurityRule"
        }
      },
      "title": "Cabinet"
    },
    "CampaignsSummary": {
      "type": "object",
      "properties": {
        "types": {
          "type": "array",
          "description": "Типы кампаний",
          "items": {
            "type": "string",
            "enum": [
              "SHOP",
              "SUPPLIER",
              "CROSSBORDER",
              "FMCG",
              "DELIVERY"
            ]
          }
        }
      },
      "title": "CampaignsSummary",
      "xml": {
        "name": "campaigns",
        "attribute": false,
        "wrapped": false
      },
      "description": "Сводная информация по кампаниям"
    },
    "CategoryDTO": {
      "type": "object",
      "properties": {
        "acceptGoodContent": {
          "type": "boolean",
          "xml": {
            "name": "acceptGoodContent",
            "attribute": true,
            "wrapped": false
          },
          "description": "Принимать только Good Content"
        },
        "acceptPartnerModels": {
          "type": "boolean",
          "xml": {
            "name": "acceptPartnerModels",
            "attribute": true,
            "wrapped": false
          },
          "description": "Возможно массовое заведение моделей через XLS-файлы"
        },
        "acceptPartnerSkus": {
          "type": "boolean",
          "xml": {
            "name": "acceptPartnerSkus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Возможно массовое заведение sku через XLS-файлы"
        },
        "categoryId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "categoryId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор категории"
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Имя категории"
        },
        "offerCount": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "offerCount",
            "attribute": true,
            "wrapped": false
          },
          "description": "Количество оферов в категории"
        },
        "parentId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "parentId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор родительской категории. У родительской категории parentId = null"
        }
      },
      "title": "CategoryDTO",
      "xml": {
        "name": "CategoryDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Категория для офера из mboc"
    },
    "CategoryDto": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор последней категории из names"
        },
        "names": {
          "type": "array",
          "xml": {
            "name": "names",
            "attribute": true,
            "wrapped": false
          },
          "description": "Имена категорий. Первый элемент родительский, последний дочерний",
          "items": {
            "type": "string"
          }
        }
      },
      "title": "CategoryDto",
      "xml": {
        "name": "CategoryDto",
        "attribute": false,
        "wrapped": false
      },
      "description": "Маркетная категория для фильтра на фронте"
    },
    "CategoryParameterDTO": {
      "type": "object",
      "properties": {
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание"
        },
        "filterIndex": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "filterIndex",
            "attribute": true,
            "wrapped": false
          },
          "description": "Common filter index"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор параметра"
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Наименование параметра"
        },
        "restrictions": {
          "type": "array",
          "xml": {
            "name": "restrictions",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ограничения параметра",
          "items": {
            "type": "string"
          }
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "description": "Тип параметра"
        },
        "unit": {
          "type": "string",
          "xml": {
            "name": "unit",
            "attribute": true,
            "wrapped": false
          },
          "description": "Единица измерения"
        }
      },
      "title": "CategoryParameterDTO"
    },
    "Cell": {
      "type": "object",
      "title": "Cell"
    },
    "CertificationDocument": {
      "type": "object",
      "properties": {
        "customsCommodityCodes": {
          "type": "array",
          "xml": {
            "name": "customsCommodityCodes",
            "attribute": false,
            "wrapped": true
          },
          "items": {
            "type": "string"
          }
        },
        "deletePictureMdmUrls": {
          "type": "array",
          "xml": {
            "name": "deletePictureMdmUrls",
            "attribute": true,
            "wrapped": false
          },
          "items": {
            "type": "string"
          }
        },
        "endDate": {
          "xml": {
            "name": "endDate",
            "attribute": true,
            "wrapped": false
          },
          "description": "Дата окончания действия сертификата",
          "$ref": "#/definitions/LocalDate"
        },
        "newScanFileIds": {
          "type": "array",
          "xml": {
            "name": "newScanFileIds",
            "attribute": true,
            "wrapped": false
          },
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "pictureUrls": {
          "type": "array",
          "xml": {
            "name": "pictureUrls",
            "attribute": true,
            "wrapped": false
          },
          "description": "Сылки на картинки (сканы) к сертификату",
          "items": {
            "type": "string"
          }
        },
        "registrationNumber": {
          "type": "string",
          "xml": {
            "name": "registrationNumber",
            "attribute": true,
            "wrapped": false
          },
          "description": "Регистрационный номер документа"
        },
        "requirements": {
          "type": "string",
          "xml": {
            "name": "requirements",
            "attribute": false,
            "wrapped": false
          }
        },
        "serialNumber": {
          "type": "string",
          "xml": {
            "name": "serialNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "startDate": {
          "type": "string",
          "format": "date",
          "xml": {
            "name": "startDate",
            "attribute": true,
            "wrapped": false
          },
          "description": "Дата начала действия сертификата"
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "description": "Тип документа",
          "enum": [
            "UNKNOWN",
            "DECLARATION_OF_CONFORMITY",
            "CERTIFICATE_OF_CONFORMITY",
            "CERTIFICATE_OF_STATE_REGISTRATION",
            "EXEMPTION_LETTER",
            "FIRE_SAFETY_CERTIFICATE",
            "FIRE_SAFETY_DECLARATION",
            "EX_EQUIPMENT_CERTIFICATE",
            "ENVIRONMENTAL_SAFETY_CERTIFICATE",
            "CERTIFICATE_OF_ORIGIN",
            "PASSPORT_MSDS",
            "MEDICAL_PRODUCT_REGISTRATION_CERTIFICATE"
          ]
        }
      },
      "title": "CertificationDocument",
      "description": "Сертификат для свзяки с офферами поставщика"
    },
    "CertificationDocumentAttachmentUpload": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор загрузки"
        },
        "size": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "size",
            "attribute": true,
            "wrapped": false
          },
          "description": "Размер файла загрузки"
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          },
          "description": "Url файла загрузки"
        }
      },
      "title": "CertificationDocumentAttachmentUpload",
      "xml": {
        "name": "certificationDocumentAttachmentUpload",
        "attribute": false,
        "wrapped": false
      },
      "description": "Вложенный файл поставщика"
    },
    "CertificationDocumentOffersPageDTO": {
      "type": "object",
      "properties": {
        "nextPageToken": {
          "type": "string",
          "xml": {
            "name": "nextPageToken",
            "attribute": true,
            "wrapped": false
          }
        },
        "shopSkus": {
          "type": "array",
          "xml": {
            "name": "shopSkus",
            "attribute": false,
            "wrapped": true
          },
          "items": {
            "$ref": "#/definitions/MappedShopSku"
          }
        }
      },
      "title": "CertificationDocumentOffersPageDTO",
      "xml": {
        "name": "certificationDocumentOffersPage",
        "attribute": false,
        "wrapped": false
      }
    },
    "CertificationDocumentPageDTO": {
      "type": "object",
      "properties": {
        "certificationDocuments": {
          "type": "array",
          "xml": {
            "name": "certificationDocuments",
            "attribute": false,
            "wrapped": true
          },
          "items": {
            "$ref": "#/definitions/CertificationDocument"
          }
        },
        "nextPageToken": {
          "type": "string",
          "xml": {
            "name": "nextPageToken",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "CertificationDocumentPageDTO",
      "xml": {
        "name": "certificationDocumentPage",
        "attribute": false,
        "wrapped": false
      }
    },
    "CertificationDocumentsChangeRequest": {
      "type": "object",
      "properties": {
        "certificationDocuments": {
          "type": "array",
          "xml": {
            "name": "certificationDocuments",
            "attribute": false,
            "wrapped": true
          },
          "items": {
            "$ref": "#/definitions/CertificationDocument"
          }
        }
      },
      "title": "CertificationDocumentsChangeRequest",
      "xml": {
        "name": "certificationDocumentsChangeRequest",
        "attribute": false,
        "wrapped": false
      },
      "description": "Запрос на изменение списка сертификатов."
    },
    "ChangeOfferStatusRequest": {
      "type": "object",
      "properties": {
        "value": {
          "type": "boolean",
          "xml": {
            "name": "value",
            "attribute": false,
            "wrapped": false
          },
          "description": "Значение флага скрытия"
        }
      },
      "title": "ChangeOfferStatusRequest",
      "xml": {
        "name": "changeRequest",
        "attribute": false,
        "wrapped": false
      },
      "description": "Запрос на измение флага скрытия офера в оферном хранилище"
    },
    "ClientApplications": {
      "type": "object",
      "properties": {
        "applications": {
          "type": "array",
          "description": "Список заявок",
          "items": {
            "$ref": "#/definitions/SupplierApplicationDTO"
          },
          "required": true
        }
      },
      "title": "ClientApplications",
      "xml": {
        "name": "clientApplications",
        "attribute": false,
        "wrapped": false
      },
      "description": "Заявки на подлючение к Беру поставщиков клиента"
    },
    "ClientSummary": {
      "type": "object",
      "properties": {
        "campaignsSummary": {
          "description": "Сводная информация по кампаниям клиента.",
          "$ref": "#/definitions/CampaignsSummary"
        },
        "clientId": {
          "type": "integer",
          "format": "int64",
          "description": "Id клиента в Балансе. -1, если клиент не найден."
        },
        "isAgency": {
          "type": "boolean",
          "description": "Является ли клиент агенством."
        },
        "isSubclient": {
          "type": "boolean",
          "description": "Является ли субклиентом."
        }
      },
      "title": "ClientSummary",
      "xml": {
        "name": "client",
        "attribute": false,
        "wrapped": false
      },
      "description": "Сводная информация по клиенту и его кампаниям"
    },
    "ContactInfoDTO": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string",
          "xml": {
            "name": "email",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "phoneNumber": {
          "type": "string",
          "xml": {
            "name": "phoneNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "shopPhoneNumber": {
          "type": "string",
          "xml": {
            "name": "shopPhoneNumber",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "ContactInfoDTO",
      "xml": {
        "name": "organizationInfo",
        "attribute": false,
        "wrapped": false
      }
    },
    "Coordinates": {
      "type": "object",
      "properties": {
        "lat": {
          "type": "number",
          "format": "double",
          "description": "Географичесская широта"
        },
        "lon": {
          "type": "number",
          "format": "double",
          "description": "Географичесская долгота"
        }
      },
      "title": "Coordinates",
      "description": "GPS-координаты"
    },
    "CountAmountCurrencyDTO": {
      "type": "object",
      "properties": {
        "count": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "count",
            "attribute": true,
            "wrapped": false
          }
        },
        "nItems": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "nItems",
            "attribute": true,
            "wrapped": false
          }
        },
        "sum": {
          "$ref": "#/definitions/AmountCurrencyDTO"
        }
      },
      "title": "CountAmountCurrencyDTO",
      "xml": {
        "name": "countAmountCurrency",
        "attribute": false,
        "wrapped": false
      }
    },
    "CpcState": {
      "type": "object",
      "properties": {
        "canSwitchToOn": {
          "type": "boolean",
          "description": "Можно ли перевести тумблер CPC в состояние \"ON\"."
        },
        "cpc": {
          "type": "string",
          "description": "Текущее состояние подключения к программе \"Покупка на сайте\"",
          "enum": [
            "REAL",
            "NONE"
          ]
        },
        "cpcCutoffs": {
          "type": "array",
          "description": "Открытые CPC-отключения магазина",
          "items": {
            "type": "string",
            "enum": [
              "NULL",
              "YAMANAGER",
              "QMANAGER",
              "FINANCE",
              "TECHNICAL_DLV_REGIONS",
              "PARTNER",
              "FORTESTING",
              "CLONE",
              "QMANAGER_CHEESY",
              "QMANAGER_FRAUD",
              "QMANAGER_CLONE",
              "QMANAGER_OTHER",
              "TECHNICAL_YML",
              "TECHNICAL_ORG_INFO",
              "TECHNICAL_OWN_REGION",
              "TECHNICAL_SHIPPING_INFO",
              "PARTNER_SCHEDULE",
              "QUALITY_PINGER",
              "WEBMASTER",
              "QUALITY",
              "NEW_SNIPPET",
              "MODIFIED",
              "NEED_TESTING",
              "LINK_REJECTED",
              "TECHNICAL_NEED_INFO",
              "EXPIRED_UPLOAD_FEED",
              "CPA_QUALITY_API",
              "CPA_QUALITY_AUTO",
              "CPA_API_NEED_INFO",
              "CPA_NEED_TESTING",
              "CPA_QUALITY_CHEESY",
              "CPA_QUALITY_OTHER",
              "CPA_FINANCE",
              "CPA_FOR_TESTING",
              "CPA_PARTNER",
              "CPA_CONTRACT",
              "CPA_CPC",
              "CPA_FEED",
              "CPA_QUALITY_PARTNER",
              "CPA_NO_PAYMENT_METHOD_AVAILABLE",
              "CPA_GENERAL",
              "COMMON_QUALITY",
              "COMMON_OTHER",
              "CPA_TEST_ORDER_API",
              "CPC_PARTNER",
              "CPA_TECHNICAL_NEED_INFO",
              "CPA_QUALITY_OTHER_AUTO",
              "CPC_FINANCE_LIMIT",
              "CPC_OVERDRAFT_CONTROL"
            ]
          }
        },
        "passedModerationOnce": {
          "type": "boolean",
          "description": "true, если магазин хоть раз успешно проходил CPC-премодерацию"
        }
      },
      "title": "CpcState",
      "xml": {
        "name": "CpcState",
        "attribute": false,
        "wrapped": false
      },
      "description": "Состояния CPC программы магазина"
    },
    "CreateFeedResponse": {
      "type": "object",
      "properties": {
        "feedId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "CreateFeedResponse",
      "xml": {
        "name": "createFeedResponse",
        "attribute": false,
        "wrapped": false
      }
    },
    "CreateUpdateOfferRequest": {
      "type": "object",
      "properties": {
        "category": {
          "xml": {
            "name": "category",
            "attribute": true,
            "wrapped": false
          },
          "description": "Категория товара",
          "$ref": "#/definitions/CategoryDTO"
        },
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание"
        },
        "id": {
          "type": "string",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор офера"
        },
        "model": {
          "xml": {
            "name": "model",
            "attribute": true,
            "wrapped": false
          },
          "description": "Модель",
          "$ref": "#/definitions/ModelDTO"
        },
        "pictures": {
          "type": "array",
          "xml": {
            "name": "pictures",
            "attribute": true,
            "wrapped": false
          },
          "description": "Картинки",
          "items": {
            "$ref": "#/definitions/PictureDTO"
          }
        },
        "prices": {
          "xml": {
            "name": "prices",
            "attribute": true,
            "wrapped": false
          },
          "description": "Цена",
          "$ref": "#/definitions/PriceDTO"
        },
        "publishingStatus": {
          "xml": {
            "name": "publishingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус скрытия",
          "$ref": "#/definitions/PublishingStatusDTO"
        },
        "seller": {
          "xml": {
            "name": "seller",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий продавца",
          "$ref": "#/definitions/SalesNotesDTO"
        },
        "titles": {
          "xml": {
            "name": "titles",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название",
          "$ref": "#/definitions/HighlightedTextDTO"
        },
        "urls": {
          "xml": {
            "name": "urls",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ссылки на товар магазина",
          "$ref": "#/definitions/UrlsDTO"
        },
        "vendor": {
          "xml": {
            "name": "vendor",
            "attribute": true,
            "wrapped": false
          },
          "description": "Вендор",
          "$ref": "#/definitions/VendorDTO"
        },
        "wareId": {
          "type": "string",
          "xml": {
            "name": "wareId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ware id офера"
        }
      },
      "title": "CreateUpdateOfferRequest",
      "description": "Запрос на создание или изменение офера в оферном хранилище"
    },
    "CredentialsDTO": {
      "type": "object",
      "properties": {
        "login": {
          "type": "string",
          "xml": {
            "name": "login",
            "attribute": true,
            "wrapped": false
          }
        },
        "password": {
          "type": "string",
          "xml": {
            "name": "password",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "CredentialsDTO"
    },
    "CreditTemplateListDTO": {
      "type": "object",
      "properties": {
        "creditTemplates": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/CreditTemplateResponseDTO"
          }
        }
      },
      "title": "CreditTemplateListDTO",
      "xml": {
        "name": "creditTemplates",
        "attribute": false,
        "wrapped": false
      }
    },
    "CreditTemplateRequestDTO": {
      "type": "object",
      "properties": {
        "bankId": {
          "type": "integer",
          "format": "int64",
          "description": "Идентификатор банка"
        },
        "conditionsUrl": {
          "type": "string",
          "description": "Ссылка на сайт с описанием полных условий кредитования"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "description": "Внутренний идентификатор шаблона, при создании нового шаблона должен быть null"
        },
        "maxPrice": {
          "type": "integer",
          "format": "int64",
          "description": "Максимальная цена товара, к которой применим дефолтный шаблон"
        },
        "maxTermMonths": {
          "type": "integer",
          "format": "int32",
          "description": "Максимальный срок кредита в месяцах"
        },
        "minPrice": {
          "type": "integer",
          "format": "int64",
          "description": "Минимальная цена товара, к которой применим дефолтный шаблон"
        },
        "minRateScaled": {
          "type": "integer",
          "format": "int32",
          "description": "Минимальная процентная ставка по кредиту, умноженная на RATE_POW"
        },
        "partnerId": {
          "type": "integer",
          "format": "int64",
          "description": "Идентификатор магазина"
        },
        "type": {
          "type": "string",
          "description": "Тип шаблона (для использования в фиде, по умолчанию или по умолчанию с ограничениями)",
          "enum": [
            "FEED",
            "DEFAULT_FOR_ALL",
            "DEFAULT_FOR_ALL_IN_RANGE"
          ]
        }
      },
      "title": "CreditTemplateRequestDTO",
      "xml": {
        "name": "creditTemplate",
        "attribute": false,
        "wrapped": false
      }
    },
    "CreditTemplateResponseDTO": {
      "type": "object",
      "properties": {
        "bankInfo": {
          "description": "Информация о банке",
          "$ref": "#/definitions/BankInfoDTO"
        },
        "conditionsUrl": {
          "type": "string",
          "description": "Ссылка на сайт с описанием полных условий кредитования"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "description": "Внутренний идентификатор шаблона"
        },
        "maxPrice": {
          "type": "integer",
          "format": "int64",
          "description": "Максимальная цена товара, к которой применим дефолтный шаблон"
        },
        "maxTermMonths": {
          "type": "integer",
          "format": "int32",
          "description": "Максимальный срок кредита в месяцах"
        },
        "minPrice": {
          "type": "integer",
          "format": "int64",
          "description": "Минимальная цена товара, к которой применим дефолтный шаблон"
        },
        "minRateScaled": {
          "type": "integer",
          "format": "int32",
          "description": "Минимальная процентная ставка по кредиту, умноженная на RATE_POW"
        },
        "partnerId": {
          "type": "integer",
          "format": "int64",
          "description": "Идентификатор магазина"
        },
        "partnerTemplateId": {
          "type": "integer",
          "format": "int64",
          "description": "Магазинный идентификатор шаблона"
        },
        "type": {
          "type": "string",
          "description": "Тип шаблона (для использования в фиде, по умолчанию или по умолчанию с ограничениями)",
          "enum": [
            "FEED",
            "DEFAULT_FOR_ALL",
            "DEFAULT_FOR_ALL_IN_RANGE"
          ]
        }
      },
      "title": "CreditTemplateResponseDTO"
    },
    "CrossborderCampaignDto": {
      "type": "object",
      "properties": {
        "campaignId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "campaignId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id кампании"
        },
        "enabled": {
          "type": "boolean",
          "xml": {
            "name": "enabled",
            "attribute": true,
            "wrapped": false
          },
          "description": "Признак включенности магазина"
        },
        "name": {
          "type": "string",
          "example": "Самый лучший магазин",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Внешнее название магазина"
        }
      },
      "title": "CrossborderCampaignDto",
      "xml": {
        "name": "campaign",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о кроссбордер-кампании"
    },
    "CrossborderOrganizationInfoDTO": {
      "type": "object",
      "properties": {
        "accountNumber": {
          "type": "string",
          "xml": {
            "name": "accountNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankAddress": {
          "type": "string",
          "xml": {
            "name": "bankAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankName": {
          "type": "string",
          "xml": {
            "name": "bankName",
            "attribute": true,
            "wrapped": false
          }
        },
        "brandName": {
          "type": "string",
          "xml": {
            "name": "brandName",
            "attribute": true,
            "wrapped": false
          }
        },
        "contractCurrency": {
          "type": "string",
          "xml": {
            "name": "contractCurrency",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "RUR",
            "USD",
            "EUR",
            "BYN",
            "KZT",
            "UAH"
          ]
        },
        "legalAddress": {
          "type": "string",
          "xml": {
            "name": "legalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationNameEnglish": {
          "type": "string",
          "xml": {
            "name": "organizationNameEnglish",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationNameOriginal": {
          "type": "string",
          "xml": {
            "name": "organizationNameOriginal",
            "attribute": true,
            "wrapped": false
          }
        },
        "payeeId": {
          "type": "string",
          "xml": {
            "name": "payeeId",
            "attribute": true,
            "wrapped": false
          }
        },
        "paymentMethod": {
          "type": "string",
          "xml": {
            "name": "paymentMethod",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "BANK_ACCOUNT",
            "PAYONEER"
          ]
        },
        "physicalAddress": {
          "type": "string",
          "xml": {
            "name": "physicalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "postcode": {
          "type": "string",
          "xml": {
            "name": "postcode",
            "attribute": true,
            "wrapped": false
          }
        },
        "registrationNumber": {
          "type": "string",
          "xml": {
            "name": "registrationNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "representativeAddress": {
          "type": "string",
          "xml": {
            "name": "representativeAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "representativeFullName": {
          "type": "string",
          "xml": {
            "name": "representativeFullName",
            "attribute": true,
            "wrapped": false
          }
        },
        "representativeIdCardNumber": {
          "type": "string",
          "xml": {
            "name": "representativeIdCardNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "swift": {
          "type": "string",
          "xml": {
            "name": "swift",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "CrossborderOrganizationInfoDTO"
    },
    "CrossborderPartnerApplicationDTO": {
      "type": "object",
      "properties": {
        "documents": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/PartnerDocumentDTO"
          }
        },
        "form": {
          "$ref": "#/definitions/CrossborderPartnerApplicationFormDTO"
        },
        "requestId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "requestId",
            "attribute": true,
            "wrapped": false
          }
        },
        "serviceConditions": {
          "$ref": "#/definitions/CrossborderPartnerServiceConditionsDTO"
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        },
        "validations": {
          "$ref": "#/definitions/ApplicationValidationsDTO"
        }
      },
      "title": "CrossborderPartnerApplicationDTO"
    },
    "CrossborderPartnerApplicationFormDTO": {
      "type": "object",
      "properties": {
        "accountNumber": {
          "type": "string",
          "xml": {
            "name": "accountNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "accountNumberFormat": {
          "type": "string",
          "xml": {
            "name": "accountNumberFormat",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "IBAN",
            "OTHER"
          ]
        },
        "applicantEmail": {
          "type": "string",
          "xml": {
            "name": "applicantEmail",
            "attribute": true,
            "wrapped": false
          }
        },
        "applicantFullName": {
          "type": "string",
          "xml": {
            "name": "applicantFullName",
            "attribute": true,
            "wrapped": false
          }
        },
        "applicantPhone": {
          "$ref": "#/definitions/StrictPartnerPhoneDTO"
        },
        "bankAddress": {
          "type": "string",
          "xml": {
            "name": "bankAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankName": {
          "type": "string",
          "xml": {
            "name": "bankName",
            "attribute": true,
            "wrapped": false
          }
        },
        "brandName": {
          "type": "string",
          "xml": {
            "name": "brandName",
            "attribute": true,
            "wrapped": false
          }
        },
        "companyLegalCity": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "companyLegalCity",
            "attribute": true,
            "wrapped": false
          }
        },
        "companyLegalCountry": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "companyLegalCountry",
            "attribute": true,
            "wrapped": false
          }
        },
        "contractCurrency": {
          "type": "string",
          "xml": {
            "name": "contractCurrency",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "RUR",
            "USD",
            "EUR",
            "BYN",
            "KZT",
            "UAH"
          ]
        },
        "iban": {
          "type": "string",
          "xml": {
            "name": "iban",
            "attribute": true,
            "wrapped": false
          }
        },
        "legalAddress": {
          "type": "string",
          "xml": {
            "name": "legalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationNameEnglish": {
          "type": "string",
          "xml": {
            "name": "organizationNameEnglish",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationNameOriginal": {
          "type": "string",
          "xml": {
            "name": "organizationNameOriginal",
            "attribute": true,
            "wrapped": false
          }
        },
        "payeeId": {
          "type": "string"
        },
        "paymentMethod": {
          "type": "string",
          "enum": [
            "BANK_ACCOUNT",
            "PAYONEER"
          ]
        },
        "physicalAddress": {
          "type": "string",
          "xml": {
            "name": "physicalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "postcode": {
          "type": "string",
          "xml": {
            "name": "postcode",
            "attribute": true,
            "wrapped": false
          }
        },
        "registrationNumber": {
          "type": "string",
          "xml": {
            "name": "registrationNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "representativeAddress": {
          "type": "string",
          "xml": {
            "name": "representativeAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "representativeFullName": {
          "type": "string",
          "xml": {
            "name": "representativeFullName",
            "attribute": true,
            "wrapped": false
          }
        },
        "representativeIdCardNumber": {
          "type": "string",
          "xml": {
            "name": "representativeIdCardNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "swift": {
          "type": "string",
          "xml": {
            "name": "swift",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "CrossborderPartnerApplicationFormDTO"
    },
    "CrossborderPartnerExternalContractsDTO": {
      "type": "object",
      "properties": {
        "generalContractId": {
          "type": "string",
          "xml": {
            "name": "generalContractId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Внешний номер основного договора"
        },
        "subsidiesContractId": {
          "type": "string",
          "xml": {
            "name": "subsidiesContractId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Внешний номер договора на субсидирование"
        }
      },
      "title": "CrossborderPartnerExternalContractsDTO",
      "xml": {
        "name": "crossborderPartnerExternalContracts",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация по внешним номерам договоров красного партнера."
    },
    "CrossborderPartnerServiceConditionsDTO": {
      "type": "object",
      "properties": {
        "deliveryPaymentOwner": {
          "type": "string",
          "xml": {
            "name": "deliveryPaymentOwner",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "MARKET",
            "MERCHANT"
          ]
        },
        "fee": {
          "type": "number",
          "xml": {
            "name": "fee",
            "attribute": true,
            "wrapped": false
          }
        },
        "freeDeliveryThresholdRub": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "freeDeliveryThresholdRub",
            "attribute": true,
            "wrapped": false
          }
        },
        "isPaidDeliveryForPartner": {
          "type": "boolean",
          "xml": {
            "name": "isPaidDeliveryForPartner",
            "attribute": true,
            "wrapped": false
          }
        },
        "marketPriceMultiplier": {
          "type": "number",
          "xml": {
            "name": "marketPriceMultiplier",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "CrossborderPartnerServiceConditionsDTO"
    },
    "CrossborderPayeeDTO": {
      "type": "object",
      "properties": {
        "partnerId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "partnerId",
            "attribute": false,
            "wrapped": false
          },
          "description": "Id партнера, для которого создается получатель."
        },
        "payeeId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "payeeId",
            "attribute": false,
            "wrapped": false
          },
          "description": "Идентификатор получателя платежей"
        },
        "paymentService": {
          "type": "string",
          "xml": {
            "name": "paymentService",
            "attribute": false,
            "wrapped": false
          },
          "description": "Идентификатор платежной системы.",
          "enum": [
            "PAYONEER",
            "PING_PONG",
            "LIAN_LIAN"
          ]
        }
      },
      "title": "CrossborderPayeeDTO",
      "description": "Информация о получателе выплат от платежных систем на Красном маркете."
    },
    "DataCampFeedHistory": {
      "type": "object",
      "properties": {
        "complete": {
          "type": "boolean",
          "xml": {
            "name": "complete",
            "attribute": true,
            "wrapped": false
          },
          "description": "Тип фида: true - комплитный, false - апдейтный"
        },
        "feed_id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "feed_id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор фида"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор истории фида"
        },
        "updateTime": {
          "type": "string",
          "format": "date-time",
          "xml": {
            "name": "updateTime",
            "attribute": true,
            "wrapped": false
          },
          "description": "Дата и время обновления фида в оферном хранилище"
        }
      },
      "title": "DataCampFeedHistory"
    },
    "DataCampFeedHistoryResponse": {
      "type": "object",
      "properties": {
        "history": {
          "type": "array",
          "xml": {
            "name": "history",
            "attribute": true,
            "wrapped": false
          },
          "items": {
            "$ref": "#/definitions/DataCampFeedHistory"
          }
        }
      },
      "title": "DataCampFeedHistoryResponse",
      "xml": {
        "name": "response",
        "attribute": false,
        "wrapped": false
      },
      "description": "Ответ на запрос истории обновления фида в оферном хранилище"
    },
    "DataModel": {
      "type": "object",
      "properties": {
        "datasources": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Entry"
          }
        },
        "suppliers": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Entry"
          }
        }
      },
      "title": "DataModel",
      "xml": {
        "name": "DataModel",
        "attribute": false,
        "wrapped": false
      }
    },
    "DatasourceNameDTO": {
      "type": "object",
      "properties": {
        "internalName": {
          "type": "string",
          "xml": {
            "name": "internalName",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DatasourceNameDTO"
    },
    "DeleteOffersRequest": {
      "type": "object",
      "properties": {
        "offers": {
          "type": "array",
          "xml": {
            "name": "offers",
            "attribute": false,
            "wrapped": false
          },
          "description": "Список идентификаторов оферов для удаления",
          "items": {
            "$ref": "#/definitions/OfferIdentifier"
          }
        }
      },
      "title": "DeleteOffersRequest"
    },
    "DeliveryCostsRequest": {
      "type": "object",
      "properties": {
        "declaredValuePercent": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "declaredValuePercent",
            "attribute": true,
            "wrapped": false
          }
        },
        "deliveryServiceId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "deliveryServiceId",
            "attribute": true,
            "wrapped": false
          }
        },
        "deliveryType": {
          "type": "string",
          "xml": {
            "name": "deliveryType",
            "attribute": true,
            "wrapped": false
          }
        },
        "height": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "height",
            "attribute": true,
            "wrapped": false
          }
        },
        "length": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "length",
            "attribute": true,
            "wrapped": false
          }
        },
        "orderId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "orderId",
            "attribute": true,
            "wrapped": false
          }
        },
        "orderTotal": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "orderTotal",
            "attribute": true,
            "wrapped": false
          }
        },
        "regionFrom": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "regionFrom",
            "attribute": true,
            "wrapped": false
          }
        },
        "regionTo": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "regionTo",
            "attribute": true,
            "wrapped": false
          }
        },
        "warehouse-id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "warehouse-id",
            "attribute": true,
            "wrapped": false
          }
        },
        "weight": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "weight",
            "attribute": true,
            "wrapped": false
          }
        },
        "width": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "width",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryCostsRequest",
      "xml": {
        "name": "DeliveryCostsRequest",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryCostsRequestOld": {
      "type": "object",
      "properties": {
        "accessed-value": {
          "type": "number",
          "xml": {
            "name": "accessed-value",
            "attribute": true,
            "wrapped": false
          }
        },
        "delivery-service-id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "delivery-service-id",
            "attribute": true,
            "wrapped": false
          }
        },
        "delivery-type": {
          "type": "string",
          "xml": {
            "name": "delivery-type",
            "attribute": true,
            "wrapped": false
          }
        },
        "height": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "height",
            "attribute": true,
            "wrapped": false
          }
        },
        "length": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "length",
            "attribute": true,
            "wrapped": false
          }
        },
        "region-from": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "region-from",
            "attribute": true,
            "wrapped": false
          }
        },
        "region-to": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "region-to",
            "attribute": true,
            "wrapped": false
          }
        },
        "warehouse-id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "warehouse-id",
            "attribute": true,
            "wrapped": false
          }
        },
        "weight": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "weight",
            "attribute": true,
            "wrapped": false
          }
        },
        "width": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "width",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryCostsRequestOld",
      "xml": {
        "name": "DeliveryCostsRequestOld",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryCostsResponse": {
      "type": "object",
      "properties": {
        "deliveryCost": {
          "type": "number",
          "xml": {
            "name": "deliveryCost",
            "attribute": true,
            "wrapped": false
          }
        },
        "fullCost": {
          "type": "number",
          "xml": {
            "name": "fullCost",
            "attribute": true,
            "wrapped": false
          }
        },
        "insuranceCost": {
          "type": "number",
          "xml": {
            "name": "insuranceCost",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryCostsResponse",
      "xml": {
        "name": "DeliveryCostsResponse",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryCostsResponseOld": {
      "type": "object",
      "properties": {
        "delivery-cost": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/DeliveryCostsResponseOldElement"
          }
        }
      },
      "title": "DeliveryCostsResponseOld",
      "xml": {
        "name": "DeliveryCostsResponseOld",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryCostsResponseOldElement": {
      "type": "object",
      "properties": {
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          }
        },
        "value": {
          "type": "number",
          "xml": {
            "name": "value",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryCostsResponseOldElement",
      "xml": {
        "name": "DeliveryCostsResponseOldElement",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryPartnerApplicationDTO": {
      "type": "object",
      "properties": {
        "documents": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/PartnerDocumentDTO"
          }
        },
        "form": {
          "$ref": "#/definitions/DeliveryPartnerApplicationFormDTO"
        },
        "requestId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "requestId",
            "attribute": true,
            "wrapped": false
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        },
        "validations": {
          "$ref": "#/definitions/ApplicationValidationsDTO"
        }
      },
      "title": "DeliveryPartnerApplicationDTO"
    },
    "DeliveryPartnerApplicationFormDTO": {
      "type": "object",
      "properties": {
        "accountant": {
          "xml": {
            "name": "accountant",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/DeliveryPartnerApplicationPersonDTO"
        },
        "inn": {
          "type": "string",
          "xml": {
            "name": "inn",
            "attribute": true,
            "wrapped": false
          }
        },
        "kpp": {
          "type": "string",
          "xml": {
            "name": "kpp",
            "attribute": true,
            "wrapped": false
          }
        },
        "legalAddress": {
          "type": "string",
          "xml": {
            "name": "legalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "ogrn": {
          "type": "string",
          "xml": {
            "name": "ogrn",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationName": {
          "type": "string",
          "xml": {
            "name": "organizationName",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationType": {
          "type": "string",
          "xml": {
            "name": "organizationType",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "NONE",
            "OOO",
            "ZAO",
            "IP",
            "CHP",
            "OTHER",
            "OAO"
          ]
        },
        "paymentInfo": {
          "xml": {
            "name": "paymentInfo",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/DeliveryPartnerApplicationPaymentInfoDTO"
        },
        "postAddress": {
          "type": "string",
          "xml": {
            "name": "postAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "postAddressEqualsToLegal": {
          "type": "boolean",
          "xml": {
            "name": "postAddressEqualsToLegal",
            "attribute": true,
            "wrapped": false
          }
        },
        "postcode": {
          "type": "string",
          "xml": {
            "name": "postcode",
            "attribute": true,
            "wrapped": false
          }
        },
        "workSchedule": {
          "type": "string",
          "xml": {
            "name": "workSchedule",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryPartnerApplicationFormDTO"
    },
    "DeliveryPartnerApplicationPaymentInfoDTO": {
      "type": "object",
      "properties": {
        "bankAccount": {
          "type": "string",
          "xml": {
            "name": "bankAccount",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankBik": {
          "type": "string",
          "xml": {
            "name": "bankBik",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankCorrAccount": {
          "type": "string",
          "xml": {
            "name": "bankCorrAccount",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankName": {
          "type": "string",
          "xml": {
            "name": "bankName",
            "attribute": true,
            "wrapped": false
          }
        },
        "email": {
          "type": "string",
          "xml": {
            "name": "email",
            "attribute": true,
            "wrapped": false
          }
        },
        "firstName": {
          "type": "string",
          "xml": {
            "name": "firstName",
            "attribute": true,
            "wrapped": false
          }
        },
        "lastName": {
          "type": "string",
          "xml": {
            "name": "lastName",
            "attribute": true,
            "wrapped": false
          }
        },
        "middleName": {
          "type": "string",
          "xml": {
            "name": "middleName",
            "attribute": true,
            "wrapped": false
          }
        },
        "personFormation": {
          "type": "string",
          "xml": {
            "name": "personFormation",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "ARTICLES_OF_ASSOCIATION",
            "ORDER",
            "POWER_OF_ATTORNEY"
          ]
        },
        "personPosition": {
          "type": "string",
          "xml": {
            "name": "personPosition",
            "attribute": true,
            "wrapped": false
          }
        },
        "phone": {
          "type": "string",
          "xml": {
            "name": "phone",
            "attribute": true,
            "wrapped": false
          }
        },
        "taxSystem": {
          "type": "string",
          "xml": {
            "name": "taxSystem",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "0",
            "1",
            "2",
            "3",
            "4",
            "5"
          ]
        }
      },
      "title": "DeliveryPartnerApplicationPaymentInfoDTO"
    },
    "DeliveryPartnerApplicationPersonDTO": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string",
          "xml": {
            "name": "email",
            "attribute": true,
            "wrapped": false
          }
        },
        "firstName": {
          "type": "string",
          "xml": {
            "name": "firstName",
            "attribute": true,
            "wrapped": false
          }
        },
        "lastName": {
          "type": "string",
          "xml": {
            "name": "lastName",
            "attribute": true,
            "wrapped": false
          }
        },
        "middleName": {
          "type": "string",
          "xml": {
            "name": "middleName",
            "attribute": true,
            "wrapped": false
          }
        },
        "phoneNumber": {
          "type": "string",
          "xml": {
            "name": "phoneNumber",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryPartnerApplicationPersonDTO"
    },
    "DeliveryPartnerStateDTO": {
      "type": "object",
      "properties": {
        "campaignId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "campaignId",
            "attribute": true,
            "wrapped": false
          }
        },
        "domain": {
          "type": "string",
          "xml": {
            "name": "domain",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        }
      },
      "title": "DeliveryPartnerStateDTO",
      "xml": {
        "name": "partner",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryPartnersDTO": {
      "type": "object",
      "properties": {
        "partners": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/DeliveryPartnerStateDTO"
          }
        }
      },
      "title": "DeliveryPartnersDTO",
      "xml": {
        "name": "partners",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryServiceInfo": {
      "type": "object",
      "properties": {
        "description": {
          "type": "string"
        },
        "humanReadableId": {
          "type": "string"
        },
        "id": {
          "type": "integer",
          "format": "int64"
        },
        "isCommon": {
          "type": "boolean"
        },
        "isGlobal": {
          "type": "boolean"
        },
        "logo": {
          "type": "string"
        },
        "marketDeliveryForAllShops": {
          "type": "boolean"
        },
        "marketDeliveryForShops": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "marketDeliveryInletRegions": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "marketDeliveryOutletRegions": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "marketDeliveryShipmentTypes": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "INTAKE",
              "SELF_EXPORT"
            ]
          }
        },
        "marketStatus": {
          "type": "string",
          "enum": [
            "OFF",
            "ON",
            "PAUSE"
          ]
        },
        "name": {
          "type": "string"
        },
        "pickupAvailable": {
          "type": "boolean"
        },
        "regions": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "type": {
          "type": "string",
          "enum": [
            "CARRIER",
            "SORTING_CENTER",
            "FULFILLMENT"
          ]
        },
        "url": {
          "type": "string"
        }
      },
      "title": "DeliveryServiceInfo",
      "xml": {
        "name": "DeliveryServiceInfo",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryServiceInfoShort": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64"
        },
        "name": {
          "type": "string"
        }
      },
      "title": "DeliveryServiceInfoShort",
      "xml": {
        "name": "DeliveryServiceInfoShort",
        "attribute": false,
        "wrapped": false
      }
    },
    "DeliveryServiceRequestDTO": {
      "type": "object",
      "properties": {
        "courierDeliveryStrategy": {
          "type": "string",
          "description": "Стратегия расчета СиС курьерской доставки. Дефолтное значение UNKNOWN_COST_TIME",
          "enum": [
            "UNKNOWN_COST_TIME (есть доставка. СиС не заданы)",
            "AUTO_CALCULATED (есть доставка. СиС рассчитываются автоматически)"
          ]
        },
        "pickupDeliveryStrategy": {
          "type": "string",
          "description": "Стратегия расчета СиС доставки в пвз. Дефолтное значение UNKNOWN_COST_TIME",
          "enum": [
            "UNKNOWN_COST_TIME (есть доставка. СиС не заданы)",
            "AUTO_CALCULATED (есть доставка. СиС рассчитываются автоматически)",
            "NO_DELIVERY (нет доставки в региональную группу)"
          ]
        }
      },
      "title": "DeliveryServiceRequestDTO",
      "xml": {
        "name": "deliveryService",
        "attribute": false,
        "wrapped": false
      },
      "description": "Настройки СД для сохранения"
    },
    "DeliveryServiceResponseDTO": {
      "type": "object",
      "properties": {
        "checked": {
          "type": "boolean",
          "xml": {
            "name": "checked",
            "attribute": true,
            "wrapped": false
          }
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "isCommon": {
          "type": "boolean",
          "xml": {
            "name": "isCommon",
            "attribute": true,
            "wrapped": false
          }
        },
        "isGlobal": {
          "type": "boolean",
          "xml": {
            "name": "isGlobal",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DeliveryServiceResponseDTO",
      "xml": {
        "name": "deliveryService",
        "attribute": false,
        "wrapped": false
      }
    },
    "DetailedCpaOrderResponse": {
      "type": "object",
      "properties": {
        "cpaReport": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/DetailedCpaOrders"
          }
        }
      },
      "title": "DetailedCpaOrderResponse",
      "xml": {
        "name": "detailedCpaOrderResponse",
        "attribute": false,
        "wrapped": false
      }
    },
    "DetailedCpaOrders": {
      "type": "object",
      "properties": {
        "acceptedCount": {
          "type": "integer",
          "format": "int32"
        },
        "date": {
          "type": "string",
          "format": "date-time"
        },
        "deliveryCount": {
          "type": "integer",
          "format": "int32"
        },
        "money": {
          "type": "integer",
          "format": "int32"
        }
      },
      "title": "DetailedCpaOrders",
      "xml": {
        "name": "cpaOrderReport",
        "attribute": false,
        "wrapped": false
      }
    },
    "DynamicPriceControlConfigDTO": {
      "type": "object",
      "properties": {
        "maxAllowedDiscountPercent": {
          "type": "number",
          "xml": {
            "name": "maxAllowedDiscountPercent",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "DynamicPriceControlConfigDTO"
    },
    "EditFeedDTO": {
      "type": "object",
      "properties": {
        "login": {
          "type": "string"
        },
        "password": {
          "type": "string"
        },
        "url": {
          "type": "string"
        },
        "validation_id": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "EditFeedDTO",
      "xml": {
        "name": "editFeedDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "Entry": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64"
        },
        "name": {
          "type": "string"
        }
      },
      "title": "Entry"
    },
    "ExistingOffersDTO": {
      "type": "object",
      "properties": {
        "acceptedOffers": {
          "type": "integer",
          "format": "int64"
        },
        "moderationOffers": {
          "type": "integer",
          "format": "int64"
        },
        "rejectedOffers": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "ExistingOffersDTO",
      "xml": {
        "name": "existingOrders",
        "attribute": false,
        "wrapped": false
      }
    },
    "Feature": {
      "type": "object",
      "properties": {
        "defaults": {
          "$ref": "#/definitions/BaseSecurable"
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "operations": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "override": {
          "type": "boolean"
        },
        "params": {
          "type": "object"
        },
        "roles": {
          "$ref": "#/definitions/SecurityRule"
        },
        "states": {
          "$ref": "#/definitions/SecurityRule"
        }
      },
      "title": "Feature"
    },
    "FeatureCutoffInfoDto": {
      "type": "object",
      "properties": {
        "reason": {
          "type": "string",
          "xml": {
            "name": "reason",
            "attribute": true,
            "wrapped": false
          },
          "description": "Причина отключения фичи, полученная из АБО.",
          "enum": [
            "PERCENT_CANCELLED_ORDERS",
            "PERCENT_ARBITRAGE_USER_WIN",
            "MASS_CART_DIFF",
            "MANUAL",
            "PINGER_API",
            "TEST_ORDER",
            "PERCENT_REFUND",
            "RECHECK"
          ]
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "description": "Тип отключения."
        }
      },
      "title": "FeatureCutoffInfoDto",
      "description": "Представление открытых отключений программы."
    },
    "FeatureCutoffMessageDto": {
      "type": "object",
      "properties": {
        "body": {
          "type": "string",
          "description": "Тело сообщения"
        },
        "featureType": {
          "type": "string",
          "xml": {
            "name": "featureType",
            "attribute": true,
            "wrapped": false
          },
          "description": "Тип программы",
          "enum": [
            "PROMO_CPC",
            "SUBSIDIES",
            "FULFILLMENT",
            "CPA_20",
            "RED_MARKET",
            "DROPSHIP",
            "SHOP_LOGO",
            "PREPAY",
            "CASHBACK",
            "CROSSDOCK",
            "MARKETPLACE",
            "FMCG_PARTNER",
            "DAAS",
            "ALCOHOL",
            "CUT_PRICE",
            "CREDITS"
          ]
        },
        "shopId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "shopId",
            "attribute": true,
            "wrapped": false
          },
          "description": "ID магазина/поставщика"
        },
        "subject": {
          "type": "string",
          "description": "Заголовок сообщения"
        }
      },
      "title": "FeatureCutoffMessageDto",
      "description": "Уведомление по статусу программы"
    },
    "FeedInfoDTO": {
      "type": "object",
      "properties": {
        "feed": {
          "description": "Информация о текущем фиде(прайс-листе) поставщика",
          "$ref": "#/definitions/AssortmentFeedDTO"
        }
      },
      "title": "FeedInfoDTO",
      "xml": {
        "name": "assortmentInfoDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о текущем фиде(прайс-листе) поставщика"
    },
    "FeedLogCodeStats": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string"
        },
        "errorsCount": {
          "type": "integer",
          "format": "int32"
        },
        "examples": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/FeedLogLine"
          }
        },
        "subcode": {
          "type": "string"
        }
      },
      "title": "FeedLogCodeStats"
    },
    "FeedLogLine": {
      "type": "object",
      "properties": {
        "arguments": {
          "type": "object",
          "additionalProperties": {
            "type": "string"
          }
        },
        "code": {
          "type": "string"
        },
        "humanReadable": {
          "type": "string"
        },
        "lineNo": {
          "type": "integer",
          "format": "int32"
        },
        "positionNumber": {
          "type": "integer",
          "format": "int32"
        },
        "subcode": {
          "type": "string"
        }
      },
      "title": "FeedLogLine"
    },
    "FeedLogStats": {
      "type": "object",
      "properties": {
        "arguments": {
          "type": "object",
          "additionalProperties": {
            "type": "string"
          }
        },
        "code": {
          "type": "string"
        }
      },
      "title": "FeedLogStats"
    },
    "FeedResourceDTO": {
      "type": "object",
      "properties": {
        "credentials": {
          "xml": {
            "name": "credentials",
            "attribute": true,
            "wrapped": false
          },
          "description": "Данные авторизация, для доступа к ресурсу",
          "$ref": "#/definitions/CredentialsDTO"
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "FeedResourceDTO",
      "description": "Информация о ресурсе к которому, осуществляется доступ по URL"
    },
    "FeedSuggestDTO": {
      "type": "object",
      "properties": {
        "suggestId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "FeedSuggestDTO",
      "xml": {
        "name": "feedSuggestDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "FeedSuggestInfoDTO": {
      "type": "object",
      "properties": {
        "declinedOffers": {
          "type": "integer",
          "format": "int64"
        },
        "enrichedFile": {
          "$ref": "#/definitions/FileUploadDTO"
        },
        "feedSuggestResult": {
          "type": "string",
          "enum": [
            "UNKNOWN",
            "PROCESSING",
            "OK",
            "WARNING",
            "ERROR"
          ]
        },
        "marketSku": {
          "type": "boolean"
        },
        "prices": {
          "type": "boolean"
        },
        "processedOffers": {
          "type": "integer",
          "format": "int64"
        },
        "totalOffers": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "FeedSuggestInfoDTO",
      "xml": {
        "name": "FeedSuggestInfoDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "FeedSummaryDTO": {
      "type": "object",
      "properties": {
        "offerCount": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "offerCount",
            "attribute": true,
            "wrapped": false
          }
        },
        "upload": {
          "$ref": "#/definitions/UploadFeedDTO"
        }
      },
      "title": "FeedSummaryDTO"
    },
    "FeedValidationDTO": {
      "type": "object",
      "properties": {
        "login": {
          "type": "string"
        },
        "password": {
          "type": "string"
        },
        "type": {
          "type": "string",
          "enum": [
            "TOLERANT",
            "FULL",
            "NULL",
            "SUPPLIER_MAPPING",
            "SUPPLIER_PRICES",
            "SUPPLIER_MAPPING_WITH_PRICES",
            "CHINA_GOODS_APP_FEED",
            "FMCG_APP_FEED"
          ]
        },
        "url": {
          "type": "string"
        },
        "validation_id": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "FeedValidationDTO",
      "xml": {
        "name": "feedValidation",
        "attribute": false,
        "wrapped": false
      }
    },
    "FeedValidationInfo": {
      "type": "object",
      "properties": {
        "feedTemplate": {
          "type": "string",
          "enum": [
            "ru.yandex.market.common.excel.MarketTemplate@489b0369[categoryCode=1,category=NONE,templateIds=[ru.yandex.market.common.excel.CodeConfig@2016896e]]",
            "ru.yandex.market.common.excel.MarketTemplate@2d279cd2[categoryCode=2,category=COMMON,templateIds=[ru.yandex.market.common.excel.CodeConfig@5773ad4a, ru.yandex.market.common.excel.CodeConfig@47636c86]]",
            "ru.yandex.market.common.excel.MarketTemplate@532ad0fd[categoryCode=3,category=BOOKS,templateIds=[ru.yandex.market.common.excel.CodeConfig@427a0746, ru.yandex.market.common.excel.CodeConfig@8a9ebbe]]",
            "ru.yandex.market.common.excel.MarketTemplate@5472a171[categoryCode=4,category=MUSIC,templateIds=[ru.yandex.market.common.excel.CodeConfig@515a3e3a, ru.yandex.market.common.excel.CodeConfig@43885360]]",
            "ru.yandex.market.common.excel.MarketTemplate@4bbb4b4[categoryCode=5,category=SUPPLIER,templateIds=[ru.yandex.market.common.excel.CodeConfig@4503f47b]]",
            "ru.yandex.market.common.excel.MarketTemplate@45a8b90f[categoryCode=6,category=ALCOHOL,templateIds=[ru.yandex.market.common.excel.CodeConfig@44baf189]]"
          ]
        },
        "finishDate": {
          "type": "string",
          "format": "date-time"
        },
        "id": {
          "type": "integer",
          "format": "int64"
        },
        "request": {
          "$ref": "#/definitions/FeedValidationRequest"
        },
        "requestDate": {
          "type": "string",
          "format": "date-time"
        },
        "result": {
          "type": "string",
          "enum": [
            "UNKNOWN",
            "OK",
            "WARNING",
            "ERROR"
          ]
        }
      },
      "title": "FeedValidationInfo"
    },
    "FeedValidationParsedDTO": {
      "type": "object",
      "properties": {
        "feedValidationInfo": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/FeedValidationInfo"
          }
        },
        "parsedLog": {
          "$ref": "#/definitions/ParseLogParsed"
        }
      },
      "title": "FeedValidationParsedDTO",
      "xml": {
        "name": "feedValidation",
        "attribute": false,
        "wrapped": false
      }
    },
    "FeedValidationRequest": {
      "type": "object",
      "properties": {
        "connectionTimeout": {
          "type": "integer",
          "format": "int32"
        },
        "datasourceId": {
          "type": "integer",
          "format": "int64"
        },
        "feedValidationType": {
          "type": "string",
          "enum": [
            "TOLERANT",
            "FULL",
            "NULL",
            "SUPPLIER_MAPPING",
            "SUPPLIER_PRICES",
            "SUPPLIER_MAPPING_WITH_PRICES",
            "CHINA_GOODS_APP_FEED",
            "FMCG_APP_FEED"
          ]
        },
        "login": {
          "type": "string"
        },
        "password": {
          "type": "string"
        },
        "readTimeout": {
          "type": "integer",
          "format": "int32"
        },
        "uploadDate": {
          "type": "string",
          "format": "date-time"
        },
        "uploadId": {
          "type": "integer",
          "format": "int64"
        },
        "uploadName": {
          "type": "string"
        },
        "uploadSize": {
          "type": "integer",
          "format": "int32"
        },
        "url": {
          "type": "string"
        }
      },
      "title": "FeedValidationRequest"
    },
    "FileUploadDTO": {
      "type": "object",
      "properties": {
        "fileName": {
          "type": "string"
        },
        "fileSize": {
          "type": "integer",
          "format": "int64",
          "description": "Размер файла в байтах"
        },
        "uploadDateTime": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "uploadDateTime",
            "attribute": false,
            "wrapped": false
          }
        }
      },
      "title": "FileUploadDTO",
      "xml": {
        "name": "fileUploadDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о файле, загруженном на сервер"
    },
    "FmcgCampaignDTO": {
      "type": "object",
      "properties": {
        "campaignId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "campaignId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id кампании"
        },
        "enabled": {
          "type": "boolean",
          "xml": {
            "name": "enabled",
            "attribute": true,
            "wrapped": false
          },
          "description": "Признак включенности магазина (из параметра IS_ENABLED)"
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Внешнее название магазина (из параметра SHOP_NAME)"
        }
      },
      "title": "FmcgCampaignDTO",
      "xml": {
        "name": "campaign",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о FMCG-кампании"
    },
    "FmcgPartnerApplicationDTO": {
      "type": "object",
      "properties": {
        "documents": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/PartnerDocumentDTO"
          }
        },
        "form": {
          "$ref": "#/definitions/FmcgPartnerApplicationFormDTO"
        },
        "requestId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "requestId",
            "attribute": true,
            "wrapped": false
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        }
      },
      "title": "FmcgPartnerApplicationDTO"
    },
    "FmcgPartnerApplicationFormDTO": {
      "type": "object",
      "properties": {
        "legalAddress": {
          "type": "string",
          "xml": {
            "name": "legalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "ogrn": {
          "type": "string",
          "xml": {
            "name": "ogrn",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationName": {
          "type": "string",
          "xml": {
            "name": "organizationName",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationType": {
          "type": "string",
          "xml": {
            "name": "organizationType",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "NONE",
            "OOO",
            "ZAO",
            "IP",
            "CHP",
            "OTHER",
            "OAO"
          ]
        },
        "physicalAddress": {
          "type": "string",
          "xml": {
            "name": "physicalAddress",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "FmcgPartnerApplicationFormDTO"
    },
    "FmcgShoppingCartDTO": {
      "type": "object",
      "properties": {
        "requestType": {
          "type": "string",
          "xml": {
            "name": "requestType",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "GET",
            "POST"
          ]
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "FmcgShoppingCartDTO"
    },
    "FulfillmentServiceDTO": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "warehouse": {
          "$ref": "#/definitions/WarehouseDTO"
        }
      },
      "title": "FulfillmentServiceDTO"
    },
    "GenericResponse": {
      "type": "object",
      "properties": {
        "status": {
          "type": "string",
          "description": "Статус ответа ручки (значение ОК)",
          "enum": [
            "OK"
          ]
        }
      },
      "title": "GenericResponse",
      "xml": {
        "name": "GenericResponse",
        "attribute": false,
        "wrapped": false
      },
      "description": "Ответ бэкенда не несущий смысловой нагрузки"
    },
    "GeoInfo": {
      "type": "object",
      "properties": {
        "gpsCoordinates": {
          "xml": {
            "name": "gpsCoordinates",
            "attribute": false,
            "wrapped": false
          },
          "description": "GPS-координаты",
          "$ref": "#/definitions/Coordinates"
        },
        "gpsCoords": {
          "type": "string",
          "xml": {
            "name": "gpsCoords",
            "attribute": false,
            "wrapped": false
          },
          "description": "Текстовое представление GPS-координат. Координаты GPS точки продаж, может быть null, если координаты еще не определены"
        },
        "regionId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "regionId",
            "attribute": false,
            "wrapped": false
          },
          "description": "Идетификатор региона, к которому принадлежит точка"
        }
      },
      "title": "GeoInfo",
      "description": "Информация о географическом положении точки"
    },
    "Goal": {
      "type": "object",
      "properties": {
        "counterId": {
          "type": "string",
          "xml": {
            "name": "counterId",
            "attribute": true,
            "wrapped": false
          }
        },
        "goalId": {
          "type": "string",
          "xml": {
            "name": "goalId",
            "attribute": true,
            "wrapped": false
          }
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "shopId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "shopId",
            "attribute": true,
            "wrapped": false
          }
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "CHECKOUT"
          ]
        }
      },
      "title": "Goal"
    },
    "GuaranteeLetterInfoDTO": {
      "type": "object",
      "properties": {
        "originalFilename": {
          "type": "string",
          "xml": {
            "name": "originalFilename",
            "attribute": true,
            "wrapped": false
          },
          "description": "Оригинальное имя файла"
        },
        "partnerId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "partnerId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор партнера"
        },
        "updateTime": {
          "type": "string",
          "format": "date-time",
          "xml": {
            "name": "updateTime",
            "attribute": true,
            "wrapped": false
          },
          "description": "Время последнего обновления файла в хранилище"
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          },
          "description": "URL гарантийного письма в хранилище"
        }
      },
      "title": "GuaranteeLetterInfoDTO",
      "xml": {
        "name": "guaranteeLetterInfo",
        "attribute": false,
        "wrapped": false
      }
    },
    "HiddenOfferDto": {
      "type": "object",
      "properties": {
        "categoryId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "categoryId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id категории"
        },
        "categoryName": {
          "type": "string",
          "xml": {
            "name": "categoryName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Имя категории"
        },
        "cmId": {
          "type": "string",
          "xml": {
            "name": "cmId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Classifier Magic Id"
        },
        "details": {
          "xml": {
            "name": "details",
            "attribute": true,
            "wrapped": false
          },
          "description": "Детали скрытия",
          "$ref": "#/definitions/JsonNode"
        },
        "hidingTimestamp": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "hidingTimestamp",
            "attribute": true,
            "wrapped": false
          },
          "description": "Время скрытия оффера"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id оффера"
        },
        "marketSku": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "marketSku",
            "attribute": true,
            "wrapped": false
          },
          "description": "Маркетный sku"
        },
        "offerId": {
          "type": "string",
          "xml": {
            "name": "offerId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id оффера из фида для магазинов или shop_sku для поставщиков"
        },
        "offerName": {
          "type": "string",
          "xml": {
            "name": "offerName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Имя оффера"
        },
        "quantity": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "quantity",
            "attribute": true,
            "wrapped": false
          },
          "description": "Количество доступных на складе товаров"
        },
        "reason": {
          "type": "string",
          "xml": {
            "name": "reason",
            "attribute": true,
            "wrapped": false
          },
          "description": "Причина скрытия"
        },
        "source": {
          "type": "string",
          "xml": {
            "name": "source",
            "attribute": true,
            "wrapped": false
          },
          "description": "Источник скрытия оффера",
          "enum": [
            "ABO",
            "PARTNER_API",
            "INDEXER",
            "MDM"
          ]
        },
        "subreason": {
          "type": "string",
          "xml": {
            "name": "subreason",
            "attribute": true,
            "wrapped": false
          },
          "description": "Допольнительная информация по причине скрытия"
        },
        "timeout": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "timeout",
            "attribute": true,
            "wrapped": false
          },
          "description": "Время в миллисекундах, когда оффер снова станет видимым, заполняктся для предложений, скрытых через partner-api."
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          },
          "description": "Url оффера"
        }
      },
      "title": "HiddenOfferDto",
      "description": "Информация по скрытому офферу"
    },
    "HiddenOffersDto": {
      "type": "object",
      "properties": {
        "count": {
          "type": "integer",
          "format": "int32",
          "description": "Количество офферов с учетом фильтра"
        },
        "offers": {
          "type": "array",
          "xml": {
            "name": "offer",
            "attribute": true,
            "wrapped": false
          },
          "description": "Скрытые офферы",
          "items": {
            "$ref": "#/definitions/HiddenOfferDto"
          }
        },
        "totalCount": {
          "type": "integer",
          "format": "int32",
          "description": "Количество офферов с учетом фильтра только по магазину"
        }
      },
      "title": "HiddenOffersDto",
      "xml": {
        "name": "HiddenOffersDto",
        "attribute": false,
        "wrapped": false
      },
      "description": "Список скрытых офферов"
    },
    "HidingDetails": {
      "type": "object",
      "title": "HidingDetails"
    },
    "HighlightedTextDTO": {
      "type": "object",
      "properties": {
        "raw": {
          "type": "string",
          "xml": {
            "name": "raw",
            "attribute": true,
            "wrapped": false
          },
          "description": "RAW"
        }
      },
      "title": "HighlightedTextDTO"
    },
    "ImageVersionDTO": {
      "type": "object",
      "properties": {
        "height": {
          "type": "integer",
          "format": "int32"
        },
        "isRetina": {
          "type": "boolean"
        },
        "url": {
          "type": "string"
        },
        "width": {
          "type": "integer",
          "format": "int32"
        }
      },
      "title": "ImageVersionDTO"
    },
    "IndexerHidingDetailsDto": {
      "type": "object",
      "properties": {
        "details": {
          "xml": {
            "name": "details",
            "attribute": true,
            "wrapped": false
          },
          "description": "Детали скрытия",
          "$ref": "#/definitions/JsonNode"
        },
        "errorCode": {
          "type": "string",
          "xml": {
            "name": "errorCode",
            "attribute": true,
            "wrapped": false
          },
          "description": "Код ошибки"
        }
      },
      "title": "IndexerHidingDetailsDto",
      "xml": {
        "name": "IndexerHidingDetailsDto",
        "attribute": false,
        "wrapped": false
      },
      "description": "Детали скрытия оффера от Индексатора"
    },
    "IntakeCost": {
      "type": "object",
      "properties": {
        "billed": {
          "type": "boolean"
        },
        "cost": {
          "type": "number"
        }
      },
      "title": "IntakeCost",
      "xml": {
        "name": "IntakeCost",
        "attribute": false,
        "wrapped": false
      }
    },
    "JsonNode": {
      "type": "object",
      "title": "JsonNode"
    },
    "LocalTime": {
      "type": "object",
      "title": "LocalTime"
    },
    "MappedMarketSku": {
      "type": "object",
      "properties": {
        "marketSku": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "marketSku",
            "attribute": true,
            "wrapped": false
          },
          "description": "Непосредственное значение SKU как целого числа"
        },
        "shopSkus": {
          "type": "array",
          "xml": {
            "name": "shopSkus",
            "attribute": false,
            "wrapped": true
          },
          "description": "Все Shop-SKU привязанные к данному маркетному SKU",
          "items": {
            "$ref": "#/definitions/ModeratedShopSkuDTO"
          }
        }
      },
      "title": "MappedMarketSku",
      "xml": {
        "name": "marketSku",
        "attribute": false,
        "wrapped": false
      },
      "description": "SKU используемое маркетом (Market-SKU) привязанное к каким-либо SKU магазинов/поставщиков"
    },
    "MappedShopSku": {
      "type": "object",
      "properties": {
        "availability": {
          "type": "string",
          "xml": {
            "name": "availability",
            "attribute": true,
            "wrapped": false
          },
          "description": "Сатус активности офера",
          "enum": [
            "ACTIVE",
            "INACTIVE",
            "DELISTED"
          ]
        },
        "barcodes": {
          "type": "array",
          "xml": {
            "name": "barcodes",
            "attribute": false,
            "wrapped": true
          },
          "description": "Штрихкоды, которые могут быть у товара",
          "items": {
            "type": "string"
          }
        },
        "brand": {
          "type": "string",
          "xml": {
            "name": "brand",
            "attribute": true,
            "wrapped": false
          },
          "description": "Торговая марка товара"
        },
        "categoryName": {
          "type": "string",
          "xml": {
            "name": "categoryName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Категория товара"
        },
        "contentProcessingTaskId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "contentProcessingTaskId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ид задачи обработки контента"
        },
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание офера"
        },
        "mappings": {
          "xml": {
            "name": "mappings",
            "attribute": false,
            "wrapped": false
          },
          "description": "Статус модерация",
          "$ref": "#/definitions/ShopSkuMappingDTO"
        },
        "masterData": {
          "xml": {
            "name": "masterData",
            "attribute": false,
            "wrapped": false
          },
          "description": "Мастер данные, то что нам нужно собирать по закону об агрегаторах",
          "$ref": "#/definitions/MasterData"
        },
        "offerProcessingComments": {
          "type": "array",
          "xml": {
            "name": "offerProcessingComments",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий к статусу размещения офера",
          "items": {
            "$ref": "#/definitions/UserMessageDTO"
          }
        },
        "offerProcessingStatus": {
          "type": "string",
          "xml": {
            "name": "offerProcessingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус размещения офера",
          "enum": [
            "UNKNOWN",
            "READY",
            "IN_WORK",
            "NEED_INFO",
            "NEED_MAPPING",
            "NEED_CONTENT",
            "SUSPENDED",
            "CONTENT_PROCESSING",
            "REJECTED"
          ]
        },
        "shopSku": {
          "type": "string",
          "xml": {
            "name": "shopSku",
            "attribute": true,
            "wrapped": false
          },
          "description": "Непосредственное строковое значение SKU"
        },
        "title": {
          "type": "string",
          "xml": {
            "name": "title",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название офера"
        },
        "urls": {
          "type": "array",
          "xml": {
            "name": "urls",
            "attribute": false,
            "wrapped": true
          },
          "description": "Урл товара на сайте",
          "items": {
            "type": "string"
          }
        },
        "vendorCode": {
          "type": "string",
          "xml": {
            "name": "vendorCode",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор товара от производителя"
        }
      },
      "title": "MappedShopSku",
      "description": "SKU используемое поставщиком/магазином (Shop-SKU) в месте привязанными Market SKU."
    },
    "MarketSku": {
      "type": "object",
      "properties": {
        "marketSku": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "marketSku",
            "attribute": true,
            "wrapped": false
          },
          "description": "Непосредственное значение SKU как целого числа"
        }
      },
      "title": "MarketSku",
      "description": "SKU используемое маркетом (Market-SKU) привязанное к каким-либо SKU магазинов/поставщиков"
    },
    "MarketToken": {
      "type": "object",
      "properties": {
        "applicationId": {
          "type": "string",
          "description": "Идентификатор сервиса, к которому осуществляется доступ посредством токена"
        },
        "expireDate": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "expireDate",
            "attribute": false,
            "wrapped": false
          }
        },
        "key": {
          "type": "string",
          "description": "Ключ токена"
        },
        "userId": {
          "type": "integer",
          "format": "int64",
          "description": "Идентификатор пользователя, которому принадлежит токен"
        }
      },
      "title": "MarketToken",
      "description": "Токен для доступа к приложениям маркета"
    },
    "MasterData": {
      "type": "object",
      "properties": {
        "boxCount": {
          "xml": {
            "name": "boxCount",
            "attribute": true,
            "wrapped": false
          },
          "description": "Количество слкладских мест занимает товар",
          "$ref": "#/definitions/OptionalInt"
        },
        "deliveryDurationDays": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "deliveryDurationDays",
            "attribute": true,
            "wrapped": false
          },
          "description": "Срок поставки"
        },
        "guaranteePeriod": {
          "xml": {
            "name": "guaranteePeriod",
            "attribute": false,
            "wrapped": false
          },
          "description": "Гарантийный срок с единицами измерения и комментарием",
          "$ref": "#/definitions/TimePeriodDTO"
        },
        "guaranteePeriodDays": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "guaranteePeriodDays",
            "attribute": true,
            "wrapped": false
          },
          "description": "Гарантийный срок"
        },
        "lifeTime": {
          "xml": {
            "name": "lifeTime",
            "attribute": false,
            "wrapped": false
          },
          "description": "Срок службы с единицами измерения и комментарием",
          "$ref": "#/definitions/TimePeriodDTO"
        },
        "lifeTimeDays": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "lifeTimeDays",
            "attribute": true,
            "wrapped": false
          },
          "description": "Срок службы"
        },
        "manufacturer": {
          "type": "string",
          "xml": {
            "name": "manufacturer",
            "attribute": true,
            "wrapped": false
          },
          "description": "Производитель"
        },
        "manufacturerCountry": {
          "type": "string",
          "xml": {
            "name": "manufacturerCountry",
            "attribute": false,
            "wrapped": false
          },
          "description": "Страна производства"
        },
        "minShipment": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "minShipment",
            "attribute": true,
            "wrapped": false
          },
          "description": "Минимальная партия поставки"
        },
        "quantumOfSupply": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "quantumOfSupply",
            "attribute": true,
            "wrapped": false
          },
          "description": "Добавочная партия, квант поставки"
        },
        "shelfLife": {
          "xml": {
            "name": "shelfLife",
            "attribute": false,
            "wrapped": false
          },
          "description": "Срок годности с единицами измерения и комментарием",
          "$ref": "#/definitions/TimePeriodDTO"
        },
        "shelfLifeDays": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "shelfLifeDays",
            "attribute": true,
            "wrapped": false
          },
          "description": "Срок годности"
        },
        "supplyScheduleDays": {
          "type": "array",
          "xml": {
            "name": "supplyScheduleDays",
            "attribute": false,
            "wrapped": true
          },
          "description": "Расписание поставки",
          "items": {
            "type": "string"
          }
        },
        "transportUnitSize": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "transportUnitSize",
            "attribute": true,
            "wrapped": false
          },
          "description": "Количество товаров в упаковке, кратность короба"
        }
      },
      "title": "MasterData",
      "description": "Мастер данные, то что нам нужно собирать по закону об агрегаторах"
    },
    "ModelDTO": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор маркетной модели"
        },
        "shortName": {
          "type": "string",
          "xml": {
            "name": "shortName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название маркетной модели"
        }
      },
      "title": "ModelDTO"
    },
    "ModeratedShopSkuDTO": {
      "type": "object",
      "properties": {
        "availability": {
          "type": "string",
          "xml": {
            "name": "availability",
            "attribute": true,
            "wrapped": false
          },
          "description": "Сатус активности офера",
          "enum": [
            "ACTIVE",
            "INACTIVE",
            "DELISTED"
          ]
        },
        "barcodes": {
          "type": "array",
          "xml": {
            "name": "barcodes",
            "attribute": false,
            "wrapped": true
          },
          "description": "Штрихкоды, которые могут быть у товара",
          "items": {
            "type": "string"
          }
        },
        "brand": {
          "type": "string",
          "xml": {
            "name": "brand",
            "attribute": true,
            "wrapped": false
          },
          "description": "Торговая марка товара"
        },
        "categoryName": {
          "type": "string",
          "xml": {
            "name": "categoryName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Категория товара"
        },
        "contentProcessingTaskId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "contentProcessingTaskId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ид задачи обработки контента"
        },
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание офера"
        },
        "masterData": {
          "xml": {
            "name": "masterData",
            "attribute": false,
            "wrapped": false
          },
          "description": "Мастер данные, то что нам нужно собирать по закону об агрегаторах",
          "$ref": "#/definitions/MasterData"
        },
        "offerProcessingComments": {
          "type": "array",
          "xml": {
            "name": "offerProcessingComments",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий к статусу размещения офера",
          "items": {
            "$ref": "#/definitions/UserMessageDTO"
          }
        },
        "offerProcessingStatus": {
          "type": "string",
          "xml": {
            "name": "offerProcessingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус размещения офера",
          "enum": [
            "UNKNOWN",
            "READY",
            "IN_WORK",
            "NEED_INFO",
            "NEED_MAPPING",
            "NEED_CONTENT",
            "SUSPENDED",
            "CONTENT_PROCESSING",
            "REJECTED"
          ]
        },
        "shopSku": {
          "type": "string",
          "xml": {
            "name": "shopSku",
            "attribute": true,
            "wrapped": false
          },
          "description": "Непосредственное строковое значение SKU"
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус модерация",
          "enum": [
            "ACCEPTED",
            "REJECTED",
            "MODERATION"
          ]
        },
        "title": {
          "type": "string",
          "xml": {
            "name": "title",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название офера"
        },
        "urls": {
          "type": "array",
          "xml": {
            "name": "urls",
            "attribute": false,
            "wrapped": true
          },
          "description": "Урл товара на сайте",
          "items": {
            "type": "string"
          }
        },
        "vendorCode": {
          "type": "string",
          "xml": {
            "name": "vendorCode",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор товара от производителя"
        }
      },
      "title": "ModeratedShopSkuDTO",
      "xml": {
        "name": "shopSku",
        "attribute": false,
        "wrapped": false
      },
      "description": "SKU используемое поставщиком/магазином (Shop-SKU) в месте со статусом модерации привязки."
    },
    "ModerationRequestState": {
      "type": "object",
      "properties": {
        "attemptsLeft": {
          "type": "integer",
          "format": "int32",
          "description": "Количество оставшихся попыток пройти модерацию"
        },
        "cpaModerationDisabledReasons": {
          "type": "array",
          "description": "Причины, по которым модерация невозможна CPA-модерация",
          "items": {
            "type": "string",
            "enum": [
              "PROGRAM_IS_NOT_SELECTED",
              "MISSED_DATASOURCE_PARAMS",
              "FATAL_CUTOFFS",
              "MODERATION_IN_PROGRESS",
              "MODERATION_NOT_NEEDED",
              "NO_MORE_ATTEMPTS",
              "MODERATION_PASSED",
              "MODERATION_DISABLED",
              "FEED_ERRORS"
            ]
          }
        },
        "cpcModerationDisabledReasons": {
          "type": "array",
          "description": "Причины, по которым модерация невозможна CPC-модерация",
          "items": {
            "type": "string",
            "enum": [
              "PROGRAM_IS_NOT_SELECTED",
              "MISSED_DATASOURCE_PARAMS",
              "FATAL_CUTOFFS",
              "MODERATION_IN_PROGRESS",
              "MODERATION_NOT_NEEDED",
              "NO_MORE_ATTEMPTS",
              "MODERATION_PASSED",
              "MODERATION_DISABLED",
              "FEED_ERRORS"
            ]
          }
        },
        "internalTestingTypes": {
          "type": "array",
          "description": "Внутренние типы модерации, на которые будет отправлен магазин. Это включает в себя GENERAL-модерацию",
          "items": {
            "type": "string",
            "enum": [
              "GENERAL",
              "CPC",
              "CPA",
              "SELF_CHECK"
            ]
          }
        },
        "moderationEnabled": {
          "type": "boolean",
          "description": "Модерация доступна или нет"
        },
        "testingTypes": {
          "type": "array",
          "description": "Типы модерации, на которые будет отправлен магазин",
          "items": {
            "type": "string",
            "enum": [
              "GENERAL",
              "CPC",
              "CPA",
              "SELF_CHECK"
            ]
          }
        }
      },
      "title": "ModerationRequestState",
      "xml": {
        "name": "moderationRequestState",
        "attribute": false,
        "wrapped": false
      },
      "description": "Программы, по которомы необходима модерация"
    },
    "NotificationContact": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string"
        },
        "firstName": {
          "type": "string"
        },
        "lastName": {
          "type": "string"
        },
        "phone": {
          "type": "string"
        }
      },
      "title": "NotificationContact",
      "xml": {
        "name": "notificationContact",
        "attribute": false,
        "wrapped": false
      }
    },
    "NotificationContactDTO": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string"
        },
        "firstName": {
          "type": "string"
        },
        "lastName": {
          "type": "string"
        },
        "phone": {
          "type": "string"
        }
      },
      "title": "NotificationContactDTO",
      "xml": {
        "name": "notificationContact",
        "attribute": false,
        "wrapped": false
      }
    },
    "OfferCountDTO": {
      "type": "object",
      "properties": {
        "offerCount": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "offerCount",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "OfferCountDTO"
    },
    "OfferDTO": {
      "type": "object",
      "properties": {
        "category": {
          "xml": {
            "name": "category",
            "attribute": true,
            "wrapped": false
          },
          "description": "Категория товара",
          "$ref": "#/definitions/CategoryDTO"
        },
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание"
        },
        "id": {
          "type": "string",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор офера"
        },
        "model": {
          "xml": {
            "name": "model",
            "attribute": true,
            "wrapped": false
          },
          "description": "Модель",
          "$ref": "#/definitions/ModelDTO"
        },
        "partnerPictures": {
          "type": "array",
          "xml": {
            "name": "partnerPictures",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ссылки на картинки партнера",
          "items": {
            "type": "string"
          }
        },
        "pictures": {
          "type": "array",
          "xml": {
            "name": "pictures",
            "attribute": true,
            "wrapped": false
          },
          "description": "Картинки",
          "items": {
            "$ref": "#/definitions/PictureDTO"
          }
        },
        "prices": {
          "xml": {
            "name": "prices",
            "attribute": true,
            "wrapped": false
          },
          "description": "Цена",
          "$ref": "#/definitions/PriceDTO"
        },
        "publishingStatus": {
          "xml": {
            "name": "publishingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус скрытия",
          "$ref": "#/definitions/PublishingStatusDTO"
        },
        "seller": {
          "xml": {
            "name": "seller",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий продавца",
          "$ref": "#/definitions/SalesNotesDTO"
        },
        "titles": {
          "xml": {
            "name": "titles",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название",
          "$ref": "#/definitions/HighlightedTextDTO"
        },
        "urls": {
          "xml": {
            "name": "urls",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ссылки на товар магазина",
          "$ref": "#/definitions/UrlsDTO"
        },
        "vendor": {
          "xml": {
            "name": "vendor",
            "attribute": true,
            "wrapped": false
          },
          "description": "Вендор",
          "$ref": "#/definitions/VendorDTO"
        },
        "wareId": {
          "type": "string",
          "xml": {
            "name": "wareId",
            "attribute": true,
            "wrapped": false
          },
          "description": "ID офера"
        }
      },
      "title": "OfferDTO",
      "description": "Представление офера из хранилища"
    },
    "OfferIdentifier": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id офера"
        }
      },
      "title": "OfferIdentifier"
    },
    "OfferProcessingStatus": {
      "type": "object",
      "properties": {
        "offerProcessingStatus": {
          "type": "string",
          "xml": {
            "name": "offerProcessingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус процессинга офера",
          "enum": [
            "UNKNOWN",
            "READY",
            "IN_WORK",
            "NEED_INFO",
            "NEED_MAPPING",
            "NEED_CONTENT",
            "SUSPENDED",
            "CONTENT_PROCESSING",
            "REJECTED"
          ]
        }
      },
      "title": "OfferProcessingStatus",
      "description": "Запрос на изменение статуса процессинга офера"
    },
    "OfferProcessingStatusStats": {
      "type": "object",
      "properties": {
        "count": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "count",
            "attribute": true,
            "wrapped": false
          },
          "description": "Количество"
        },
        "offerProcessingStatus": {
          "type": "string",
          "xml": {
            "name": "offerProcessingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус размещения офера",
          "enum": [
            "UNKNOWN",
            "READY",
            "IN_WORK",
            "NEED_INFO",
            "NEED_MAPPING",
            "NEED_CONTENT",
            "SUSPENDED",
            "CONTENT_PROCESSING",
            "REJECTED"
          ]
        },
        "offerProcessingStatusStats": {
          "type": "array",
          "xml": {
            "name": "offerProcessingStatusStats",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статистика по статусу офера",
          "items": {
            "$ref": "#/definitions/OfferProcessingStatusStats"
          }
        }
      },
      "title": "OfferProcessingStatusStats",
      "description": "Статистика по статусу процессинга офера"
    },
    "OffersSummaryDTO": {
      "type": "object",
      "properties": {
        "approved": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "approved",
            "attribute": true,
            "wrapped": false
          }
        },
        "onModeration": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "onModeration",
            "attribute": true,
            "wrapped": false
          }
        },
        "total": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "total",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "OffersSummaryDTO"
    },
    "OldPriceDTO": {
      "type": "object",
      "properties": {
        "oldMin": {
          "type": "number",
          "xml": {
            "name": "oldMin",
            "attribute": true,
            "wrapped": false
          },
          "description": "Старая цена товара"
        },
        "percent": {
          "type": "number",
          "xml": {
            "name": "percent",
            "attribute": true,
            "wrapped": false
          },
          "description": "Размер скидки в процентах"
        }
      },
      "title": "OldPriceDTO"
    },
    "OptionalInt": {
      "type": "object",
      "title": "OptionalInt"
    },
    "OrderMinCostDTO": {
      "type": "object",
      "properties": {
        "isEnabled": {
          "type": "boolean"
        },
        "value": {
          "type": "integer",
          "format": "int32"
        }
      },
      "title": "OrderMinCostDTO",
      "xml": {
        "name": "orderMinCost",
        "attribute": false,
        "wrapped": false
      }
    },
    "OrderSummaryDTO": {
      "type": "object",
      "properties": {
        "deliveredCount": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "deliveredCount",
            "attribute": true,
            "wrapped": false
          }
        },
        "inDeliveryCount": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "inDeliveryCount",
            "attribute": true,
            "wrapped": false
          }
        },
        "paidByBuyer": {
          "$ref": "#/definitions/AmountCurrencyDTO"
        },
        "paidByMarket": {
          "$ref": "#/definitions/AmountCurrencyDTO"
        },
        "totalCount": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "totalCount",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "OrderSummaryDTO"
    },
    "OrderSummaryDTOV2": {
      "type": "object",
      "properties": {
        "delivered": {
          "$ref": "#/definitions/CountAmountCurrencyDTO"
        },
        "deliveredLast": {
          "$ref": "#/definitions/CountAmountCurrencyDTO"
        },
        "inDelivery": {
          "$ref": "#/definitions/CountAmountCurrencyDTO"
        },
        "inDeliveryLast": {
          "$ref": "#/definitions/CountAmountCurrencyDTO"
        },
        "month": {
          "xml": {
            "name": "month",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/YearMonth"
        },
        "total": {
          "$ref": "#/definitions/CountAmountCurrencyDTO"
        }
      },
      "title": "OrderSummaryDTOV2",
      "xml": {
        "name": "order",
        "attribute": false,
        "wrapped": false
      }
    },
    "OrganizationInfo": {
      "type": "object",
      "properties": {
        "factualAddress": {
          "type": "string",
          "description": "Фактический адрес"
        },
        "juridicalAddress": {
          "type": "string",
          "description": "Юридический адрес"
        },
        "name": {
          "type": "string",
          "description": "Название организации"
        },
        "organizationType": {
          "type": "string",
          "description": "Тип организации",
          "enum": [
            "NONE",
            "OOO",
            "ZAO",
            "IP",
            "CHP",
            "OTHER",
            "OAO"
          ]
        },
        "registrationNumber": {
          "type": "string",
          "description": "Регистрационный номер"
        },
        "source": {
          "type": "string",
          "description": "Источник заполнения информации",
          "enum": [
            "PARTNER_INTERFACE",
            "YA_MONEY",
            "YANDEX_MARKET"
          ]
        }
      },
      "title": "OrganizationInfo",
      "description": "Юридическая информация организации"
    },
    "OrganizationInfoDTO": {
      "type": "object",
      "properties": {
        "accountNumber": {
          "type": "string",
          "xml": {
            "name": "accountNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "bankName": {
          "type": "string",
          "xml": {
            "name": "bankName",
            "attribute": true,
            "wrapped": false
          }
        },
        "bik": {
          "type": "string",
          "xml": {
            "name": "bik",
            "attribute": true,
            "wrapped": false
          }
        },
        "corrAccountNumber": {
          "type": "string",
          "xml": {
            "name": "corrAccountNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "factAddress": {
          "type": "string",
          "xml": {
            "name": "factAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "inn": {
          "type": "string",
          "xml": {
            "name": "inn",
            "attribute": true,
            "wrapped": false
          }
        },
        "juridicalAddress": {
          "type": "string",
          "xml": {
            "name": "juridicalAddress",
            "attribute": true,
            "wrapped": false
          }
        },
        "kpp": {
          "type": "string",
          "xml": {
            "name": "kpp",
            "attribute": true,
            "wrapped": false
          }
        },
        "licenseDate": {
          "type": "string",
          "format": "date",
          "xml": {
            "name": "licenseDate",
            "attribute": true,
            "wrapped": false
          }
        },
        "licenseNumber": {
          "type": "string",
          "xml": {
            "name": "licenseNumber",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "ogrn": {
          "type": "string",
          "xml": {
            "name": "ogrn",
            "attribute": true,
            "wrapped": false
          }
        },
        "postcode": {
          "type": "string",
          "xml": {
            "name": "postcode",
            "attribute": true,
            "wrapped": false
          }
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "NONE",
            "OOO",
            "ZAO",
            "IP",
            "CHP",
            "OTHER",
            "OAO"
          ]
        },
        "workSchedule": {
          "type": "string",
          "xml": {
            "name": "workSchedule",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "OrganizationInfoDTO",
      "xml": {
        "name": "organizationInfo",
        "attribute": false,
        "wrapped": false
      }
    },
    "OrganizationInfoTypesDTO": {
      "type": "object",
      "properties": {
        "types": {
          "type": "array",
          "description": "Типы",
          "items": {
            "type": "string",
            "enum": [
              "NONE",
              "OOO",
              "ZAO",
              "IP",
              "CHP",
              "OTHER",
              "OAO"
            ]
          }
        }
      },
      "title": "OrganizationInfoTypesDTO",
      "xml": {
        "name": "data",
        "attribute": false,
        "wrapped": false
      },
      "description": "Типы организаций"
    },
    "Page": {
      "type": "object",
      "properties": {
        "defaults": {
          "$ref": "#/definitions/BaseSecurable"
        },
        "features": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Feature"
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "override": {
          "type": "boolean"
        },
        "roles": {
          "$ref": "#/definitions/SecurityRule"
        },
        "states": {
          "$ref": "#/definitions/SecurityRule"
        }
      },
      "title": "Page"
    },
    "PagedCrossborderCampaignsDto": {
      "type": "object",
      "properties": {
        "campaigns": {
          "type": "array",
          "description": "Список кампаний на данной странице.",
          "items": {
            "$ref": "#/definitions/CrossborderCampaignDto"
          }
        },
        "pager": {
          "description": "Информация о странице данных.",
          "$ref": "#/definitions/PagerInfo"
        }
      },
      "title": "PagedCrossborderCampaignsDto",
      "description": "Страница информации о кроссбордер кампаниях"
    },
    "PagerInfo": {
      "type": "object",
      "properties": {
        "currentPage": {
          "type": "integer",
          "format": "int32",
          "example": 1,
          "description": "Номер текущей отборажаемой страницы."
        },
        "perpageNumber": {
          "type": "integer",
          "format": "int32",
          "example": 4,
          "description": "Количество элементов на странице."
        },
        "totalCount": {
          "type": "integer",
          "format": "int32",
          "example": 3,
          "description": "Общее количество результатов."
        }
      },
      "title": "PagerInfo",
      "xml": {
        "name": "paging",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о странице данных."
    },
    "PagingDirectionsDTO": {
      "type": "object",
      "properties": {
        "nextPageToken": {
          "type": "string",
          "example": "MQ",
          "xml": {
            "name": "nextPageToken",
            "attribute": true,
            "wrapped": false
          },
          "description": "Токен следующей страницы."
        },
        "prevPageToken": {
          "type": "string",
          "example": "MA",
          "xml": {
            "name": "prevPageToken",
            "attribute": true,
            "wrapped": false
          },
          "description": "Токен предыдущей страницы."
        }
      },
      "title": "PagingDirectionsDTO",
      "xml": {
        "name": "paging",
        "attribute": false,
        "wrapped": false
      }
    },
    "PagingElementDTO": {
      "type": "object",
      "properties": {
        "last": {
          "type": "boolean",
          "xml": {
            "name": "last",
            "attribute": true,
            "wrapped": false
          }
        },
        "pageOffset": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "pageOffset",
            "attribute": true,
            "wrapped": false
          }
        },
        "pageToken": {
          "type": "string",
          "xml": {
            "name": "pageToken",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "PagingElementDTO"
    },
    "PapiHidingDetailsDto": {
      "type": "object",
      "properties": {
        "message": {
          "type": "string",
          "xml": {
            "name": "message",
            "attribute": true,
            "wrapped": false
          },
          "description": "Сообщение"
        },
        "timeout": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "timeout",
            "attribute": true,
            "wrapped": false
          },
          "description": "Время в миллисекундах, когда оффер снова станет видимым"
        }
      },
      "title": "PapiHidingDetailsDto",
      "xml": {
        "name": "PapiHidingDetailsDto",
        "attribute": false,
        "wrapped": false
      },
      "description": "Детали скрытия оффера через ПАПИ"
    },
    "ParseLogParsed": {
      "type": "object",
      "properties": {
        "codeStats": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/FeedLogCodeStats"
          }
        },
        "feedStats": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/FeedLogStats"
          }
        }
      },
      "title": "ParseLogParsed"
    },
    "PartnerDocumentDTO": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "requestId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "requestId",
            "attribute": true,
            "wrapped": false
          }
        },
        "size": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "size",
            "attribute": true,
            "wrapped": false
          }
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "OTHER",
            "SIGNED_APP_FORM",
            "SIGNATORY_DOC",
            "SIGNED_APP_PROGRAMS_UPDATE",
            "CERTIFICATE_OF_INCORPORATION",
            "COMPANY_LICENCE",
            "TAX_REGISTRATION",
            "ID"
          ]
        }
      },
      "title": "PartnerDocumentDTO",
      "xml": {
        "name": "document",
        "attribute": false,
        "wrapped": false
      }
    },
    "PartnerDocumentUrlDTO": {
      "type": "object",
      "properties": {
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "PartnerDocumentUrlDTO",
      "xml": {
        "name": "documentUrl",
        "attribute": false,
        "wrapped": false
      }
    },
    "PartnerErrorInfo": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string"
        },
        "details": {
          "type": "object"
        },
        "message": {
          "type": "string"
        },
        "stackTrace": {
          "type": "string"
        }
      },
      "title": "PartnerErrorInfo",
      "xml": {
        "name": "error",
        "attribute": false,
        "wrapped": false
      }
    },
    "PhoneNumber": {
      "type": "object",
      "properties": {
        "city": {
          "type": "string",
          "xml": {
            "name": "city",
            "attribute": false,
            "wrapped": false
          },
          "description": "Город"
        },
        "comments": {
          "type": "string",
          "xml": {
            "name": "comments",
            "attribute": false,
            "wrapped": false
          },
          "description": "Комментарии"
        },
        "country": {
          "type": "string",
          "xml": {
            "name": "country",
            "attribute": false,
            "wrapped": false
          },
          "description": "Страна"
        },
        "extension": {
          "type": "string",
          "xml": {
            "name": "extension",
            "attribute": false,
            "wrapped": false
          },
          "description": "Расширение номера телефона"
        },
        "number": {
          "type": "string",
          "xml": {
            "name": "number",
            "attribute": false,
            "wrapped": false
          },
          "description": "Номер телефона"
        },
        "phoneType": {
          "type": "string",
          "example": "FAX",
          "xml": {
            "name": "phoneType",
            "attribute": false,
            "wrapped": false
          },
          "description": "Тип телефона"
        }
      },
      "title": "PhoneNumber",
      "description": "Телефон точки продаж"
    },
    "PictureDTO": {
      "type": "object",
      "properties": {
        "containerHeight": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "containerHeight",
            "attribute": true,
            "wrapped": false
          },
          "description": "Высота котейнера, под который предназначается картинка"
        },
        "containerWidth": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "containerWidth",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ширина контейнера, под который предназначается картинка"
        },
        "height": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "height",
            "attribute": true,
            "wrapped": false
          },
          "description": "Высота"
        },
        "thumbnails": {
          "type": "array",
          "xml": {
            "name": "thumbnails",
            "attribute": true,
            "wrapped": false
          },
          "description": "Тамбнэйлы",
          "items": {
            "$ref": "#/definitions/PictureDTO"
          }
        },
        "url": {
          "type": "string",
          "xml": {
            "name": "url",
            "attribute": true,
            "wrapped": false
          },
          "description": "Url картинки"
        },
        "width": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "width",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ширина"
        }
      },
      "title": "PictureDTO"
    },
    "Point": {
      "type": "object",
      "properties": {
        "address": {
          "xml": {
            "name": "address",
            "attribute": false,
            "wrapped": false
          },
          "description": "Адрес точки продаж",
          "$ref": "#/definitions/Address"
        },
        "contactName": {
          "type": "string",
          "xml": {
            "name": "contactName",
            "attribute": false,
            "wrapped": false
          },
          "description": "Контактное лицо"
        },
        "emails": {
          "type": "array",
          "xml": {
            "name": "emails",
            "attribute": false,
            "wrapped": false
          },
          "description": "Email-адреса точки продаж",
          "items": {
            "type": "string"
          }
        },
        "geoInfo": {
          "xml": {
            "name": "geoInfo",
            "attribute": false,
            "wrapped": false
          },
          "description": "Информация о географическом положении точки",
          "$ref": "#/definitions/GeoInfo"
        },
        "phones": {
          "type": "array",
          "xml": {
            "name": "phones",
            "attribute": false,
            "wrapped": false
          },
          "description": "Телефоны точки продаж",
          "items": {
            "$ref": "#/definitions/PhoneNumber"
          }
        },
        "schedule": {
          "xml": {
            "name": "schedule",
            "attribute": false,
            "wrapped": false
          },
          "description": "Часы работы точки продаж",
          "$ref": "#/definitions/Schedule"
        }
      },
      "title": "Point",
      "description": "Точка продаж"
    },
    "PremoderationState": {
      "type": "object",
      "properties": {
        "programStates": {
          "type": "array",
          "description": "Модерируемые программы",
          "items": {
            "$ref": "#/definitions/ProgramState"
          }
        }
      },
      "title": "PremoderationState",
      "xml": {
        "name": "premoderationState",
        "attribute": false,
        "wrapped": false
      },
      "description": "Краткая информация о модерации"
    },
    "PrepayRequestDTO": {
      "type": "object",
      "properties": {
        "comment": {
          "type": "string"
        },
        "contactInfo": {
          "$ref": "#/definitions/ContactInfoDTO"
        },
        "crossborderOrganizationInfo": {
          "$ref": "#/definitions/CrossborderOrganizationInfoDTO"
        },
        "datasourceIds": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "documents": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/PrepayRequestDocumentDTO"
          }
        },
        "filled": {
          "type": "boolean",
          "xml": {
            "name": "filled",
            "attribute": true,
            "wrapped": false
          }
        },
        "organizationInfo": {
          "$ref": "#/definitions/OrganizationInfoDTO"
        },
        "prepayType": {
          "type": "string",
          "xml": {
            "name": "prepayType",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "UNKNOWN",
            "YANDEX_MONEY",
            "YANDEX_MARKET"
          ]
        },
        "requestId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "requestId",
            "attribute": true,
            "wrapped": false
          }
        },
        "requestType": {
          "type": "string",
          "xml": {
            "name": "requestType",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "SHOP",
            "SUPPLIER",
            "CROSSBORDER",
            "FMCG",
            "DELIVERY"
          ]
        },
        "signatory": {
          "$ref": "#/definitions/SignatoryInfoDTO"
        },
        "startDate": {
          "type": "string",
          "format": "date-time",
          "xml": {
            "name": "startDate",
            "attribute": true,
            "wrapped": false
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        },
        "updatedAt": {
          "type": "string",
          "format": "date-time",
          "xml": {
            "name": "updatedAt",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "PrepayRequestDTO"
    },
    "PrepayRequestDocumentDTO": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "size": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "size",
            "attribute": true,
            "wrapped": false
          }
        },
        "type": {
          "type": "string",
          "xml": {
            "name": "type",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "OTHER",
            "SIGNED_APP_FORM",
            "SIGNATORY_DOC",
            "SIGNED_APP_PROGRAMS_UPDATE",
            "CERTIFICATE_OF_INCORPORATION",
            "COMPANY_LICENCE",
            "TAX_REGISTRATION",
            "ID"
          ]
        },
        "uploadDate": {
          "type": "string",
          "format": "date-time",
          "xml": {
            "name": "uploadDate",
            "attribute": true,
            "wrapped": false
          }
        },
        "url": {
          "type": "string"
        }
      },
      "title": "PrepayRequestDocumentDTO"
    },
    "PrepayRequestForm": {
      "type": "object",
      "properties": {
        "contactInfo": {
          "$ref": "#/definitions/ContactInfoDTO"
        },
        "datasourceIds": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "organizationInfo": {
          "$ref": "#/definitions/OrganizationInfoDTO"
        },
        "signatory": {
          "$ref": "#/definitions/SignatoryInfoDTO"
        }
      },
      "title": "PrepayRequestForm"
    },
    "PrepaymentShopInfoDTO": {
      "type": "object",
      "properties": {
        "availabilityStatus": {
          "type": "string",
          "xml": {
            "name": "availabilityStatus",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "AVAILABLE",
            "NOT_AVAILABLE",
            "APPLIED"
          ]
        },
        "prepayRequests": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/PrepayRequestDTO"
          }
        }
      },
      "title": "PrepaymentShopInfoDTO",
      "xml": {
        "name": "prepaymentShopInfo",
        "attribute": false,
        "wrapped": false
      }
    },
    "PriceDTO": {
      "type": "object",
      "properties": {
        "currency": {
          "type": "string",
          "xml": {
            "name": "currency",
            "attribute": true,
            "wrapped": false
          },
          "description": "Валюта",
          "enum": [
            "RUR",
            "USD",
            "EUR",
            "BYN",
            "KZT",
            "UAH"
          ]
        },
        "oldPrice": {
          "description": "Старая цена, от которой расчитывается скидка",
          "$ref": "#/definitions/OldPriceDTO"
        },
        "value": {
          "type": "number",
          "xml": {
            "name": "value",
            "attribute": true,
            "wrapped": false
          },
          "description": "Собственно, цена"
        },
        "vat": {
          "type": "string",
          "xml": {
            "name": "vat",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ставка НДС",
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        }
      },
      "title": "PriceDTO",
      "description": "Объектное составное представление цены товара"
    },
    "ProcessedMarketSkuQuery": {
      "type": "object",
      "properties": {
        "mappedMarketSkus": {
          "type": "array",
          "xml": {
            "name": "mappedMarketSkus",
            "attribute": false,
            "wrapped": true
          },
          "description": "Найденные Market-SKU привязанные к каким-либо магазинным SKU",
          "items": {
            "$ref": "#/definitions/MappedMarketSku"
          }
        },
        "marketSkus": {
          "type": "array",
          "xml": {
            "name": "marketSkus",
            "attribute": false,
            "wrapped": true
          },
          "description": "Список Market-SKU для которых интересен результат запроса на поиск привязок",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        }
      },
      "title": "ProcessedMarketSkuQuery",
      "xml": {
        "name": "marketSkuQuery",
        "attribute": false,
        "wrapped": false
      },
      "description": "Обработанный запрос на поиск привязок для маркетных SKU"
    },
    "ProgramField": {
      "type": "object",
      "properties": {
        "filled": {
          "type": "boolean",
          "description": "Признак заполненности поля"
        },
        "name": {
          "type": "string",
          "description": "Название поля"
        },
        "required": {
          "type": "boolean",
          "description": "Признак обязательности поля"
        }
      },
      "title": "ProgramField",
      "xml": {
        "name": "field",
        "attribute": false,
        "wrapped": false
      },
      "description": "Данные о поле программы"
    },
    "ProgramState": {
      "type": "object",
      "properties": {
        "program": {
          "type": "string",
          "description": "Модерируемая программа",
          "enum": [
            "GENERAL",
            "CPC",
            "CPA",
            "SELF_CHECK"
          ]
        },
        "startDate": {
          "type": "integer",
          "format": "int64",
          "description": "Таймстемп начала модерации (в миллисекундах)"
        },
        "testingStatus": {
          "type": "string",
          "description": "Статус модерации",
          "enum": [
            "UNDEFINED",
            "INITED",
            "READY_FOR_CHECK",
            "WAITING_FEED_FIRST_LOAD",
            "CHECKING",
            "WAITING_FEED_LAST_LOAD",
            "PASSED",
            "CANCELED",
            "READY_TO_FAIL",
            "FAILED",
            "DISABLED",
            "EXPIRED",
            "PENDING_CHECK_START",
            "NEED_INFO"
          ]
        }
      },
      "title": "ProgramState"
    },
    "ProgramStatus": {
      "type": "object",
      "properties": {
        "isEnabled": {
          "type": "boolean",
          "description": "Признак того, включена ли программа."
        },
        "needTestingState": {
          "type": "string",
          "description": "Признак того, нужна ли модерация.",
          "enum": [
            "REQUIRED",
            "NOT_REQUIRED",
            "BLOCKED"
          ]
        },
        "newbie": {
          "type": "boolean",
          "description": "Признак того, платил ли магазин хотя бы раз (если newbie=true, то не платил)."
        },
        "program": {
          "type": "string",
          "description": "Имя программы",
          "enum": [
            "CPC",
            "CHINA_GOODS_APP_PLACEMENT",
            "MARKETPLACE"
          ]
        },
        "status": {
          "type": "string",
          "description": "Статус",
          "enum": [
            "EMPTY",
            "FULL",
            "ENABLING",
            "FILLED",
            "TESTING",
            "SUSPENDED",
            "TESTING_FAILED",
            "TESTING_DROPPED",
            "FAILED",
            "NONE",
            "DISABLED",
            "TESTING_HOLD"
          ]
        },
        "subStatuses": {
          "type": "array",
          "description": "Подстатусы, уточняющие статус.",
          "items": {
            "$ref": "#/definitions/ProgramSubstatus"
          }
        }
      },
      "title": "ProgramStatus",
      "description": "Результат определения статуса программы"
    },
    "ProgramStatusInfoDTO": {
      "type": "object",
      "properties": {
        "shopId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "shopId",
            "attribute": true,
            "wrapped": false
          }
        },
        "statuses": {
          "type": "object",
          "xml": {
            "name": "statuses",
            "attribute": true,
            "wrapped": false
          },
          "additionalProperties": {
            "$ref": "#/definitions/ProgramStatusesDTO"
          }
        }
      },
      "title": "ProgramStatusInfoDTO"
    },
    "ProgramStatusResponseDTO": {
      "type": "object",
      "properties": {
        "programStatusInfo": {
          "$ref": "#/definitions/ProgramStatusInfoDTO"
        }
      },
      "title": "ProgramStatusResponseDTO",
      "xml": {
        "name": "ProgramStatusResponseDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "ProgramStatusesDTO": {
      "type": "object",
      "properties": {
        "isActive": {
          "type": "boolean",
          "xml": {
            "name": "isActive",
            "attribute": true,
            "wrapped": false
          }
        },
        "isSigned": {
          "type": "boolean",
          "xml": {
            "name": "isSigned",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "ProgramStatusesDTO"
    },
    "ProgramSubstatus": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string",
          "description": "Код подстатуса."
        },
        "params": {
          "type": "object",
          "description": "Параметры подстатуса."
        }
      },
      "title": "ProgramSubstatus",
      "description": "Подстатус программы"
    },
    "PublishingStatusDTO": {
      "type": "object",
      "properties": {
        "summary": {
          "type": "string",
          "xml": {
            "name": "summary",
            "attribute": true,
            "wrapped": false
          },
          "description": "Непосредственно сам статус",
          "enum": [
            "AVAILABLE",
            "INDEXING",
            "HIDDEN"
          ]
        }
      },
      "title": "PublishingStatusDTO",
      "description": "Статус скрытия офера"
    },
    "PushFeedInfo": {
      "type": "object",
      "properties": {
        "complete": {
          "type": "boolean",
          "description": "Тип фида - комплитный (true) или апдейтный (false)"
        },
        "updateTime": {
          "type": "integer",
          "format": "int64",
          "description": "Таймстемп обновления фида в оферном хранилище в мс"
        }
      },
      "title": "PushFeedInfo"
    },
    "QueryResultInfo": {
      "type": "object",
      "properties": {
        "value": {
          "type": "string"
        }
      },
      "title": "QueryResultInfo"
    },
    "RatingSummaryDTO": {
      "type": "object",
      "properties": {
        "ratingValue": {
          "type": "number",
          "format": "double",
          "xml": {
            "name": "ratingValue",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "RatingSummaryDTO",
      "xml": {
        "name": "rating",
        "attribute": false,
        "wrapped": false
      }
    },
    "RegionDTO": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "parentId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "RegionDTO",
      "xml": {
        "name": "region",
        "attribute": false,
        "wrapped": false
      }
    },
    "RegionGroupPaymentDTO": {
      "type": "object",
      "properties": {
        "paymentTypes": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "COURIER_CASH",
              "COURIER_CARD",
              "PREPAYMENT_OTHER",
              "PREPAYMENT_CARD"
            ]
          }
        },
        "regionGroupId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "regionGroupId",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "RegionGroupPaymentDTO"
    },
    "RegionGroupStatusDTO": {
      "type": "object",
      "properties": {
        "comment": {
          "type": "string"
        },
        "deliveryReasons": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "NO_DELIVERY",
              "INVALID_DELIVERY_TIME",
              "INVALID_DELIVERY_COST"
            ]
          }
        },
        "paymentReasons": {
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "COURIER_CASH",
              "COURIER_CARD",
              "PREPAYMENT_OTHER",
              "PREPAYMENT_CARD"
            ]
          }
        },
        "regionGroupId": {
          "type": "integer",
          "format": "int64"
        },
        "status": {
          "type": "string",
          "enum": [
            "REVOKE",
            "SUCCESS",
            "FAIL",
            "NEW",
            "DONT_WANT",
            "FAIL_MANUAL"
          ]
        }
      },
      "title": "RegionGroupStatusDTO",
      "xml": {
        "name": "regionGroupPaymentTypes",
        "attribute": false,
        "wrapped": false
      }
    },
    "Report": {
      "type": "object",
      "properties": {
        "metaData": {
          "$ref": "#/definitions/ReportMetaData"
        },
        "rows": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Row"
          }
        }
      },
      "title": "Report",
      "xml": {
        "name": "report",
        "attribute": false,
        "wrapped": false
      }
    },
    "ReportMetaData": {
      "type": "object",
      "title": "ReportMetaData"
    },
    "ReportQueryInfo": {
      "type": "object",
      "properties": {
        "description": {
          "type": "string"
        },
        "headers": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/QueryResultInfo"
          }
        },
        "name": {
          "type": "string"
        },
        "params": {
          "type": "object",
          "additionalProperties": {
            "type": "string"
          }
        },
        "show-name": {
          "type": "string"
        }
      },
      "title": "ReportQueryInfo",
      "xml": {
        "name": "report-query-info",
        "attribute": false,
        "wrapped": false
      }
    },
    "ResultDTO": {
      "type": "object",
      "properties": {
        "declinedOffers": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "declinedOffers",
            "attribute": true,
            "wrapped": false
          }
        },
        "enrichedFile": {
          "$ref": "#/definitions/FileUploadDTO"
        },
        "error": {
          "$ref": "#/definitions/UserMessageDTO"
        },
        "existingOffers": {
          "$ref": "#/definitions/ExistingOffersDTO"
        },
        "newOffers": {
          "type": "integer",
          "format": "int64"
        },
        "processedOffers": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "processedOffers",
            "attribute": true,
            "wrapped": false
          }
        },
        "totalOffers": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "totalOffers",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "ResultDTO",
      "xml": {
        "name": "feedValidationDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "RewardQuarterDTO": {
      "type": "object",
      "properties": {
        "quarter": {
          "type": "integer",
          "format": "int32",
          "description": "Номер квартала [1, 4]",
          "required": true
        },
        "year": {
          "type": "integer",
          "format": "int32",
          "description": "Год",
          "required": true
        }
      },
      "title": "RewardQuarterDTO",
      "description": "Квартал"
    },
    "Row": {
      "type": "object",
      "properties": {
        "cells": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Cell"
          }
        }
      },
      "title": "Row",
      "xml": {
        "name": "report-row",
        "attribute": false,
        "wrapped": false
      }
    },
    "SalesNotesDTO": {
      "type": "object",
      "properties": {
        "currency": {
          "type": "string"
        },
        "orderMinCost": {
          "$ref": "#/definitions/OrderMinCostDTO"
        },
        "partnerId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "SalesNotesDTO",
      "xml": {
        "name": "salesInfo",
        "attribute": false,
        "wrapped": false
      }
    },
    "Schedule": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "description": "Идентификатор, к которому привязывается расписание"
        },
        "lines": {
          "type": "array",
          "description": "Строки расписания",
          "items": {
            "$ref": "#/definitions/ScheduleLine"
          }
        }
      },
      "title": "Schedule",
      "description": "Расписание точки продаж. Может содержать разные дни, разные временные интервалы"
    },
    "ScheduleLine": {
      "type": "object",
      "properties": {
        "days": {
          "type": "integer",
          "format": "int32",
          "description": "Количество дней действия расписания минус 1 (0 - 1 день, 1 - два дня и т.д.)"
        },
        "minutes": {
          "type": "integer",
          "format": "int32",
          "description": "Количество минут действия расписания"
        },
        "startDay": {
          "type": "string",
          "description": "День недели начала действия расписания",
          "enum": [
            "NEVERSDAY",
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
          ]
        },
        "startMinute": {
          "type": "integer",
          "format": "int32",
          "description": "Минута начала действия расписания"
        }
      },
      "title": "ScheduleLine",
      "description": "Строчка расписания"
    },
    "ScheduleLineDTO": {
      "type": "object",
      "properties": {
        "endDay": {
          "type": "string",
          "enum": [
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
          ]
        },
        "endTime": {
          "$ref": "#/definitions/LocalTime"
        },
        "startDay": {
          "type": "string",
          "enum": [
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
          ]
        },
        "startTime": {
          "$ref": "#/definitions/LocalTime"
        }
      },
      "title": "ScheduleLineDTO"
    },
    "ScheduleResponseDTO": {
      "type": "object",
      "properties": {
        "schedules": {
          "type": "array",
          "description": "Расписания показов",
          "items": {
            "$ref": "#/definitions/WeeklyScheduleDTO"
          },
          "required": true
        },
        "timezoneName": {
          "type": "string",
          "xml": {
            "name": "timezoneName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название таймзоны в формате Europe/Moscow",
          "required": true
        },
        "timezoneOffset": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "timezoneOffset",
            "attribute": true,
            "wrapped": false
          },
          "description": "Смещение таймзоны в миллисекундах относительно GMT",
          "required": true
        },
        "total": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "total",
            "attribute": true,
            "wrapped": false
          },
          "description": "Общее количество миллисекунд, которое показывается телефон",
          "required": true
        }
      },
      "title": "ScheduleResponseDTO",
      "xml": {
        "name": "schedule",
        "attribute": false,
        "wrapped": false
      },
      "description": "Расписание на неделю"
    },
    "ScheduleUpdateDTO": {
      "type": "object",
      "properties": {
        "schedules": {
          "type": "array",
          "description": "Список промежутков",
          "items": {
            "$ref": "#/definitions/WeeklyScheduleDTO"
          },
          "required": true
        },
        "timezoneName": {
          "type": "string",
          "description": "Таймзона, в которой задано расписание",
          "required": true
        }
      },
      "title": "ScheduleUpdateDTO",
      "xml": {
        "name": "schedule",
        "attribute": false,
        "wrapped": false
      },
      "description": "Обновление расписания"
    },
    "SearchOffersRequest": {
      "type": "object",
      "properties": {
        "pageToken": {
          "type": "string",
          "xml": {
            "name": "pageToken",
            "attribute": false,
            "wrapped": false
          },
          "description": "Токен постраничного вывода. Если есть, содержит закодированное представление страницы, которую нужно вернуть."
        }
      },
      "title": "SearchOffersRequest",
      "xml": {
        "name": "searchRequest",
        "attribute": false,
        "wrapped": false
      },
      "description": "Поисковый запрос к оферному хранилищу"
    },
    "SearchOffersResponse": {
      "type": "object",
      "properties": {
        "offers": {
          "type": "array",
          "description": "Найденные оферы",
          "items": {
            "$ref": "#/definitions/OfferDTO"
          }
        },
        "paging": {
          "description": "Токены для вывода соседних страниц",
          "$ref": "#/definitions/PagingDirectionsDTO"
        },
        "spreadPaging": {
          "type": "array",
          "description": "Набор токенов для навигации по страницам в обе стороны + Токен последней страницы",
          "items": {
            "$ref": "#/definitions/PagingElementDTO"
          }
        },
        "total": {
          "type": "integer",
          "format": "int32",
          "description": "Общее количество оферов, удовлетворяющих запросу"
        }
      },
      "title": "SearchOffersResponse",
      "xml": {
        "name": "response",
        "attribute": false,
        "wrapped": false
      },
      "description": "Ответ на поисковый запрос к оферному хранилищу"
    },
    "SecurityRule": {
      "type": "object",
      "properties": {
        "items": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "quantifier": {
          "type": "string",
          "xml": {
            "name": "quantifier",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "ANY",
            "ALL"
          ]
        },
        "result": {
          "type": "boolean"
        }
      },
      "title": "SecurityRule"
    },
    "SelectedDeliveryServiceResponseDTO": {
      "type": "object",
      "properties": {
        "common": {
          "type": "boolean",
          "xml": {
            "name": "common",
            "attribute": true,
            "wrapped": false
          },
          "description": "Является ли СД обычной",
          "required": true
        },
        "courierDeliveryStrategy": {
          "type": "string",
          "description": "Стратегия расчета СиС курьерской доставки",
          "enum": [
            "NO_DELIVERY",
            "AUTO_CALCULATED",
            "UNKNOWN_COST_TIME",
            "FIXED_COST_TIME"
          ],
          "required": true
        },
        "global": {
          "type": "boolean",
          "xml": {
            "name": "global",
            "attribute": true,
            "wrapped": false
          },
          "description": "Является ли СД глобальной",
          "required": true
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "ID СД",
          "required": true
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название СД",
          "required": true
        },
        "pickupDeliveryStrategy": {
          "type": "string",
          "description": "Стратегия расчета СиС доставки в пвз",
          "enum": [
            "NO_DELIVERY",
            "AUTO_CALCULATED",
            "UNKNOWN_COST_TIME",
            "FIXED_COST_TIME"
          ],
          "required": true
        }
      },
      "title": "SelectedDeliveryServiceResponseDTO",
      "description": "Данные настроенной СД"
    },
    "ShopDeliveryScheduleSetting": {
      "type": "object",
      "properties": {
        "days": {
          "type": "integer",
          "format": "int32"
        },
        "isSelected": {
          "type": "boolean"
        },
        "minutes": {
          "type": "integer",
          "format": "int32"
        },
        "startDay": {
          "type": "string",
          "enum": [
            "NEVERSDAY",
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
          ]
        },
        "startMinute": {
          "type": "integer",
          "format": "int32"
        }
      },
      "title": "ShopDeliveryScheduleSetting"
    },
    "ShopDeliveryServiceCondition": {
      "type": "object",
      "properties": {
        "condition": {
          "type": "string",
          "enum": [
            "DELIVERY_SETTINGS",
            "CPA",
            "CPA_PREPAYMENT",
            "MARKET_DELIVERY_ENABLED",
            "DELIVERY_CONTRACT_SIGNED"
          ]
        },
        "value": {
          "type": "boolean"
        }
      },
      "title": "ShopDeliveryServiceCondition"
    },
    "ShopFeatureInfoDto": {
      "type": "object",
      "properties": {
        "can-enable": {
          "type": "boolean",
          "xml": {
            "name": "can-enable",
            "attribute": true,
            "wrapped": false
          },
          "description": "Выполнены ли все предусловия для включения программы, если применимо."
        },
        "cutoffs": {
          "type": "array",
          "description": "Список типов отключений, если применимо.",
          "items": {
            "$ref": "#/definitions/FeatureCutoffInfoDto"
          }
        },
        "failed-precondition": {
          "type": "array",
          "description": "Если can-enable false, список проваленных проверок предусловий.",
          "items": {
            "type": "string"
          }
        },
        "feature-id": {
          "type": "string",
          "xml": {
            "name": "feature-id",
            "attribute": true,
            "wrapped": false
          },
          "description": "ID программы",
          "enum": [
            "PROMO_CPC",
            "SUBSIDIES",
            "FULFILLMENT",
            "CPA_20",
            "RED_MARKET",
            "DROPSHIP",
            "SHOP_LOGO",
            "PREPAY",
            "CASHBACK",
            "CROSSDOCK",
            "MARKETPLACE",
            "FMCG_PARTNER",
            "DAAS",
            "ALCOHOL",
            "CUT_PRICE",
            "CREDITS"
          ]
        },
        "feature-name": {
          "type": "string",
          "xml": {
            "name": "feature-name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Строковый код программы"
        },
        "recent-message": {
          "description": "Последнее отправленное уведомление об отключении, если применимо.",
          "$ref": "#/definitions/FeatureCutoffMessageDto"
        },
        "shop-id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "shop-id",
            "attribute": true,
            "wrapped": false
          },
          "description": "ID магазина/поставщика"
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус программы",
          "enum": [
            "REVOKE",
            "SUCCESS",
            "FAIL",
            "NEW",
            "DONT_WANT",
            "FAIL_MANUAL"
          ]
        }
      },
      "title": "ShopFeatureInfoDto",
      "description": "Представление состояния программы."
    },
    "ShopLogoInfoDTO": {
      "type": "object",
      "properties": {
        "datasourceId": {
          "type": "integer",
          "format": "int64"
        },
        "imageType": {
          "type": "string",
          "enum": [
            "PNG",
            "SVG"
          ]
        },
        "logoVersions": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ImageVersionDTO"
          }
        },
        "uploadDate": {
          "type": "string",
          "format": "date-time"
        }
      },
      "title": "ShopLogoInfoDTO"
    },
    "ShopMarketDeliveryConditionsDto": {
      "type": "object",
      "properties": {
        "activeState": {
          "type": "string",
          "enum": [
            "ENABLED",
            "DISABLED",
            "NEVER_SET"
          ]
        },
        "conditions": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ShopDeliveryServiceCondition"
          }
        }
      },
      "title": "ShopMarketDeliveryConditionsDto",
      "xml": {
        "name": "ShopMarketDeliveryConditionsDto",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopMarketDeliveryService": {
      "type": "object",
      "properties": {
        "conditions": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ShopDeliveryServiceCondition"
          }
        },
        "deliveryService": {
          "$ref": "#/definitions/DeliveryServiceInfo"
        },
        "settings": {
          "$ref": "#/definitions/ShopMarketDeliveryServiceSettings"
        }
      },
      "title": "ShopMarketDeliveryService",
      "xml": {
        "name": "ShopMarketDeliveryService",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopMarketDeliveryServiceSettings": {
      "type": "object",
      "properties": {
        "active": {
          "type": "boolean"
        },
        "canChangeDateSwitchHour": {
          "type": "boolean"
        },
        "cpa": {
          "type": "boolean"
        },
        "dateSwitchHour": {
          "type": "integer",
          "format": "int32"
        },
        "declaredValuePercent": {
          "type": "integer",
          "format": "int32"
        },
        "deliveryServiceId": {
          "type": "integer",
          "format": "int64"
        },
        "depth": {
          "type": "integer",
          "format": "int32"
        },
        "height": {
          "type": "integer",
          "format": "int32"
        },
        "inletId": {
          "type": "integer",
          "format": "int32"
        },
        "intakeScheduleSettings": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ShopDeliveryScheduleSetting"
          }
        },
        "marketDeliveryEnabled": {
          "type": "boolean"
        },
        "prepayment": {
          "type": "boolean"
        },
        "shipmentType": {
          "type": "string",
          "enum": [
            "INTAKE",
            "SELF_EXPORT"
          ]
        },
        "shopId": {
          "type": "integer",
          "format": "int64"
        },
        "warehouseId": {
          "type": "integer",
          "format": "int32"
        },
        "weight": {
          "type": "integer",
          "format": "int32"
        },
        "width": {
          "type": "integer",
          "format": "int32"
        }
      },
      "title": "ShopMarketDeliveryServiceSettings"
    },
    "ShopRegistrationDTO": {
      "type": "object",
      "properties": {
        "campaignType": {
          "type": "string",
          "enum": [
            "SHOP",
            "SUPPLIER",
            "CROSSBORDER",
            "FMCG",
            "DELIVERY"
          ]
        },
        "domain": {
          "type": "string"
        },
        "global": {
          "type": "boolean"
        },
        "internalShopName": {
          "type": "string"
        },
        "isOnline": {
          "type": "boolean"
        },
        "localRegionId": {
          "type": "integer",
          "format": "int64"
        },
        "notificationContact": {
          "$ref": "#/definitions/NotificationContact"
        },
        "ownerLogin": {
          "type": "string"
        },
        "regionId": {
          "type": "integer",
          "format": "int64"
        },
        "registrationSource": {
          "type": "string"
        },
        "shopName": {
          "type": "string"
        }
      },
      "title": "ShopRegistrationDTO",
      "xml": {
        "name": "registerShopAndCampaignRequest",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopRegistrationResponse": {
      "type": "object",
      "properties": {
        "agencyId": {
          "type": "integer",
          "format": "int64"
        },
        "campaignId": {
          "type": "integer",
          "format": "int64"
        },
        "datasourceId": {
          "type": "integer",
          "format": "int64"
        },
        "managerId": {
          "type": "integer",
          "format": "int64"
        },
        "ownerId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "ShopRegistrationResponse",
      "xml": {
        "name": "registerShopAndCampaignResponse",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopSku": {
      "type": "object",
      "properties": {
        "availability": {
          "type": "string",
          "xml": {
            "name": "availability",
            "attribute": true,
            "wrapped": false
          },
          "description": "Сатус активности офера",
          "enum": [
            "ACTIVE",
            "INACTIVE",
            "DELISTED"
          ]
        },
        "availabilityStatus": {
          "type": "string",
          "xml": {
            "name": "availabilityStatus",
            "attribute": false,
            "wrapped": false
          },
          "enum": [
            "ACTIVE",
            "INACTIVE",
            "DELISTED"
          ]
        },
        "barcodes": {
          "type": "array",
          "xml": {
            "name": "barcodes",
            "attribute": false,
            "wrapped": true
          },
          "description": "Штрихкоды, которые могут быть у товара",
          "items": {
            "type": "string"
          }
        },
        "brand": {
          "type": "string",
          "xml": {
            "name": "brand",
            "attribute": true,
            "wrapped": false
          },
          "description": "Торговая марка товара"
        },
        "categoryName": {
          "type": "string",
          "xml": {
            "name": "categoryName",
            "attribute": true,
            "wrapped": false
          },
          "description": "Категория товара"
        },
        "contentProcessingTaskId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "contentProcessingTaskId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ид задачи обработки контента"
        },
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание офера"
        },
        "masterData": {
          "xml": {
            "name": "masterData",
            "attribute": false,
            "wrapped": false
          },
          "description": "Мастер данные, то что нам нужно собирать по закону об агрегаторах",
          "$ref": "#/definitions/MasterData"
        },
        "offerProcessingComments": {
          "type": "array",
          "xml": {
            "name": "offerProcessingComments",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий к статусу размещения офера",
          "items": {
            "$ref": "#/definitions/UserMessageDTO"
          }
        },
        "offerProcessingStatus": {
          "type": "string",
          "xml": {
            "name": "offerProcessingStatus",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус размещения офера",
          "enum": [
            "UNKNOWN",
            "READY",
            "IN_WORK",
            "NEED_INFO",
            "NEED_MAPPING",
            "NEED_CONTENT",
            "SUSPENDED",
            "CONTENT_PROCESSING",
            "REJECTED"
          ]
        },
        "shopSku": {
          "type": "string",
          "xml": {
            "name": "shopSku",
            "attribute": true,
            "wrapped": false
          },
          "description": "Непосредственное строковое значение SKU"
        },
        "title": {
          "type": "string",
          "xml": {
            "name": "title",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название офера"
        },
        "updateComment": {
          "type": "string",
          "xml": {
            "name": "updateComment",
            "attribute": true,
            "wrapped": false
          }
        },
        "urls": {
          "type": "array",
          "xml": {
            "name": "urls",
            "attribute": false,
            "wrapped": true
          },
          "description": "Урл товара на сайте",
          "items": {
            "type": "string"
          }
        },
        "vendorCode": {
          "type": "string",
          "xml": {
            "name": "vendorCode",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор товара от производителя"
        }
      },
      "title": "ShopSku",
      "description": "SKU используемое поставщиком/магазином (Shop-SKU)"
    },
    "ShopSkuMappingDTO": {
      "type": "object",
      "properties": {
        "active": {
          "xml": {
            "name": "active",
            "attribute": false,
            "wrapped": false
          },
          "description": "Текущий активный marketSku",
          "$ref": "#/definitions/MarketSku"
        },
        "awaitingModeration": {
          "xml": {
            "name": "awaitingModeration",
            "attribute": false,
            "wrapped": false
          },
          "description": "Альтернативный marketSku, который ожидает модерации",
          "$ref": "#/definitions/MarketSku"
        },
        "rejected": {
          "type": "array",
          "xml": {
            "name": "rejected",
            "attribute": false,
            "wrapped": false
          },
          "description": "Список отвергнутых marketSku",
          "items": {
            "$ref": "#/definitions/MarketSku"
          }
        },
        "suggested": {
          "xml": {
            "name": "suggested",
            "attribute": false,
            "wrapped": false
          },
          "description": "Рекоммендуемый marketSku на основе данных о товаре поставщика",
          "$ref": "#/definitions/MarketSku"
        }
      },
      "title": "ShopSkuMappingDTO"
    },
    "ShopSkuPageDTO": {
      "type": "object",
      "properties": {
        "nextPageToken": {
          "type": "string",
          "xml": {
            "name": "nextPageToken",
            "attribute": true,
            "wrapped": false
          }
        },
        "shopSkus": {
          "type": "array",
          "xml": {
            "name": "shopSkus",
            "attribute": false,
            "wrapped": true
          },
          "items": {
            "$ref": "#/definitions/MappedShopSku"
          }
        },
        "total": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "total",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "ShopSkuPageDTO",
      "xml": {
        "name": "shopSkuPage",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopSurveyDTO": {
      "type": "object",
      "properties": {
        "surveyId": {
          "type": "string"
        },
        "surveyUrl": {
          "type": "string"
        }
      },
      "title": "ShopSurveyDTO",
      "xml": {
        "name": "shopSurvey",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopSurveysDTO": {
      "type": "object",
      "properties": {
        "shopSurveys": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ShopSurveyDTO"
          }
        }
      },
      "title": "ShopSurveysDTO",
      "xml": {
        "name": "shopSurveys",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopVat": {
      "type": "object",
      "properties": {
        "datasourceId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "datasourceId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор магазина"
        },
        "deliveryVat": {
          "type": "string",
          "xml": {
            "name": "deliveryVat",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ставка НДС доставки",
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        },
        "taxSystem": {
          "type": "string",
          "xml": {
            "name": "taxSystem",
            "attribute": true,
            "wrapped": false
          },
          "description": "Система налогообложения магазина",
          "enum": [
            "0",
            "1",
            "2",
            "3",
            "4",
            "5"
          ]
        },
        "vat": {
          "type": "string",
          "xml": {
            "name": "vat",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ставка НДС",
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        },
        "vatSource": {
          "type": "string",
          "xml": {
            "name": "vatSource",
            "attribute": true,
            "wrapped": false
          },
          "description": "Источник ставок НДС",
          "enum": [
            "0",
            "1"
          ]
        }
      },
      "title": "ShopVat",
      "description": "Модель настроек налогообложения магазина"
    },
    "ShopVatForm": {
      "type": "object",
      "properties": {
        "datasources": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          }
        },
        "deliveryVat": {
          "type": "string",
          "xml": {
            "name": "deliveryVat",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        },
        "taxSystem": {
          "type": "string",
          "xml": {
            "name": "taxSystem",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "0",
            "1",
            "2",
            "3",
            "4",
            "5"
          ]
        },
        "vat": {
          "type": "string",
          "xml": {
            "name": "vat",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        },
        "vatSource": {
          "type": "string",
          "xml": {
            "name": "vatSource",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "0",
            "1"
          ]
        }
      },
      "title": "ShopVatForm",
      "xml": {
        "name": "shopVat",
        "attribute": false,
        "wrapped": false
      }
    },
    "ShopWarehouse": {
      "type": "object",
      "properties": {
        "forReturn": {
          "type": "boolean",
          "xml": {
            "name": "forReturn",
            "attribute": false,
            "wrapped": false
          },
          "description": "Флаг, говорящий о том, что склад является дефолтным для возврата"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": false,
            "wrapped": false
          },
          "description": "Идентификатор точки продаж"
        },
        "point": {
          "xml": {
            "name": "point",
            "attribute": false,
            "wrapped": false
          },
          "description": "Точка продаж",
          "$ref": "#/definitions/Point"
        },
        "shopId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "shopId",
            "attribute": false,
            "wrapped": false
          },
          "description": "Идентификатор магазина, к которому относится точка продаж"
        },
        "supplierWarehouse": {
          "type": "boolean",
          "xml": {
            "name": "supplierWarehouse",
            "attribute": false,
            "wrapped": false
          },
          "description": "Флаг, говорящий о том, что это не собственный склад магазина, а склад поставщика"
        }
      },
      "title": "ShopWarehouse",
      "description": "Склад магазина для заборов"
    },
    "SignatoryInfoDTO": {
      "type": "object",
      "properties": {
        "docInfo": {
          "type": "string"
        },
        "docType": {
          "type": "string",
          "xml": {
            "name": "docType",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "OTHER",
            "AOA_OR_ENTREPRENEUR",
            "POA",
            "REGISTRATION_CERTIFICATE",
            "ORDER",
            "ENACTMENT",
            "BRANCH_STATUTE",
            "CONTRACT",
            "PROTOCOL",
            "RESOLUTION"
          ]
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "position": {
          "type": "string",
          "xml": {
            "name": "position",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "SignatoryInfoDTO",
      "xml": {
        "name": "signatory",
        "attribute": false,
        "wrapped": false
      }
    },
    "SparkInfoDTO": {
      "type": "object",
      "properties": {
        "active": {
          "type": "boolean",
          "description": "Признак того что организация действующая"
        },
        "organizationInfo": {
          "description": "Юридическая информация",
          "$ref": "#/definitions/OrganizationInfo"
        },
        "sparkStatus": {
          "type": "string",
          "description": "Статус ответа СПАРК",
          "enum": [
            "OK",
            "ERROR",
            "OGRN_NOT_FOUND",
            "RATE_LIMIT",
            "OGRN_INVALID_FORMAT"
          ]
        }
      },
      "title": "SparkInfoDTO",
      "xml": {
        "name": "sparkInfo",
        "attribute": false,
        "wrapped": false
      },
      "description": "Юридеческая информация от СПАРКА (через ABO)"
    },
    "StockStorageSummaryDTO": {
      "type": "object",
      "properties": {
        "available": {
          "xml": {
            "name": "available",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/OfferCountDTO"
        },
        "defect": {
          "xml": {
            "name": "defect",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/OfferCountDTO"
        },
        "expired": {
          "xml": {
            "name": "expired",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/OfferCountDTO"
        },
        "hidden": {
          "$ref": "#/definitions/OfferCountDTO"
        },
        "paidStorage": {
          "$ref": "#/definitions/OfferCountDTO"
        },
        "recommendationRefill": {
          "$ref": "#/definitions/OfferCountDTO"
        },
        "total": {
          "xml": {
            "name": "total",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/OfferCountDTO"
        }
      },
      "title": "StockStorageSummaryDTO"
    },
    "StreamingResponseBody": {
      "type": "object",
      "title": "StreamingResponseBody"
    },
    "StrictPartnerPhoneDTO": {
      "type": "object",
      "properties": {
        "countryCode": {
          "type": "string",
          "xml": {
            "name": "countryCode",
            "attribute": true,
            "wrapped": false
          }
        },
        "extension": {
          "type": "string",
          "xml": {
            "name": "extension",
            "attribute": true,
            "wrapped": false
          }
        },
        "phoneNumber": {
          "type": "string",
          "xml": {
            "name": "phoneNumber",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "StrictPartnerPhoneDTO"
    },
    "SubsidiesContract": {
      "type": "object",
      "properties": {
        "contractNo": {
          "type": "string",
          "xml": {
            "name": "contractNo",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "SubsidiesContract",
      "xml": {
        "name": "contract",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierApplicationDTO": {
      "type": "object",
      "properties": {
        "campaignId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "campaignId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id кампании"
        },
        "comment": {
          "type": "string",
          "xml": {
            "name": "comment",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий"
        },
        "contactInfo": {
          "description": "Контактная информация",
          "$ref": "#/definitions/ContactInfoDTO"
        },
        "documents": {
          "type": "array",
          "description": "Информация о прикреплённых документах",
          "items": {
            "$ref": "#/definitions/PartnerDocumentDTO"
          }
        },
        "domain": {
          "type": "string",
          "xml": {
            "name": "domain",
            "attribute": true,
            "wrapped": false
          },
          "description": "Адрес домена поставщика"
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Название поставщика"
        },
        "organizationInfo": {
          "description": "Информация об организации",
          "$ref": "#/definitions/OrganizationInfoDTO"
        },
        "requestId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "requestId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Id заявки на предоплату"
        },
        "returnContact": {
          "xml": {
            "name": "returnContact",
            "attribute": true,
            "wrapped": false
          },
          "description": "Контактная информация",
          "$ref": "#/definitions/SupplierReturnContactDTO"
        },
        "signatory": {
          "description": "Информация о подписанте заявления",
          "$ref": "#/definitions/SignatoryInfoDTO"
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "description": "Статус заявки",
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        },
        "updatedAt": {
          "type": "string",
          "format": "date",
          "xml": {
            "name": "updatedAt",
            "attribute": true,
            "wrapped": false
          },
          "description": "Дата обновления"
        },
        "validations": {
          "description": "Текущий статус заявки",
          "$ref": "#/definitions/ApplicationValidationsDTO"
        },
        "vatInfo": {
          "description": "Информация о системе налогооблажения",
          "$ref": "#/definitions/VatInfoDTO"
        }
      },
      "title": "SupplierApplicationDTO",
      "xml": {
        "name": "supplierApplication",
        "attribute": false,
        "wrapped": false
      },
      "description": "Заявка на подключение к синему маркету"
    },
    "SupplierApplicationStatusDTO": {
      "type": "object",
      "properties": {
        "status": {
          "type": "string",
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        }
      },
      "title": "SupplierApplicationStatusDTO",
      "xml": {
        "name": "applicatonStatus",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierCommitIdDTO": {
      "type": "object",
      "properties": {
        "commitId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "SupplierCommitIdDTO",
      "xml": {
        "name": "supplierCommitIdDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierCommitInfoDTO": {
      "type": "object",
      "properties": {
        "declinedOffers": {
          "type": "integer",
          "format": "int64"
        },
        "enrichedFile": {
          "$ref": "#/definitions/FileUploadDTO"
        },
        "error": {
          "$ref": "#/definitions/UserMessageDTO"
        },
        "existingOffers": {
          "$ref": "#/definitions/ExistingOffersDTO"
        },
        "feedCommitResult": {
          "type": "string",
          "enum": [
            "UNKNOWN",
            "PROCESSING",
            "OK",
            "WARNING",
            "ERROR"
          ]
        },
        "feedValidationResult": {
          "type": "string",
          "enum": [
            "UNKNOWN",
            "PROCESSING",
            "OK",
            "WARNING",
            "ERROR"
          ]
        },
        "newOffers": {
          "type": "integer",
          "format": "int64"
        },
        "processedOffers": {
          "type": "integer",
          "format": "int64"
        },
        "totalOffers": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "SupplierCommitInfoDTO",
      "xml": {
        "name": "supplierCommitInfoDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierContract": {
      "type": "object",
      "properties": {
        "generalContractId": {
          "type": "string",
          "xml": {
            "name": "generalContractId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Номер основного договора"
        },
        "subsidiesContractId": {
          "type": "string",
          "xml": {
            "name": "subsidiesContractId",
            "attribute": true,
            "wrapped": false
          },
          "description": "Номер договора на субсидирование"
        }
      },
      "title": "SupplierContract",
      "description": "Номера договоров, заключенных с поставщиком"
    },
    "SupplierFeedInfoDTO": {
      "type": "object",
      "properties": {
        "assortment": {
          "description": "Информация о текущем ассортименте(маппинге) поставщика",
          "$ref": "#/definitions/AssortmentFeedDTO"
        },
        "feed": {
          "description": "Информация о текущем фиде(прайс-листе) поставщика",
          "$ref": "#/definitions/AssortmentFeedDTO"
        }
      },
      "title": "SupplierFeedInfoDTO",
      "xml": {
        "name": "supplierFeedDataDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Информация о текущем фиде(прайс-листе) и ассортименте(маппинге) поставщика"
    },
    "SupplierFeedUpdateResultDTO": {
      "type": "object",
      "properties": {
        "feedId": {
          "type": "integer",
          "format": "int64"
        },
        "supplierId": {
          "type": "integer",
          "format": "int64"
        }
      },
      "title": "SupplierFeedUpdateResultDTO",
      "xml": {
        "name": "supplierFeedDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierInfoDto": {
      "type": "object",
      "properties": {
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "SupplierInfoDto",
      "xml": {
        "name": "supplier",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierRegistrationDTO": {
      "type": "object",
      "properties": {
        "domain": {
          "type": "string",
          "xml": {
            "name": "domain",
            "attribute": true,
            "wrapped": false
          }
        },
        "dropship": {
          "type": "boolean",
          "xml": {
            "name": "dropship",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "notificationContact": {
          "xml": {
            "name": "notificationContact",
            "attribute": true,
            "wrapped": false
          },
          "$ref": "#/definitions/NotificationContactDTO"
        }
      },
      "title": "SupplierRegistrationDTO",
      "xml": {
        "name": "supplier",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierReturnContactDTO": {
      "type": "object",
      "properties": {
        "email": {
          "type": "string",
          "xml": {
            "name": "email",
            "attribute": true,
            "wrapped": false
          }
        },
        "firstName": {
          "type": "string",
          "xml": {
            "name": "firstName",
            "attribute": true,
            "wrapped": false
          }
        },
        "lastName": {
          "type": "string",
          "xml": {
            "name": "lastName",
            "attribute": true,
            "wrapped": false
          }
        },
        "phoneNumber": {
          "type": "string",
          "xml": {
            "name": "phoneNumber",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "SupplierReturnContactDTO",
      "xml": {
        "name": "returnContact",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierStateDTO": {
      "type": "object",
      "properties": {
        "campaignId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "campaignId",
            "attribute": true,
            "wrapped": false
          }
        },
        "domain": {
          "type": "string",
          "xml": {
            "name": "domain",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "INIT",
            "IN_PROGRESS",
            "COMPLETED",
            "FROZEN",
            "CLOSED",
            "DECLINED",
            "INTERNAL_CLOSED",
            "NEW",
            "NEED_INFO",
            "CANCELLED",
            "NEW_PROGRAMS_VERIFICATION_REQUIRED",
            "NEW_PROGRAMS_VERIFICATION_FAILED"
          ]
        }
      },
      "title": "SupplierStateDTO",
      "xml": {
        "name": "supplier",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierSummaryStateDTO": {
      "type": "object",
      "properties": {
        "assortment": {
          "$ref": "#/definitions/AssortmentSummaryDTO"
        },
        "feed": {
          "$ref": "#/definitions/FeedSummaryDTO"
        },
        "order": {
          "$ref": "#/definitions/OrderSummaryDTO"
        },
        "stockStorage": {
          "$ref": "#/definitions/StockStorageSummaryDTO"
        }
      },
      "title": "SupplierSummaryStateDTO",
      "xml": {
        "name": "supplierSummaryState",
        "attribute": false,
        "wrapped": false
      }
    },
    "SupplierValidationSelectionDTO": {
      "type": "object",
      "properties": {
        "validationId": {
          "type": "integer",
          "format": "int64",
          "description": "Идентификатор предварительно выполненной валидации"
        }
      },
      "title": "SupplierValidationSelectionDTO",
      "xml": {
        "name": "supplierValidationSelectionDTO",
        "attribute": false,
        "wrapped": false
      },
      "description": "Выбор предворительно выполненной валидации по идентификатору"
    },
    "SuppliersDTO": {
      "type": "object",
      "properties": {
        "suppliers": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/SupplierStateDTO"
          }
        }
      },
      "title": "SuppliersDTO",
      "xml": {
        "name": "suppliers",
        "attribute": false,
        "wrapped": false
      }
    },
    "TimePeriodDTO": {
      "type": "object",
      "properties": {
        "comment": {
          "type": "string",
          "xml": {
            "name": "comment",
            "attribute": true,
            "wrapped": false
          },
          "description": "Комментарий"
        },
        "timePeriod": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "timePeriod",
            "attribute": true,
            "wrapped": false
          },
          "description": "Длительность срока"
        },
        "timeUnit": {
          "type": "string",
          "xml": {
            "name": "timeUnit",
            "attribute": true,
            "wrapped": false
          },
          "description": "Единица измерения срока",
          "enum": [
            "HOUR",
            "DAY",
            "WEEK",
            "MONTH",
            "YEAR"
          ]
        }
      },
      "title": "TimePeriodDTO"
    },
    "TimezoneDTO": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string",
          "description": "Название таймзоны в формате Europe/Moscow",
          "required": true
        },
        "offset": {
          "type": "integer",
          "format": "int32",
          "description": "Смещение таймзоны относительно GMT в секундах",
          "required": true
        }
      },
      "title": "TimezoneDTO",
      "xml": {
        "name": "timezone",
        "attribute": false,
        "wrapped": false
      },
      "description": "Таймзона"
    },
    "UniReportResponse": {
      "type": "object",
      "properties": {
        "report": {
          "$ref": "#/definitions/Report"
        },
        "reportQueryInfo": {
          "$ref": "#/definitions/ReportQueryInfo"
        }
      },
      "title": "UniReportResponse",
      "xml": {
        "name": "uniReportResponse",
        "attribute": false,
        "wrapped": false
      }
    },
    "UploadFeedDTO": {
      "type": "object",
      "properties": {
        "fileName": {
          "type": "string"
        },
        "uploadDateTime": {
          "type": "string",
          "format": "date-time"
        }
      },
      "title": "UploadFeedDTO"
    },
    "UploadResponseDTO": {
      "type": "object",
      "properties": {
        "upload_id": {
          "type": "integer",
          "format": "int64"
        },
        "url": {
          "type": "string"
        }
      },
      "title": "UploadResponseDTO",
      "xml": {
        "name": "uploadResponseDTO",
        "attribute": false,
        "wrapped": false
      }
    },
    "UrlsDTO": {
      "type": "object",
      "properties": {
        "decrypted": {
          "type": "string",
          "xml": {
            "name": "decrypted",
            "attribute": true,
            "wrapped": false
          },
          "description": "Платная ссылка в открытом виде"
        },
        "direct": {
          "type": "string",
          "xml": {
            "name": "direct",
            "attribute": true,
            "wrapped": false
          },
          "description": "Ссылка на товар магазина"
        },
        "encrypted": {
          "type": "string",
          "xml": {
            "name": "encrypted",
            "attribute": true,
            "wrapped": false
          },
          "description": "Шифрованная платная ссылка"
        },
        "geo": {
          "type": "string",
          "xml": {
            "name": "geo",
            "attribute": true,
            "wrapped": false
          },
          "description": "Шифрованная платная ссылка на карту"
        }
      },
      "title": "UrlsDTO"
    },
    "UserMessageDTO": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string",
          "xml": {
            "name": "code",
            "attribute": true,
            "wrapped": false
          }
        },
        "template": {
          "type": "string",
          "xml": {
            "name": "template",
            "attribute": true,
            "wrapped": false
          }
        },
        "templateArguments": {
          "type": "string",
          "xml": {
            "name": "templateArguments",
            "attribute": true,
            "wrapped": false
          }
        },
        "text": {
          "type": "string",
          "xml": {
            "name": "text",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "UserMessageDTO",
      "xml": {
        "name": "userMessage",
        "attribute": false,
        "wrapped": false
      }
    },
    "VatInfoDTO": {
      "type": "object",
      "properties": {
        "deliveryVat": {
          "type": "string",
          "xml": {
            "name": "deliveryVat",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        },
        "taxSystem": {
          "type": "string",
          "xml": {
            "name": "taxSystem",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "0",
            "1",
            "2",
            "3",
            "4",
            "5"
          ]
        },
        "vat": {
          "type": "string",
          "xml": {
            "name": "vat",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
          ]
        },
        "vatSource": {
          "type": "string",
          "xml": {
            "name": "vatSource",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "0",
            "1"
          ]
        }
      },
      "title": "VatInfoDTO"
    },
    "VendorDTO": {
      "type": "object",
      "properties": {
        "description": {
          "type": "string",
          "xml": {
            "name": "description",
            "attribute": true,
            "wrapped": false
          },
          "description": "Описание вендора"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          },
          "description": "Идентификатор вендора"
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          },
          "description": "Наименование вендора"
        }
      },
      "title": "VendorDTO"
    },
    "WarehouseAddressDTO": {
      "type": "object",
      "properties": {
        "additional": {
          "type": "string",
          "xml": {
            "name": "additional",
            "attribute": true,
            "wrapped": false
          }
        },
        "block": {
          "type": "string",
          "xml": {
            "name": "block",
            "attribute": true,
            "wrapped": false
          }
        },
        "building": {
          "type": "string",
          "xml": {
            "name": "building",
            "attribute": true,
            "wrapped": false
          }
        },
        "city": {
          "type": "string",
          "xml": {
            "name": "city",
            "attribute": true,
            "wrapped": false
          }
        },
        "estate": {
          "type": "string",
          "xml": {
            "name": "estate",
            "attribute": true,
            "wrapped": false
          }
        },
        "km": {
          "type": "integer",
          "format": "int32",
          "xml": {
            "name": "km",
            "attribute": true,
            "wrapped": false
          }
        },
        "number": {
          "type": "string",
          "xml": {
            "name": "number",
            "attribute": true,
            "wrapped": false
          }
        },
        "postCode": {
          "type": "string",
          "xml": {
            "name": "postCode",
            "attribute": true,
            "wrapped": false
          }
        },
        "regionId": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "regionId",
            "attribute": true,
            "wrapped": false
          }
        },
        "street": {
          "type": "string",
          "xml": {
            "name": "street",
            "attribute": true,
            "wrapped": false
          }
        }
      },
      "title": "WarehouseAddressDTO"
    },
    "WarehouseDTO": {
      "type": "object",
      "properties": {
        "address": {
          "$ref": "#/definitions/WarehouseAddressDTO"
        },
        "id": {
          "type": "integer",
          "format": "int64",
          "xml": {
            "name": "id",
            "attribute": true,
            "wrapped": false
          }
        },
        "name": {
          "type": "string",
          "xml": {
            "name": "name",
            "attribute": true,
            "wrapped": false
          }
        },
        "phones": {
          "type": "array",
          "items": {
            "type": "string"
          }
        },
        "schedules": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/ScheduleLineDTO"
          }
        }
      },
      "title": "WarehouseDTO"
    },
    "WeeklyScheduleDTO": {
      "type": "object",
      "properties": {
        "endDay": {
          "type": "string",
          "description": "День, до которого задан промежуток. Входит в отрезок",
          "enum": [
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
          ],
          "required": true
        },
        "endTime": {
          "type": "string",
          "xml": {
            "name": "endTime",
            "attribute": false,
            "wrapped": false
          },
          "description": "Время, до которого задан промежуток. Входит в отрезок. Формат: HH:MM. Полночь: 24:00",
          "required": true
        },
        "startDay": {
          "type": "string",
          "description": "День, от которого задан промежуток. Входит в отрезок",
          "enum": [
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
          ],
          "required": true
        },
        "startTime": {
          "type": "string",
          "xml": {
            "name": "startTime",
            "attribute": false,
            "wrapped": false
          },
          "description": "Время, от которого задан промежуток. Входит в отрезок. Формат: HH:MM",
          "required": true
        }
      },
      "title": "WeeklyScheduleDTO",
      "description": "Расписание дня недели"
    },
    "WizardStepStatus": {
      "type": "object",
      "properties": {
        "required": {
          "type": "boolean",
          "xml": {
            "name": "required",
            "attribute": true,
            "wrapped": false
          }
        },
        "status": {
          "type": "string",
          "xml": {
            "name": "status",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "EMPTY",
            "FULL",
            "ENABLING",
            "FILLED",
            "TESTING",
            "SUSPENDED",
            "TESTING_FAILED",
            "TESTING_DROPPED",
            "FAILED",
            "NONE",
            "DISABLED",
            "TESTING_HOLD"
          ]
        },
        "step": {
          "type": "string",
          "xml": {
            "name": "step",
            "attribute": true,
            "wrapped": false
          },
          "enum": [
            "LEGAL",
            "FEED",
            "DELIVERY",
            "SETTINGS",
            "CROSSBORDER_LEGAL"
          ]
        }
      },
      "title": "WizardStepStatus"
    },
    "YearMonth": {
      "type": "object",
      "title": "YearMonth"
    }
  }
};


export const githubSpec: Spec = spec;
