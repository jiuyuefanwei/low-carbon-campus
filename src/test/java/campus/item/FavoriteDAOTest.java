package campus.item;

import campus.DaoTestBase;
import campus.user.SqliteUserDAO;
import campus.user.User;
import campus.user.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoriteDAOTest extends DaoTestBase {

    private FavoriteDAO favoriteDAO;
    private Long userId;
    private Long itemId;
    private Long otherItemId;

    @BeforeEach
    void setUp() throws SQLException {
        UserDAO userDAO = new SqliteUserDAO(connection);
        CategoryDAO categoryDAO = new SqliteCategoryDAO(connection);
        ItemDAO itemDAO = new SqliteItemDAO(connection);
        favoriteDAO = new SqliteFavoriteDAO(connection);

        User user = new User();
        user.setUsername("collector");
        user.setPasswordHash("hash");
        user.setDisplayName("collector");
        userDAO.insert(user);
        userId = user.getId();

        Long categoryId = categoryDAO.findActive().get(0).getId();
        itemId = insertItem(itemDAO, userId, categoryId, "计算器");
        otherItemId = insertItem(itemDAO, userId, categoryId, "书包");
    }

    @Test
    void insertQueryAndDelete() throws SQLException {
        Favorite favorite = favorite(itemId);
        favoriteDAO.insert(favorite);

        Favorite stored = favoriteDAO.findByUserAndItem(userId, itemId);
        assertEquals(favorite.getId(), stored.getId());
        assertNotNull(stored.getCreatedAt());
        assertEquals(1, favoriteDAO.findByUserId(userId).size());

        favoriteDAO.insert(favorite(otherItemId));
        assertEquals(2, favoriteDAO.findByUserId(userId).size());

        assertEquals(1, favoriteDAO.delete(userId, itemId));
        assertNull(favoriteDAO.findByUserAndItem(userId, itemId));
        assertEquals(1, favoriteDAO.findByUserId(userId).size());
    }

    @Test
    void duplicateFavoriteIsRejected() throws SQLException {
        favoriteDAO.insert(favorite(itemId));
        SQLException error = assertThrows(SQLException.class, () -> favoriteDAO.insert(favorite(itemId)));
        assertTrue(error.getMessage().toLowerCase().contains("unique"));
        assertEquals(1, favoriteDAO.findByUserId(userId).size());
    }

    private Favorite favorite(Long targetItemId) {
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(targetItemId);
        return favorite;
    }

    private Long insertItem(ItemDAO itemDAO, Long sellerId, Long categoryId, String name) throws SQLException {
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setCategoryId(categoryId);
        item.setName(name);
        item.setTradeType(Item.TRADE_SALE);
        item.setExpectedPrice(10.0);
        itemDAO.insert(item);
        return item.getId();
    }
}
