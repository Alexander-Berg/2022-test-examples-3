package ru.yandex.market.abo.core.billing.calc.calculator.type;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.shop.url.ShopUrl;
import ru.yandex.market.abo.core.shop.url.ShopUrlRepo;
import ru.yandex.market.abo.core.ticket.ProblemService;
import ru.yandex.market.abo.core.ticket.TicketService;
import ru.yandex.market.abo.core.ticket.TicketTagService;
import ru.yandex.market.abo.core.ticket.TicketType;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.CheckSubtype;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.cpa.quality.recheck.mass.MassProblemCheckService;
import ru.yandex.market.abo.cpa.quality.recheck.mass.MassProblemCheckStatus;
import ru.yandex.market.abo.cpa.quality.recheck.mass.MassProblemCheckType;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.gen.model.Hypothesis;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author antipov93.
 * @date 30.06.18.
 */
public class CoreTicketBillingTest extends BillingReportCalculatorTest {

    @SuppressWarnings("IdentityBinaryExpression")
    private static final Predicate<Object> ONE_OF_FOUR_CHANCE = o -> RND.nextBoolean() && RND.nextBoolean();

    private static final String CREATE_CONSULT_SQL = "INSERT INTO CORE_TICKET_CONSULT(YA_UID, HYP_ID) VALUES (?,?)";

    private static final Map<CheckMethod, Integer> CHECK_METHOD_TO_RESULT_INDEX = new HashMap<CheckMethod, Integer>() {{
        put(CheckMethod.BASKET, 4);
        put(CheckMethod.PHONE, 5);
        put(CheckMethod.BY_SIGHT, 6);
        put(CheckMethod.COMPLEX, 7);
        put(CheckMethod.CPA, 12);
        put(CheckMethod.CPA_COMPLEX, 15);
        put(CheckMethod.AUTO_ORDER, 16);
    }};

    private static final int ADDITIONAL_CALLS_INDEX = 8;
    private static final int WITH_DELIVERY_INDEX = 9;
    private static final int PROBLEMS_INDEX = 10;
    private static final int SHOP_URLS_INDEX = 11;
    private static final int CPA_ANSWER_WAIT_INDEX = 13;
    private static final int CPA_CALLS_INDEX = 14;
    private static final int CONSULT_INDEX = 17;
    private static final int MASS_CHECK_INDEX = 18;

    @Autowired
    private TicketService ticketService;
    @Autowired
    private TicketTagService ticketTagService;
    @Autowired
    private HypothesisService hypothesisService;
    @Autowired
    private RecheckTicketManager recheckTicketManager;
    @Autowired
    private MassProblemCheckService massProblemCheckService;
    @Autowired
    private ProblemService problemService;
    @Autowired
    private ShopUrlRepo shopUrlRepo;

    private int hypGenId;
    private Map<Long, List<Ticket>> userIdToTickets;


    public CoreTicketBillingTest() {
        super(7);
    }

    @Override
    protected void populateData() {
        hypGenId = pgJdbcTemplate.queryForObject("SELECT id FROM hyp_gen LIMIT 1", Integer.class);
        userIdToTickets = assessorIds.stream().collect(toMap(identity(), this::createTickets));

        assessorIds.forEach(userId -> {
            processSomeTickets(userId, ADDITIONAL_CALLS_INDEX, CheckMethod.PHONE, ticket ->
                    addStatusToTicketHistory(userId, ticket, TicketStatus.ANSWER_WAITING));

            processSomeTickets(userId, WITH_DELIVERY_INDEX, ticket ->
                    addStatusToTicketHistory(userId, ticket, TicketStatus.DELIVERY_WAITING));

            addProblems(userId);

            processSomeTickets(userId, SHOP_URLS_INDEX, ticket -> {
                ShopUrl shopUrl = new ShopUrl(ticket.getId(), TicketType.CORE, ticket.getShopId(), userId, "");
                shopUrlRepo.save(shopUrl);
            });

            processSomeCpaTickets(userId, CPA_ANSWER_WAIT_INDEX, ticket -> ticketService.saveTicket(
                    ticket, ticketTagService.createTag(userId), CheckSubtype.ANSWER_WAIT_ORDER));


            processSomeCpaTickets(userId, CPA_CALLS_INDEX, ticket -> ticketService.saveTicket(
                    ticket, ticketTagService.createTag(userId), CheckSubtype.ANSWER_WAIT_PHONE));

            processSomeTickets(userId, CONSULT_INDEX, ticket -> {
                pgJdbcTemplate.update(CREATE_CONSULT_SQL, userId, ticket.getId());
            });

            processSomeTickets(userId, MASS_CHECK_INDEX, ticket -> {
                Long recheckId = recheckTicketManager.addTicketIfNotExistsWithLink(ticket.getShopId(),
                        RecheckTicketType.MASS_FOUND, "", ticket.getId()).getId();
                massProblemCheckService.create(recheckId, userId, MassProblemCheckType.CONFIDENCE);
                massProblemCheckService.updateStateByTicketId(recheckId, MassProblemCheckStatus.APPROVED);
            });
        });
    }

    private List<Ticket> createTickets(long userId) {
        return EnumSet.complementOf(EnumSet.of(CheckMethod.DEFAULT)).stream()
                .flatMap(checkMethod -> {
                    int ticketsCount = RND.nextInt(10);
                    addItemsToUser(userId, CHECK_METHOD_TO_RESULT_INDEX.get(checkMethod), ticketsCount);
                    return IntStream.range(0, ticketsCount).mapToObj(i -> createTicket(checkMethod, userId));
                }).collect(toList());
    }

    private Ticket createTicket(CheckMethod checkMethod, Long userId) {
        Hypothesis hypothesis = new Hypothesis(nextId(), 0, hypGenId, "", 1.0, 0L, null);
        hypothesisService.createHypothesis(hypothesis);
        Ticket ticket = new Ticket(hypothesis, null, 0, checkMethod);
        ticket.setStatus(TicketStatus.FINISHED);
        ticketService.saveTicket(ticket, ticketTagService.createTag(userId));
        return ticket;
    }

    private void processSomeTickets(long userId, int resultIndex, Consumer<Ticket> ticketProcessor) {
        processSomeTickets(userId, resultIndex, null, ticketProcessor);
    }

    private void processSomeCpaTickets(long userId, int resultIndex, Consumer<Ticket> ticketProcessor) {
        processSomeTickets(userId, resultIndex, CheckMethod.CPA, ticketProcessor);
    }

    private void processSomeTickets(long userId, int resultIndex, CheckMethod checkMethod,
                                    Consumer<Ticket> ticketProcessor) {
        List<Ticket> toProcess = getSomeTickets(userId, checkMethod);

        addItemsToUser(userId, resultIndex, toProcess.size());
        toProcess.forEach(ticketProcessor);
    }

    private void addProblems(long userId) {
        List<Ticket> tickets = getSomeTickets(userId, null);
        List<Integer> problemTypes = pgJdbcTemplate.queryForList("SELECT id FROM core_problem_type", Integer.class);
        int totalProblemsCreated = tickets.stream().mapToInt(ticket -> (int) problemTypes.stream()
                .filter(ONE_OF_FOUR_CHANCE).limit(RND.nextInt(3) + 1)
                .peek(id -> createProblem(userId, ticket, id))
                .count() - 1
        ).sum();
        addItemsToUser(userId, PROBLEMS_INDEX, totalProblemsCreated);
    }

    private void createProblem(Long userId, Ticket ticket, int problemTypeId) {
        Problem problem = Problem.newBuilder()
                .ticketId(ticket.getId())
                .problemTypeId(problemTypeId)
                .status(ProblemStatus.NEW)
                .build();
        problemService.saveProblem(problem, ticketTagService.createTag(userId));
    }

    private List<Ticket> getSomeTickets(long userId, CheckMethod checkMethod) {
        return userIdToTickets.get(userId).stream()
                .filter(ticket -> checkMethod == null || ticket.getCheckMethod() == checkMethod)
                .filter(ONE_OF_FOUR_CHANCE)
                .collect(Collectors.toList());
    }

    private void addStatusToTicketHistory(Long userId, Ticket ticket, TicketStatus status) {
        TicketStatus originalStatus = ticket.getStatus();
        ticket.setStatus(status);
        ticketService.saveTicket(ticket, ticketTagService.createTag(userId), CheckSubtype.OTHER);
        ticket.setStatus(originalStatus);
        ticketService.saveTicket(ticket, ticketTagService.createTag(userId), CheckSubtype.OTHER);
    }
}
