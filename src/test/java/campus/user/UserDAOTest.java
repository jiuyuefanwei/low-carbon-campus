package campus.user;

import campus.DaoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserDAOTest extends DaoTestBase {

    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        userDAO = new SqliteUserDAO(connection);
    }

    @Test
    void insertAndFindByUsername() throws SQLException {
        User user = student("liming");
        user.setPhone("13800000000");
        user.setEmail("liming@example.com");
        userDAO.insert(user);

        User found = userDAO.findByUsername("liming");

        assertNotNull(found.getId());
        assertEquals(user.getId(), found.getId());
        assertEquals("hash-liming", found.getPasswordHash());
        assertEquals("liming", found.getDisplayName());
        assertEquals("13800000000", found.getPhone());
        assertEquals("liming@example.com", found.getEmail());
        assertEquals(User.ROLE_STUDENT, found.getRole());
        assertEquals(100, found.getCreditScore());
        assertEquals(User.STATUS_ACTIVE, found.getStatus());
        assertEquals(found.getId(), userDAO.findById(found.getId()).getId());
        assertNull(userDAO.findByUsername("nobody"));
    }

    @Test
    void updateProfileAndCreditScore() throws SQLException {
        User user = student("wang");
        userDAO.insert(user);

        user.setDisplayName("王同学");
        user.setPhone("13900000000");
        user.setStatus(User.STATUS_DISABLED);
        assertEquals(1, userDAO.update(user));

        User updated = userDAO.findById(user.getId());
        assertEquals("王同学", updated.getDisplayName());
        assertEquals("13900000000", updated.getPhone());
        assertEquals(User.STATUS_DISABLED, updated.getStatus());

        assertEquals(1, userDAO.updateCreditScore(user.getId(), 98));
        assertEquals(98, userDAO.findById(user.getId()).getCreditScore());
    }

    @Test
    void duplicateUsernameIsRejected() throws SQLException {
        userDAO.insert(student("same"));
        assertThrows(SQLException.class, () -> userDAO.insert(student("same")));
    }

    private User student(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash-" + username);
        user.setDisplayName(username);
        return user;
    }
}
