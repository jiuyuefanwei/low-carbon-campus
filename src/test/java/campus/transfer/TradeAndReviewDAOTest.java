package campus.transfer;

import campus.DaoTestBase;
import campus.item.Category;
import campus.item.CategoryDAO;
import campus.item.Item;
import campus.item.ItemDAO;
import campus.item.SqliteCategoryDAO;
import campus.item.SqliteItemDAO;
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

class TradeAndReviewDAOTest extends DaoTestBase {

    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private TradeDAO tradeDAO;
    private ReviewDAO reviewDAO;
    private Long buyerId;
    private Long sellerId;
    private Long itemId;
    private double carbonFactor;

    @BeforeEach
    void setUp() throws SQLException {
        userDAO = new SqliteUserDAO(connection);
        CategoryDAO categoryDAO = new SqliteCategoryDAO(connection);
        itemDAO = new SqliteItemDAO(connection);
        tradeDAO = new SqliteTradeDAO(connection);
        reviewDAO = new SqliteReviewDAO(connection);

        buyerId = insertUser("buyer");
        sellerId = insertUser("seller");
        Category category = categoryDAO.findActive().stream()
                .filter(item -> "电子产品".equals(item.getName()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        carbonFactor = category.getCarbonFactor();

        Item item = new Item();
        item.setSellerId(sellerId);
        item.setCategoryId(category.getId());
        item.setName("键盘");
        item.setExpectedPrice(50.0);
        item.setTradeType(Item.TRADE_SALE);
        itemDAO.insert(item);
        itemId = item.getId();
    }

    @Test
    void insertAndFindTrade() throws SQLException {
        Trade trade = pendingTrade();
        tradeDAO.insert(trade);

        Trade stored = tradeDAO.findById(trade.getId());
        assertEquals(Trade.STATUS_PENDING, stored.getStatus());
        assertEquals("想要这把键盘", stored.getMessage());
        assertEquals(45.0, stored.getAgreedPrice());
        assertEquals(0.0, stored.getCarbonReduction());
        assertNull(stored.getCompletedAt());
        assertEquals(1, tradeDAO.findByItemId(itemId).size());
    }

    @Test
    void sameUserCannotBeInitiatorAndOwner() {
        Trade trade = pendingTrade();
        trade.setInitiatorId(sellerId);
        trade.setOwnerId(sellerId);
        assertThrows(SQLException.class, () -> tradeDAO.insert(trade));
    }

    @Test
    void createIntentSqlReservesItemUntilRollback() throws SQLException {
        connection.setAutoCommit(false);
        Trade trade = pendingTrade();
        tradeDAO.insert(trade);
        assertEquals(1, itemDAO.updateStatus(itemId, Item.STATUS_RESERVED));

        assertEquals(Trade.STATUS_PENDING, tradeDAO.findById(trade.getId()).getStatus());
        assertEquals(Item.STATUS_RESERVED, itemDAO.findById(itemId).getStatus());

        connection.rollback();
        connection.setAutoCommit(true);

        assertNull(tradeDAO.findById(trade.getId()));
        assertEquals(Item.STATUS_AVAILABLE, itemDAO.findById(itemId).getStatus());
    }

    @Test
    void createIntentAndCompleteSqlCommitTogether() throws SQLException {
        connection.setAutoCommit(false);
        Trade trade = pendingTrade();
        tradeDAO.insert(trade);
        itemDAO.updateStatus(itemId, Item.STATUS_RESERVED);
        connection.commit();

        connection.setAutoCommit(true);
        trade.setStatus(Trade.STATUS_ACCEPTED);
        assertEquals(1, tradeDAO.update(trade));

        connection.setAutoCommit(false);
        assertEquals(1, tradeDAO.complete(trade.getId(), carbonFactor));
        assertEquals(1, itemDAO.updateStatus(itemId, Item.STATUS_COMPLETED));
        connection.commit();
        connection.setAutoCommit(true);

        Trade completed = tradeDAO.findById(trade.getId());
        assertEquals(Trade.STATUS_COMPLETED, completed.getStatus());
        assertEquals(carbonFactor, completed.getCarbonReduction());
        assertNotNull(completed.getCompletedAt());
        assertEquals(Item.STATUS_COMPLETED, itemDAO.findById(itemId).getStatus());
    }

    @Test
    void submitReviewSqlRollsBackWhenCreditUpdateFails() throws SQLException {
        Trade trade = pendingTrade();
        tradeDAO.insert(trade);

        connection.setAutoCommit(false);
        reviewDAO.insert(review(trade.getId(), buyerId, sellerId, 5, "物品很好"));
        assertThrows(SQLException.class, () -> userDAO.updateCreditScore(sellerId, 102));
        connection.rollback();
        connection.setAutoCommit(true);

        assertTrue(reviewDAO.findByTradeId(trade.getId()).isEmpty());
        assertEquals(100, userDAO.findById(sellerId).getCreditScore());
    }

    @Test
    void submitReviewSqlCommitsReviewAndCreditTogether() throws SQLException {
        Trade trade = pendingTrade();
        tradeDAO.insert(trade);

        connection.setAutoCommit(false);
        Review saved = review(trade.getId(), buyerId, sellerId, 5, "物品很好");
        reviewDAO.insert(saved);
        assertEquals(1, userDAO.updateCreditScore(sellerId, 98));
        connection.commit();
        connection.setAutoCommit(true);

        assertEquals(saved.getId(), reviewDAO.findById(saved.getId()).getId());
        assertEquals("物品很好", reviewDAO.findByTradeId(trade.getId()).get(0).getContent());
        assertEquals(98, userDAO.findById(sellerId).getCreditScore());

        assertThrows(SQLException.class, () -> reviewDAO.insert(review(trade.getId(), buyerId, sellerId, 3, "再评一次")));
        assertThrows(SQLException.class, () -> reviewDAO.insert(review(trade.getId(), buyerId, buyerId, 5, "评自己")));
        assertThrows(SQLException.class, () -> reviewDAO.insert(review(trade.getId(), sellerId, buyerId, 0, "零分")));
    }

    private Trade pendingTrade() {
        Trade trade = new Trade();
        trade.setItemId(itemId);
        trade.setInitiatorId(buyerId);
        trade.setOwnerId(sellerId);
        trade.setTradeType(Item.TRADE_SALE);
        trade.setStatus(Trade.STATUS_PENDING);
        trade.setMessage("想要这把键盘");
        trade.setAgreedPrice(45.0);
        return trade;
    }

    private Review review(Long tradeId, Long reviewerId, Long revieweeId, int rating, String content) {
        Review review = new Review();
        review.setTradeId(tradeId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setContent(content);
        return review;
    }

    private Long insertUser(String username) throws SQLException {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setDisplayName(username);
        userDAO.insert(user);
        return user.getId();
    }
}
