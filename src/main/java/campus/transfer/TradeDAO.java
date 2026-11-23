package campus.transfer;

import java.sql.SQLException;
import java.util.List;

public interface TradeDAO {

    void insert(Trade trade) throws SQLException;

    Trade findById(Long id) throws SQLException;

    List<Trade> findByItemId(Long itemId) throws SQLException;

    int update(Trade trade) throws SQLException;

    /**
     * 确认完成时写入完成状态、完成时间和减排估算值。
     */
    int complete(Long tradeId, double carbonReduction) throws SQLException;
}
