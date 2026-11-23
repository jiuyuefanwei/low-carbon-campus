package campus.item;

import campus.DaoTestBase;
import campus.user.SqliteUserDAO;
import campus.user.User;
import campus.user.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDAOTest extends DaoTestBase {

    private UserDAO userDAO;
    private CategoryDAO categoryDAO;
    private ItemDAO itemDAO;
    private Long sellerId;
    private Long otherSellerId;
    private Long bookCategoryId;
    private Long electronicCategoryId;

    @BeforeEach
    void setUp() throws SQLException {
        userDAO = new SqliteUserDAO(connection);
        categoryDAO = new SqliteCategoryDAO(connection);
        itemDAO = new SqliteItemDAO(connection);
        sellerId = insertUser("seller");
        otherSellerId = insertUser("other");
        bookCategoryId = categoryId("书籍教材");
        electronicCategoryId = categoryId("电子产品");
    }

    @Test
    void insertAndFindByIdUsesDefaults() throws SQLException {
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setCategoryId(bookCategoryId);
        item.setName("线性代数");
        item.setTradeType(Item.TRADE_FREE);
        itemDAO.insert(item);

        Item stored = itemDAO.findById(item.getId());
        assertEquals("线性代数", stored.getName());
        assertEquals(0.0, stored.getExpectedPrice());
        assertEquals(Item.STATUS_AVAILABLE, stored.getStatus());
        assertEquals(Item.TRADE_FREE, stored.getTradeType());
        assertNull(itemDAO.findById(-1L));
    }

    @Test
    void findByKeywordCategoryPriceAndSeller() throws SQLException {
        insertItem(sellerId, bookCategoryId, "高数教材", "九成新", 15.0, "数学");
        insertItem(sellerId, electronicCategoryId, "鼠标", "无线", 40.0, "数码");
        insertItem(otherSellerId, bookCategoryId, "英语教材", "有笔记", 12.0, "外语");

        ItemQuery byKeyword = new ItemQuery();
        byKeyword.setKeyword("九成新");
        assertNames(itemDAO.findByCondition(byKeyword), "高数教材");

        ItemQuery byTag = new ItemQuery();
        byTag.setKeyword("数学");
        assertNames(itemDAO.findByCondition(byTag), "高数教材");

        ItemQuery combined = new ItemQuery();
        combined.setKeyword("教材");
        combined.setCategoryId(bookCategoryId);
        combined.setMinPrice(10.0);
        combined.setMaxPrice(20.0);
        combined.setStatus(Item.STATUS_AVAILABLE);
        combined.setSellerId(sellerId);
        assertNames(itemDAO.findByCondition(combined), "高数教材");

        ItemQuery tooCheap = new ItemQuery();
        tooCheap.setCategoryId(electronicCategoryId);
        tooCheap.setMaxPrice(20.0);
        assertTrue(itemDAO.findByCondition(tooCheap).isEmpty());
    }

    @Test
    void keywordUnderscoreIsLiteral() throws SQLException {
        insertItem(sellerId, bookCategoryId, "a_b", null, 1.0, null);
        insertItem(sellerId, bookCategoryId, "abcd", null, 1.0, null);

        ItemQuery query = new ItemQuery();
        query.setKeyword("_");
        assertNames(itemDAO.findByCondition(query), "a_b");
    }

    @Test
    void updateAndDelete() throws SQLException {
        Item item = insertItem(sellerId, bookCategoryId, "台灯", "宿舍", 25.0, "照明");
        item.setName("护眼台灯");
        item.setExpectedPrice(30.0);
        item.setStatus(Item.STATUS_OFFLINE);
        item.setTakenDownAt("2026-11-23 10:00:00");
        assertEquals(1, itemDAO.update(item));

        Item stored = itemDAO.findById(item.getId());
        assertEquals("护眼台灯", stored.getName());
        assertEquals(30.0, stored.getExpectedPrice());
        assertEquals(Item.STATUS_OFFLINE, stored.getStatus());
        assertEquals("2026-11-23 10:00:00", stored.getTakenDownAt());

        assertEquals(1, itemDAO.delete(item.getId()));
        assertNull(itemDAO.findById(item.getId()));
    }

    private Long insertUser(String username) throws SQLException {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setDisplayName(username);
        userDAO.insert(user);
        return user.getId();
    }

    private Long categoryId(String name) throws SQLException {
        return categoryDAO.findActive().stream()
                .filter(category -> name.equals(category.getName()))
                .findFirst()
                .orElseThrow(IllegalStateException::new)
                .getId();
    }

    private Item insertItem(Long seller, Long category, String name, String description, double price, String tags)
            throws SQLException {
        Item item = new Item();
        item.setSellerId(seller);
        item.setCategoryId(category);
        item.setName(name);
        item.setDescription(description);
        item.setExpectedPrice(price);
        item.setTradeType(Item.TRADE_SALE);
        item.setTags(tags);
        itemDAO.insert(item);
        return item;
    }

    private void assertNames(List<Item> items, String... names) {
        assertEquals(Arrays.asList(names), items.stream().map(Item::getName).collect(Collectors.toList()));
    }
}
