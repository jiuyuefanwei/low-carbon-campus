package campus.user;

import java.sql.SQLException;

/**
 * 用户表访问。实现类只执行 SQL，由调用方决定提交或回滚。
 */
public interface UserDAO {

    void insert(User user) throws SQLException;

    User findById(Long id) throws SQLException;

    User findByUsername(String username) throws SQLException;

    int update(User user) throws SQLException;

    /**
     * 提交评价事务中的信用分更新。分数由调用方算好，这里只写入。
     */
    int updateCreditScore(Long userId, int creditScore) throws SQLException;
}
