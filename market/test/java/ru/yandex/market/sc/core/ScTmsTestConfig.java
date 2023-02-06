//package ru.yandex.market.sc.core;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import ru.yandex.market.sc.core.dbqueue.finish_route.FinishRouteService;
//import ru.yandex.market.sc.core.domain.route.jdbc.RouteFinishJdbcRepository;
//import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
//
//@Configuration
//public class ScTmsTestConfig {
//
//    @Bean
//    FinishRouteService finishRouteService(RouteFinishJdbcRepository routeFinishJdbcRepository,
//                                          UserRepository userRepository) {
//        return new FinishRouteService(routeFinishJdbcRepository, userRepository);
//    }
//
//}
