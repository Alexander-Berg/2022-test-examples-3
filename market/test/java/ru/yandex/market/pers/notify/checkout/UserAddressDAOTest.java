package ru.yandex.market.pers.notify.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.model.checkout.UserAddress;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         17.05.16
 */
public class UserAddressDAOTest extends MockedDbTest {
	@Autowired
	private UserAddressDAO userAddressDAO;
	
	@Test
	public void testGetUserAddress() throws Exception {
		long uid = RND.nextLong();
		String email = UUID.randomUUID().toString();
		userAddressDAO.setUserAddress(uid, email);
		UserAddress address = userAddressDAO.getUserAddress(uid);
		assertEquals(email, address.mail);
		assertEquals(uid, (long)address.userId);
	}

	@Test
	public void testReplaceUserAddress() throws Exception {
		long uid = RND.nextLong();
		String email = UUID.randomUUID().toString();
		userAddressDAO.setUserAddress(uid, email);

		UserAddress address = userAddressDAO.getUserAddress(uid);
		assertEquals(email, address.mail);
		assertEquals(uid, (long)address.userId);

		email = UUID.randomUUID().toString();
		userAddressDAO.setUserAddress(uid, email);

		address = userAddressDAO.getUserAddress(uid);
		assertEquals(email, address.mail);
		assertEquals(uid, (long)address.userId);
	}
}
