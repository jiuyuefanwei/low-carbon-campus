package campus.item;

import java.sql.SQLException;
import java.util.List;

public interface ItemDAO {

    void insert(Item item) throws SQLException;

    Item findById(Long id) throws SQLException;

    List<Item> findByCondition(ItemQuery query) throws SQLException;

    int update(Item item) throws SQLException;

    /**
     * 发起意向、确认完成时更新物品状态。不改其他列。
     */
    int updateStatus(Long itemId, String status) throws SQLException;

    int delete(Long id) throws SQLException;
}
