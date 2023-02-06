package ru.yandex.market.contentmapping.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import ru.yandex.market.contentmapping.auth.BlackBoxAuthorizationStub
import ru.yandex.market.contentmapping.auth.TvmTicketManager
import ru.yandex.market.contentmapping.auth.filter.DummyAuthInterceptor
import ru.yandex.market.contentmapping.dao.UserRepositoryMock
import ru.yandex.market.contentmapping.dto.mbi.cocon.FeatureCheck
import ru.yandex.market.contentmapping.dto.mbi.cocon.Page
import ru.yandex.market.contentmapping.dto.mbi.cocon.PageResponse
import ru.yandex.market.contentmapping.dto.mbi.cocon.Result
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.kotlin.typealiases.ShopId
import ru.yandex.market.contentmapping.kotlin.typealiases.UserId
import ru.yandex.market.contentmapping.model.Role
import ru.yandex.market.contentmapping.services.mbipartner.MbiPartnerUserService
import ru.yandex.market.contentmapping.services.mbipartner.MbiPartnerUserServiceImpl
import ru.yandex.market.contentmapping.services.security.AccessControlServiceImpl
import ru.yandex.market.contentmapping.services.security.CoconAccessServiceImpl
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.contentmapping.utils.SecurityFilter

/**
 * @author sergtru
 * @since 20.08.2018
 */
@Import(DaoConfig::class, TaskQueueConfig::class, TestTvmConfig::class)
@Configuration
open class TestAuthConfig(
        private val shopService: ShopService,
        private val tvmTicketManager: TvmTicketManager,
        private val sqlDatasourceConfig: SqlDatasourceConfig,
) : WebSecurityConfigurerAdapter() {
    @Bean(name = ["authApiInterceptor", "authCommonInterceptor"])
    open fun authInterceptor() = DummyAuthInterceptor(1, DUMMY_USER_NAME, Role.ADMIN)

    @Bean
    open fun blackBoxAuthorization() = BlackBoxAuthorizationStub()

    @Bean
    open fun securityFilter() = SecurityFilter(authInterceptor())

    override fun configure(http: HttpSecurity) {
        // Turn off Spring Security for tests... for now, as we don't use it
        http.authorizeRequests().anyRequest().permitAll()
        http.csrf().disable()
    }

    companion object {
        private const val DUMMY_USER_NAME = "dummy"
    }


    class MbiPartnerUserServiceMock : MbiPartnerUserServiceImpl("") {
        override fun loadUserCampaignsFromMbiPartner(userId: UserId): List<Shop> = when (userId) {
            UserRepositoryMock.MANAGER_USER.id -> UserRepositoryMock.MANAGER_MBI_SHOPS
            else -> emptyList()
        }

        override fun loadUserBusinessInfoMbiPartner(userId: UserId, businessId: ShopId) = when (userId) {
            UserRepositoryMock.MANAGER_USER.id -> {
                UserRepositoryMock.MANAGER_MBI_SHOPS
                        .filter { it.businessId == businessId }
                        .map { MbiPartnerUserService.MbiBusiness(it.name, it.businessId) }
                        .firstOrNull()
            }
            else -> null
        }
    }

    @Bean
    open fun mbiPartnerUserService() = MbiPartnerUserServiceMock()

    class CoconAccessServiceMock(val tvmTicketManager: TvmTicketManager)
        : CoconAccessServiceImpl(tvmTicketManager, "") {
        override fun getAccessInfo(userId: UserId, shopId: ShopId): PageResponse? = when (userId) {
            UserRepositoryMock.MANAGER_USER.id -> PageResponse().let { pageResponse ->
                pageResponse.result = Result().let { result ->
                    result.pages = listOf(
                            Page().let { page ->
                                page.name = "Page one"
                                page.roles = listOf(
                                        FeatureCheck().let { featureCheck ->
                                            featureCheck.result = true
                                            featureCheck
                                        }
                                )
                                page.states = listOf(
                                        FeatureCheck().let { featureCheck ->
                                            featureCheck.items = listOf(
                                                    "PARAM_VALUE(UNITED_CATALOG_STATUS:SUCCESS)"
                                            )
                                            featureCheck.result = true
                                            featureCheck
                                        }
                                )
                                page
                            }
                    )
                    result
                }
                pageResponse
            }
            else -> null
        }
    }

    @Bean
    open fun coconAccessService() = CoconAccessServiceMock(tvmTicketManager)
    @Bean
    open fun accessControlService() = AccessControlServiceImpl(
            UserRepositoryMock(),
            shopService,
            mbiPartnerUserService(),
            coconAccessService(),
            sqlDatasourceConfig.sqlTransactionTemplate(),
    )
}
