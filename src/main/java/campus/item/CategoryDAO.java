package campus.item;

import java.sql.SQLException;
import java.util.List;

public interface CategoryDAO {

    void insert(Category category) throws SQLException;

    Category findById(Long id) throws SQLException;

    List<Category> findActive() throws SQLException;

    int update(Category category) throws SQLException;
}
