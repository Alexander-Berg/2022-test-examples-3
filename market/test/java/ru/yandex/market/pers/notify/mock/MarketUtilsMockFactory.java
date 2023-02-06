package ru.yandex.market.pers.notify.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.notify.external.comparison.PersComparisonClient;

import static org.mockito.Mockito.mock;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         15.03.16
 */
@SuppressFBWarnings({"MS_MUTABLE_COLLECTION", "MS_MUTABLE_ARRAY"})
public class MarketUtilsMockFactory extends MockFactory {

	public static final Map<Object, Consumer<Object>> MOCKS = new HashMap<>();

	@Autowired
	private WebApplicationContext wac;

	public MockMvc getMockMvc() {
		return MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	public PersComparisonClient getPersComparisonClientMock() {
		PersComparisonClient result = mock(PersComparisonClient.class);
		MOCKS.put(result, (o) -> initPersComparisonMock(result));
		return result;
	}

	public void initPersComparisonMock(PersComparisonClient result) {
		// do nothing
	}


}
