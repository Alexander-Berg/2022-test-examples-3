package ru.yandex.tours.testkit

import akka.actor.{Actor, ActorRef}
import ru.yandex.common.actor.logging.Slf4jActorLogging
import ru.yandex.common.tokenization.{TokensDistribution, TokensDistributor}

/**
 * Supports (updates and keeps actual) [[ru.yandex.common.tokenization.TokensDistribution]].
 * Simply listens provided [[ru.yandex.common.tokenization.TokensDistributor]] actor and
 * updates [[ru.yandex.common.tokenization.TokensDistribution]] structure.
 */
class TokenDistributionSupport(distributor: ActorRef, distribution: TokensDistribution)
  extends Actor
  with Slf4jActorLogging {

  override def preStart() = {
    super.preStart()
    distributor ! TokensDistributor.Subscribe
  }

  override def receive = {
    case TokensDistributor.Command.Current(ownTokens, ownerships) =>
      log.debug(s"Current tokens received. " +
        s"It's ${ownTokens.mkString("[", ",", "]")}")
      distribution.set(ownTokens, ownerships)
    case TokensDistributor.Command.Take(token, ownerships) =>
      log.debug(s"Take token <$token>.")
      distribution.take(token, ownerships)
    case TokensDistributor.Command.Return(token, ownerships) =>
      log.debug(s"Return token <$token>.")
      distribution.`return`(token, ownerships)
    case TokensDistributor.Command.ReturnAll =>
      log.debug("Return all tokens.")
      distribution.returnAll()

    case TokensDistributor.Notification.Distributed =>
      log.info("Tokens have been distributed.")
      distribution.setDistributed(value = true)
    case TokensDistributor.Notification.Redistributing =>
      log.info("Tokens distribution has been violated.")
      distribution.setDistributed(value = false)
    case TokensDistributor.Notification.DistributionViolation =>
      log.info("Tokens are under redistribution.")
      distribution.setDistributed(value = false)
  }
}
