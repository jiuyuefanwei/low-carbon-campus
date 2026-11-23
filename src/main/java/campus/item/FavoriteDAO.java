package campus.item;

import java.sql.SQLException;
import java.util.List;

public interface FavoriteDAO {

    void insert(Favorite favorite) throws SQLException;

    Favorite findByUserAndItem(Long userId, Long itemId) throws SQLException;

    List<Favorite> findByUserId(Long userId) throws SQLException;

    int delete(Long userId, Long itemId) throws SQLException;
}
