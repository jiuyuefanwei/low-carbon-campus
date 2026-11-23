package campus.transfer;

import java.sql.SQLException;
import java.util.List;

public interface ReviewDAO {

    void insert(Review review) throws SQLException;

    Review findById(Long id) throws SQLException;

    List<Review> findByTradeId(Long tradeId) throws SQLException;
}
