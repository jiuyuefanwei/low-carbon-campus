package campus.item;

import campus.DaoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryDAOTest extends DaoTestBase {

    private CategoryDAO categoryDAO;

    @BeforeEach
    void setUp() {
        categoryDAO = new SqliteCategoryDAO(connection);
    }

    @Test
    void initScriptSeedsFiveActiveCategories() throws SQLException {
        List<Category> active = categoryDAO.findActive();

        assertEquals(5, active.size());
        assertEquals("书籍教材", active.get(0).getName());
        assertEquals(0.5, active.get(0).getCarbonFactor());
        assertEquals("电子产品", active.get(1).getName());
        assertEquals(2.0, active.get(1).getCarbonFactor());
    }

    @Test
    void insertUpdateAndHideInactiveCategory() throws SQLException {
        Category category = new Category();
        category.setName("实验器材");
        category.setDescription("实验室闲置器材");
        category.setCarbonFactor(1.2);
        categoryDAO.insert(category);

        Category stored = categoryDAO.findById(category.getId());
        assertEquals("实验器材", stored.getName());
        assertEquals(1.2, stored.getCarbonFactor());
        assertTrue(stored.getIsActive());
        assertTrue(categoryDAO.findActive().stream().anyMatch(item -> item.getId().equals(category.getId())));

        stored.setName("实验仪器");
        stored.setCarbonFactor(1.5);
        stored.setIsActive(false);
        assertEquals(1, categoryDAO.update(stored));

        Category updated = categoryDAO.findById(category.getId());
        assertEquals("实验仪器", updated.getName());
        assertEquals(1.5, updated.getCarbonFactor());
        assertFalse(updated.getIsActive());
        assertTrue(categoryDAO.findActive().stream().noneMatch(item -> item.getId().equals(category.getId())));
        assertNull(categoryDAO.findById(-1L));
    }
}
